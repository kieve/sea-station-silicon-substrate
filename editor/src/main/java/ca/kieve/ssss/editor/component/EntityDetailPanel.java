package ca.kieve.ssss.editor.component;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.content.EntityDefinition;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class EntityDetailPanel extends ScrollPane {
    private static final String STYLE_TITLE = "editor-title";
    private static final String STYLE_SUBTITLE =
            "editor-subtitle";

    // language=css
    private static final String CSS = """
            .%1$s {
                -fx-font-weight: bold;
                -fx-font-size: 18;
            }
            .%2$s {
                -fx-font-style: italic;
            }
            """.formatted(STYLE_TITLE, STYLE_SUBTITLE);

    private final ContentRegistry m_registry;
    private final VBox m_content;

    public EntityDetailPanel(ContentRegistry registry) {
        m_registry = registry;

        getStylesheets().add(inline(CSS));

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
        titleLabel.getStyleClass().add(STYLE_TITLE);
        m_content.getChildren().add(titleLabel);

        EntityDefinition def =
                m_registry.getEntityDefinition(entityId);

        if (!def.parents().isEmpty()) {
            var parentsLabel = new Label(
                "Parents: " + String.join(", ", def.parents()));
            parentsLabel.getStyleClass().add(STYLE_SUBTITLE);
            m_content.getChildren().add(parentsLabel);
        }

        List<ComponentDefinition> resolved =
                def.resolveComponents(m_registry);
        for (ComponentDefinition comp : resolved) {
            m_content.getChildren().add(new ComponentBox(comp));
        }
    }
}
