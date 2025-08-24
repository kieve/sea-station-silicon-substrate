package ca.kieve.ssss.repository;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

import static ca.kieve.ssss.ui.widget.GameWindow.TILE_SCALE;
import static ca.kieve.ssss.ui.widget.GameWindow.TILE_SIZE;
import static com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.DEFAULT_CHARS;

public class FontRepo {
    private FontRepo() {
        // Do not instantiate
    }

    public static final BitmapFont XIROD_32;
    public static final BitmapFont UBUNTU_32;

    public static final BitmapFont UI_UBUNTU_32;

    static {
        XIROD_32 = loadGameFont("fonts/Xirod.otf", TILE_SIZE);
        UBUNTU_32 = loadGameFont("fonts/UbuntuMono-R.ttf", TILE_SIZE);

        UI_UBUNTU_32 = loadUiFont("fonts/UbuntuMono-R.ttf", 24);
    }

    private static BitmapFont loadGameFont(String path, int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(path));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = size;
        parameter.characters = DEFAULT_CHARS + GlyphRepo.EXTRA_CHARS;
        var result = generator.generateFont(parameter);
        result.getData().setScale(TILE_SCALE);
        result.setUseIntegerPositions(false);
        generator.dispose();
        return result;
    }

    private static BitmapFont loadUiFont(String path, int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(path));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = size;
        parameter.flip = true;
        var result = generator.generateFont(parameter);
        generator.dispose();
        return result;
    }
}
