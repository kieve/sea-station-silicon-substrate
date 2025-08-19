package ca.kieve.ssss.repository;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

import ca.kieve.ssss.MainEngine;

import static ca.kieve.ssss.MainEngine.TILE_SIZE;

public class FontRepo {
    private FontRepo() {
        // Do not instantiate
    }

    public static final BitmapFont XIROD_32;

    static {
        XIROD_32 = loadFont("fonts/Xirod.otf", TILE_SIZE);
    }

    private static BitmapFont loadFont(String path, int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(path));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = size;
        var result = generator.generateFont(parameter);
        result.getData().setScale(MainEngine.TILE_SCALE);
        result.setUseIntegerPositions(false);
        generator.dispose();
        return result;
    }
}
