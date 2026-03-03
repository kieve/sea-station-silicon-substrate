package ca.kieve.ssss.content;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

import ca.kieve.ssss.component.TileGlyph;

import java.util.HashMap;
import java.util.Map;

import static ca.kieve.ssss.ui.widget.GameWindow.TILE_SCALE;
import static ca.kieve.ssss.ui.widget.GameWindow.TILE_SIZE;
import static com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.DEFAULT_CHARS;

public class GlyphFactory {
    private static final String EXTRA_CHARS = "█";

    private final ContentRegistry m_registry;
    private final Map<String, BitmapFont> m_fontCache = new HashMap<>();
    private final Map<String, TileGlyph> m_glyphCache = new HashMap<>();

    public GlyphFactory(ContentRegistry registry) {
        m_registry = registry;
    }

    public TileGlyph getGlyph(String id) {
        TileGlyph cached = m_glyphCache.get(id);
        if (cached != null) {
            return cached;
        }

        GlyphDefinition def = m_registry.getGlyphDefinition(id);
        BitmapFont font = getFont(def.font());

        float dx = def.offsetX() / TILE_SIZE;
        float dy = 1 + def.offsetY() / TILE_SIZE;

        TileGlyph glyph = new TileGlyph(font, def.character(), dx, dy);
        m_glyphCache.put(id, glyph);
        return glyph;
    }

    public BitmapFont getFont(String id) {
        BitmapFont cached = m_fontCache.get(id);
        if (cached != null) {
            return cached;
        }

        FontDefinition def = m_registry.getFontDefinition(id);
        BitmapFont font = loadGameFont(def.path(), def.size());
        m_fontCache.put(id, font);
        return font;
    }

    private BitmapFont loadGameFont(String path, int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(path));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = size;
        parameter.characters = DEFAULT_CHARS + EXTRA_CHARS;
        var result = generator.generateFont(parameter);
        result.getData().setScale(TILE_SCALE);
        result.setUseIntegerPositions(false);
        generator.dispose();
        return result;
    }

    public void dispose() {
        for (BitmapFont font : m_fontCache.values()) {
            font.dispose();
        }
        m_fontCache.clear();
        m_glyphCache.clear();
    }
}
