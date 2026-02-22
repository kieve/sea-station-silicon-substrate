package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.content.ContentRegistry;
import javafx.collections.FXCollections;
import javafx.scene.control.ListView;

import java.util.function.Consumer;

public class EntityListPanel extends ListView<String> {
    public EntityListPanel(
            ContentRegistry registry,
            Consumer<String> onEntitySelected)
    {
        var entityIds = registry.getEntityIds().stream()
            .sorted()
            .toList();

        setItems(FXCollections.observableArrayList(entityIds));
        getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> onEntitySelected.accept(newVal));
    }
}
