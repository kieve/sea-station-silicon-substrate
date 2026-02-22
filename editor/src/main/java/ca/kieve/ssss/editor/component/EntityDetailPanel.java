package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.content.EntityDefinition;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class EntityDetailPanel extends ScrollPane {
    private final ContentRegistry m_registry;
    private final VBox m_content;

    public EntityDetailPanel(ContentRegistry registry) {
        m_registry = registry;

        m_content = new VBox(10);
        m_content.setPadding(new Insets(10));

        setContent(m_content);
        setFitToWidth(true);
    }

    public void showEntity(String entityId) {
        m_content.getChildren().clear();
        if (entityId == null) {
            return;
        }

        var titleLabel = new Label(entityId);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        m_content.getChildren().add(titleLabel);

        EntityDefinition def = m_registry.getEntityDefinition(entityId);

        if (!def.parents().isEmpty()) {
            var parentsLabel = new Label(
                "Parents: " + String.join(", ", def.parents()));
            parentsLabel.setStyle("-fx-font-style: italic;");
            m_content.getChildren().add(parentsLabel);
        }

        List<ComponentDefinition> resolved = def.resolveComponents(m_registry);
        for (ComponentDefinition comp : resolved) {
            m_content.getChildren().add(new ComponentBox(comp));
        }
    }
}
