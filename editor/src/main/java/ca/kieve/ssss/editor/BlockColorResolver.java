package ca.kieve.ssss.editor;

import ca.kieve.ssss.component.ColorComp;
import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.ContentRegistry;
import javafx.scene.paint.Color;

public class BlockColorResolver {
    private final ContentRegistry m_registry;

    public BlockColorResolver(ContentRegistry registry) {
        m_registry = registry;
    }

    public Color resolve(String bpId) {
        if (!m_registry.hasEntity(bpId)) {
            return Color.MAGENTA;
        }

        var def = m_registry.getEntityDefinition(bpId);
        var components = def.resolveComponents(m_registry);
        for (ComponentDefinition comp : components) {
            if (comp.type() == ColorComp.class) {
                Object colorVal = comp.properties().get("color");
                if (colorVal instanceof String colorStr) {
                    return parseColor(colorStr);
                }
            }
        }

        return Color.WHITE;
    }

    private static Color parseColor(String colorStr) {
        if (colorStr.startsWith("#")) {
            String hex = colorStr.substring(1);
            if (hex.length() == 8) {
                double r = Integer.parseInt(hex.substring(0, 2), 16) / 255.0;
                double g = Integer.parseInt(hex.substring(2, 4), 16) / 255.0;
                double b = Integer.parseInt(hex.substring(4, 6), 16) / 255.0;
                double a = Integer.parseInt(hex.substring(6, 8), 16) / 255.0;
                return new Color(r, g, b, a);
            }
            if (hex.length() == 6) {
                return Color.web("#" + hex);
            }
        }
        try {
            return Color.web(colorStr);
        } catch (IllegalArgumentException e) {
            return Color.MAGENTA;
        }
    }
}
