package ca.kieve.ssss.editor.ui;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import javafx.stage.Stage;

import ca.kieve.ssss.editor.Globals;

/**
 * Re-enables Windows Aero Snap on an undecorated JavaFX stage.
 * <p>
 * Adds {@code WS_THICKFRAME | WS_CAPTION | WS_MAXIMIZEBOX |
 * WS_MINIMIZEBOX} to the window style, then subclasses the
 * window procedure to handle {@code WM_NCCALCSIZE} (returning 0
 * to eliminate the non-client frame area), {@code WM_NCACTIVATE}
 * and {@code WM_NCPAINT} (suppressing the focus/activation
 * border).
 * <p>
 * Uses Java 25's Foreign Function &amp; Memory API. Only runs on
 * Windows; silently no-ops on other platforms.
 */
public final class WindowsAeroSnap {
    private static final int GWL_STYLE = -16;
    private static final int WS_CAPTION = 0x00C00000;
    private static final int WS_THICKFRAME = 0x00040000;
    private static final int WS_MAXIMIZEBOX = 0x00010000;
    private static final int WS_MINIMIZEBOX = 0x00020000;

    private static final int SWP_NOMOVE = 0x0002;
    private static final int SWP_NOSIZE = 0x0001;
    private static final int SWP_NOZORDER = 0x0004;
    private static final int SWP_FRAMECHANGED = 0x0020;

    private static final int WM_NCCALCSIZE = 0x0083;
    private static final int WM_NCACTIVATE = 0x0086;
    private static final int WM_NCPAINT = 0x0085;
    private static final int WM_NCLBUTTONDOWN = 0x00A1;
    private static final int HTCAPTION = 2;

    // Prevent GC of the upcall stub and the DefSubclassProc handle
    @SuppressWarnings("unused")
    private static MemorySegment s_callbackStub;
    private static MethodHandle s_defSubclassProc;

    private static MemorySegment s_hwnd;
    private static MethodHandle s_releaseCapture;
    private static MethodHandle s_sendMessage;

    private WindowsAeroSnap() {}

    /**
     * Call after {@code stage.show()} to patch the native window
     * style for Aero Snap support.
     */
    public static void apply(Stage stage) {
        if (!Globals.IS_WIN) {
            return;
        }

        try {
            long hwnd = getNativeHandle();
            if (hwnd == 0) {
                return;
            }
            patchWindowStyle(hwnd);
        } catch (Throwable t) {
            IO.println("Aero snap patch failed: "
                    + t.getMessage());
        }
    }

    /**
     * Initiates a native window drag via the Windows caption
     * drag mechanism. This enables Aero Snap previews when the
     * window is dragged to screen edges.
     *
     * @return true if the native drag was initiated
     */
    public static boolean startNativeDrag() {
        if (s_hwnd == null || s_releaseCapture == null
                || s_sendMessage == null) {
            return false;
        }
        try {
            s_releaseCapture.invoke();
            s_sendMessage.invoke(
                    s_hwnd, WM_NCLBUTTONDOWN,
                    (long) HTCAPTION, 0L);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static long getNativeHandle() throws Exception {
        // Requires:
        //   --add-opens javafx.graphics/com.sun.glass.ui=ALL-UNNAMED
        Class<?> windowClass =
                Class.forName("com.sun.glass.ui.Window");
        @SuppressWarnings("unchecked")
        var windows = (java.util.List<?>)
                windowClass.getMethod("getWindows").invoke(null);
        if (windows.isEmpty()) {
            return 0;
        }
        Object glassWindow = windows.getFirst();
        return (long) windowClass
                .getMethod("getNativeHandle")
                .invoke(glassWindow);
    }

    private static void patchWindowStyle(long hwnd)
            throws Throwable {
        Linker linker = Linker.nativeLinker();
        Arena arena = Arena.global();

        SymbolLookup user32 =
                SymbolLookup.libraryLookup("user32.dll", arena);

        MethodHandle getWindowLong = linker.downcallHandle(
                user32.findOrThrow("GetWindowLongW"),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS,
                        ValueLayout.JAVA_INT));

        MethodHandle setWindowLong = linker.downcallHandle(
                user32.findOrThrow("SetWindowLongW"),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS,
                        ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_INT));

        MethodHandle setWindowPos = linker.downcallHandle(
                user32.findOrThrow("SetWindowPos"),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS,
                        ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_INT));

        MemorySegment hwndSeg =
                MemorySegment.ofAddress(hwnd);
        s_hwnd = hwndSeg;

