package ca.kieve.ssss.editor;

import ca.kieve.ssss.content.ContentLoader;
import ca.kieve.ssss.content.ContentRegistry;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import javafx.application.Application;

import java.util.concurrent.CountDownLatch;

public class EditorApp {
    private static ContentRegistry s_registry;

    public static ContentRegistry getRegistry() {
        return s_registry;
    }

    static void main(String[] args) throws InterruptedException {
        var latch = new CountDownLatch(1);

        var config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = -1;

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                latch.countDown();
            }
        }, config);

        latch.await();

        var loader = new ContentLoader();
        s_registry = loader.loadAll();

        Application.launch(EditorFxApp.class, args);
    }
}
