package ca.kieve.ssss.editor;

import ca.kieve.ssss.content.ContentRegistry;

public class EditorContext {
    private static EditorContext s_instance;

    private final ContentRegistry m_registry;
    private final BlockColorResolver m_colorResolver;
    private final BlockGlyphResolver m_glyphResolver;

    private EditorContext(ContentRegistry registry) {
        m_registry = registry;
        m_colorResolver = new BlockColorResolver(registry);
        m_glyphResolver = new BlockGlyphResolver(registry);
    }

    public static void initialize(ContentRegistry registry) {
        s_instance = new EditorContext(registry);
    }

    public static EditorContext getInstance() {
        return s_instance;
    }

    public ContentRegistry getRegistry() {
        return m_registry;
    }

    public BlockColorResolver getColorResolver() {
        return m_colorResolver;
    }

    public BlockGlyphResolver getGlyphResolver() {
        return m_glyphResolver;
    }
}
