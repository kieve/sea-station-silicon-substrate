package ca.kieve.ssss.editor;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.FlowLayout;

public class EditorToolBar extends JPanel {
    private final EditorModel m_model;

    public EditorToolBar(EditorModel model) {
        m_model = model;
        setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
        setBorder(BorderFactory.createTitledBorder("Tool"));

        var group = new ButtonGroup();

        var paintButton = new JToggleButton("Paint", true);
        paintButton.addActionListener(
                e -> m_model.setActiveTool(EditorModel.Tool.PAINT));

        var selectButton = new JToggleButton("Select");
        selectButton.addActionListener(
                e -> m_model.setActiveTool(EditorModel.Tool.SELECT));

        var eraseButton = new JToggleButton("Erase");
        eraseButton.addActionListener(
                e -> m_model.setActiveTool(EditorModel.Tool.ERASE));

        group.add(paintButton);
        group.add(selectButton);
        group.add(eraseButton);

        add(paintButton);
        add(selectButton);
        add(eraseButton);

        m_model.addListener(new EditorModel.Listener() {
            @Override
            public void onModelChanged() {}

            @Override
            public void onLayerChanged() {}

            @Override
            public void onBlockSelectionChanged() {}

            @Override
            public void onDirtyChanged() {}

            @Override
            public void onEntitiesChanged() {}

            @Override
            public void onToolChanged() {
                switch (m_model.getActiveTool()) {
                    case PAINT -> paintButton.setSelected(true);
                    case SELECT -> selectButton.setSelected(true);
                    case ERASE -> eraseButton.setSelected(true);
                }
            }
        });
    }
}