        // Cache ReleaseCapture and SendMessageW for native drag
        s_releaseCapture = linker.downcallHandle(
                user32.findOrThrow("ReleaseCapture"),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT));

        s_sendMessage = linker.downcallHandle(
                user32.findOrThrow("SendMessageW"),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_LONG,
                        ValueLayout.ADDRESS,
                        ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_LONG,
                        ValueLayout.JAVA_LONG));

        // Install WM_NCCALCSIZE subclass BEFORE changing styles
        // so the frame is suppressed immediately
        installSubclass(hwndSeg, arena, linker);

        // WS_THICKFRAME + WS_CAPTION enable WM_NCHITTEST which
        // is required for Aero Snap. WM_NCCALCSIZE returning 0
        // hides the caption and frame visually.
        int style = (int) getWindowLong.invoke(
                hwndSeg, GWL_STYLE);
        int newStyle = style
                | WS_THICKFRAME
                | WS_CAPTION
                | WS_MAXIMIZEBOX
                | WS_MINIMIZEBOX;
        setWindowLong.invoke(hwndSeg, GWL_STYLE, newStyle);

        // Notify Windows the frame changed
        setWindowPos.invoke(
                hwndSeg,
                MemorySegment.NULL,
                0, 0, 0, 0,
                SWP_NOMOVE | SWP_NOSIZE
                        | SWP_NOZORDER | SWP_FRAMECHANGED);
    }

    private static void installSubclass(
            MemorySegment hwndSeg,
            Arena arena,
            Linker linker) throws Throwable {
        SymbolLookup comctl32 = SymbolLookup.libraryLookup(
                "comctl32.dll", arena);

        // Cache DefSubclassProc for use in the callback
        s_defSubclassProc = linker.downcallHandle(
                comctl32.findOrThrow("DefSubclassProc"),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_LONG,
                        ValueLayout.ADDRESS,
                        ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_LONG,
                        ValueLayout.JAVA_LONG));

        // SUBCLASSPROC signature:
        //   LRESULT (HWND, UINT, WPARAM, LPARAM,
        //            UINT_PTR, DWORD_PTR)
        FunctionDescriptor subclassProcDesc =
                FunctionDescriptor.of(
                        ValueLayout.JAVA_LONG,
                        ValueLayout.ADDRESS,
                        ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_LONG,
                        ValueLayout.JAVA_LONG,
                        ValueLayout.JAVA_LONG,
                        ValueLayout.JAVA_LONG);

        MethodHandle callback = MethodHandles.lookup().findStatic(
                WindowsAeroSnap.class,
                "subclassProc",
                MethodType.methodType(
                        long.class,
                        MemorySegment.class,
                        int.class,
                        long.class,
                        long.class,
                        long.class,
                        long.class));

        // Create native function pointer — lives in global arena
        // so it's never freed
        s_callbackStub = linker.upcallStub(
                callback, subclassProcDesc, arena);

        // BOOL SetWindowSubclass(HWND, SUBCLASSPROC,
        //                        UINT_PTR, DWORD_PTR)
        MethodHandle setWindowSubclass = linker.downcallHandle(
                comctl32.findOrThrow("SetWindowSubclass"),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS,
                        ValueLayout.JAVA_LONG,
                        ValueLayout.JAVA_LONG));

        int ok = (int) setWindowSubclass.invoke(
                hwndSeg, s_callbackStub, 1L, 0L);
        if (ok == 0) {
            IO.println("SetWindowSubclass failed");
        }
    }

    /**
     * Window subclass procedure. Intercepts:
     * <ul>
     *   <li>{@code WM_NCCALCSIZE} — returns 0 to eliminate the
     *       non-client frame area</li>
     *   <li>{@code WM_NCACTIVATE} — returns TRUE to accept
     *       activation without painting the frame border</li>
     *   <li>{@code WM_NCPAINT} — returns 0 to suppress
     *       non-client area painting</li>
     * </ul>
     * All other messages are forwarded to DefSubclassProc.
     */
    @SuppressWarnings("unused") // called via upcall stub
    private static long subclassProc(
            MemorySegment hWnd,
            int uMsg,
            long wParam,
            long lParam,
            long uIdSubclass,
            long dwRefData) {
        if (uMsg == WM_NCCALCSIZE && wParam != 0) {
            return 0;
        }
        if (uMsg == WM_NCACTIVATE) {
            // Return TRUE to accept activation state change
            // without painting the default activation border
            return 1;
        }
        if (uMsg == WM_NCPAINT) {
            // Suppress all non-client painting
            return 0;
        }
        try {
            return (long) s_defSubclassProc.invoke(
                    hWnd, uMsg, wParam, lParam);
        } catch (Throwable t) {
            return 0;
        }
    }
}
