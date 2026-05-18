package ca.kieve.ssss.content;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Resolves textual content references to canonical content-root paths.
 *
 * <p>Two ref forms are supported:
 * <ul>
 *   <li>{@code ./...} — <em>relative</em> to the directory of the
 *       referring file.</li>
 *   <li>{@code /...} — <em>absolute</em>, rooted at the {@code content/}
 *       directory.</li>
 * </ul>
 *
 * <p>Paths use {@code /} as separator regardless of platform. {@code ..}
 * segments are resolved; refs that escape the content root are rejected.
 */
public final class ContentRef {
    private static final String YAML_SUFFIX = ".yaml";

    private ContentRef() {
    }

    /**
     * Strips a trailing {@code .yaml} extension, if present. Used by
     * loaders that need a content path's stem (e.g. as a default region
     * id derived from a submap ref).
     */
    public static String stripYamlSuffix(String path) {
        return path.endsWith(YAML_SUFFIX)
            ? path.substring(0, path.length() - YAML_SUFFIX.length())
            : path;
    }

    /**
     * Resolves {@code ref} against {@code referringFile} (a path under
     * {@code content/}, e.g. {@code maps/home_base/sub_complex.yaml}).
     *
     * @return the canonical path under {@code content/} , e.g.
     *         {@code maps/home_base/damaged_sub.yaml}
     */
    public static String resolve(String referringFile, String ref) {
        if (ref == null) {
            throw new IllegalArgumentException("ref must not be null");
        }
        String raw;
        if (ref.startsWith("./")) {
            String dir = parentOf(referringFile);
            String rest = ref.substring(2);
            raw = dir.isEmpty() ? rest : dir + "/" + rest;
        } else if (ref.startsWith("/")) {
            raw = ref.substring(1);
        } else {
            throw new IllegalArgumentException(
                "content ref must start with './' (relative) or '/' (absolute under content/), got: "
                    + ref
            );
        }
        return normalize(raw);
    }

    private static String parentOf(String path) {
        if (path == null) {
            return "";
        }
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }

    private static String normalize(String path) {
        Deque<String> stack = new ArrayDeque<>();
        for (String segment : path.split("/")) {
            if (segment.isEmpty() || segment.equals(".")) {
                continue;
            }
            if (!segment.equals("..")) {
                stack.addLast(segment);
                continue;
            }
            if (stack.isEmpty()) {
                throw new IllegalArgumentException("content ref escapes content root: " + path);
            }
            stack.removeLast();
        }
        return String.join("/", stack);
    }
}
