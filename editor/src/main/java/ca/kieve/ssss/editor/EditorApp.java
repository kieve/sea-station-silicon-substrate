package ca.kieve.ssss.editor;

import ca.kieve.ssss.content.ContentLoader;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

import java.util.concurrent.CountDownLatch;
import javafx.application.Application;

public class EditorApp {
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
        EditorContext.initialize(loader.loadAll());

        Application.launch(EditorFxApp.class, args);
    }
}
