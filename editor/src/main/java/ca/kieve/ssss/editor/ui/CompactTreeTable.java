package ca.kieve.ssss.editor.ui;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Cursor;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

/**
 * A compact two-column {@link TreeTableView} with hidden
 * column headers, a visible cell divider, and drag-to-resize
 * from any row. The right column auto-fills remaining width.
 *
 * <p>Top-level items (direct children of root) have their
 * left column text rendered bold.
 */
public class CompactTreeTable<T>
        extends TreeTableView<T> {
    private static final int DRAG_MARGIN = 4;
    private static final String STYLE = "compact-tree-table";
    private static final String STYLE_SECTION =
            "compact-tree-table-section";

    // language=css
    private static final String CSS = """
            .%1$s {
                -fx-font-size: 11;
            }
            .%1$s .column-header-background {
                -fx-max-height: 0;
                -fx-pref-height: 0;
                -fx-min-height: 0;
                visibility: hidden;
            }
            .%1$s .tree-table-row-cell {
                -fx-cell-size: 20;
                -fx-padding: 0;
            }
            .%1$s .tree-table-row-cell .tree-disclosure-node {
                -fx-padding: 2 4 0 4;
            }
            .%1$s .tree-table-cell {
                -fx-text-fill: -color-fg-muted;
                -fx-border-color: transparent -color-border-muted transparent transparent;
                -fx-border-width: 0 1 0 0;
            }
            .%1$s .tree-table-cell:last-tree-table-cell {
                -fx-border-color: transparent;
                -fx-border-width: 0;
            }
            .%2$s {
                -fx-font-weight: bold;
            }
            """.formatted(STYLE, STYLE_SECTION);

    private final TreeTableColumn<T, String> m_leftCol;
    private final TreeTableColumn<T, String> m_rightCol;

    private boolean m_dragging;
    private double m_dragStartX;
    private double m_dragStartWidth;

    public CompactTreeTable(
            Function<T, String> leftExtractor,
            Function<T, String> rightExtractor) {
        getStyleClass().add(STYLE);
        getStylesheets().add(inline(CSS));
        setShowRoot(false);
        setColumnResizePolicy(UNCONSTRAINED_RESIZE_POLICY);

        m_leftCol = new TreeTableColumn<>();
        m_leftCol.setCellValueFactory(p ->
                new SimpleStringProperty(
                        leftExtractor.apply(
                                p.getValue().getValue())));
        m_leftCol.setCellFactory(
                col -> new TreeTableCell<>() {
            @Override
            protected void updateItem(
                    String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().remove(STYLE_SECTION);
                if (!empty) {
                    TreeItem<T> treeItem =
                            getTreeTableRow().getTreeItem();
                    if (treeItem != null
                            && getTreeTableView()
                                    .getTreeItemLevel(
                                            treeItem)
                                    == 1) {
                        getStyleClass().add(STYLE_SECTION);
                    }
                }
            }
        });
        m_leftCol.setPrefWidth(100);

        m_rightCol = new TreeTableColumn<>();
        m_rightCol.setCellValueFactory(p ->
                new SimpleStringProperty(
                        rightExtractor.apply(
                                p.getValue().getValue())));

        getColumns().add(m_leftCol);
        getColumns().add(m_rightCol);

        m_rightCol.prefWidthProperty().bind(
                widthProperty()
                        .subtract(m_leftCol.widthProperty())
                        .subtract(2));

        setupColumnDrag();
    }

    protected TreeTableColumn<T, String> getRightColumn() {
        return m_rightCol;
    }

    private boolean isNearDivider(double x) {
        double edge = m_leftCol.getWidth();
        return Math.abs(x - edge) <= DRAG_MARGIN;
    }

    private void setupColumnDrag() {
        setOnMouseMoved(e -> {
            if (!m_dragging) {
                setCursor(isNearDivider(e.getX())
                        ? Cursor.H_RESIZE
                        : Cursor.DEFAULT);
            }
        });

        setOnMousePressed(e -> {
            if (!isNearDivider(e.getX())) {
                return;
            }
            m_dragging = true;
            m_dragStartX = e.getScreenX();
            m_dragStartWidth = m_leftCol.getWidth();
            e.consume();
        });

        setOnMouseDragged(e -> {
            if (!m_dragging) {
                return;
            }
            double delta = e.getScreenX() - m_dragStartX;
            double newWidth = Math.max(
                    30, m_dragStartWidth + delta);
            m_leftCol.setPrefWidth(newWidth);
            e.consume();
        });

        setOnMouseReleased(e -> {
            if (!m_dragging) {
                return;
            }
            m_dragging = false;
            setCursor(isNearDivider(e.getX())
                    ? Cursor.H_RESIZE
                    : Cursor.DEFAULT);
            e.consume();
        });
    }
}
