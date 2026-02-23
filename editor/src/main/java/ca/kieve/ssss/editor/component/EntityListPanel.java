package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.editor.EditorContext;
import javafx.collections.FXCollections;
import javafx.scene.control.ListView;

import java.util.function.Consumer;

public class EntityListPanel extends ListView<String> {
    public EntityListPanel(
            Consumer<String> onEntitySelected)
    {
        var registry = EditorContext.getInstance().getRegistry();
        var entityIds = registry.getEntityIds().stream()
            .sorted()
            .toList();

        setItems(FXCollections.observableArrayList(entityIds));
        getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> onEntitySelected.accept(newVal));
    }
}
