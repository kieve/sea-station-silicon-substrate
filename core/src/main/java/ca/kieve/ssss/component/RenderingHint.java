package ca.kieve.ssss.component;

/**
 * Component that provides rendering hints for entities.
 *
 * zIndex determines draw order for stacked entities:
 * - -1: Never draw
 * - 0: Floor tiles
 * - 1: Non-player entities (walls, enemies, items)
 * - 2: Player and controlled mechs
 *
 * Higher zIndex values are drawn on top.
 */
public class RenderingHint implements Component {
    public int zIndex;

    public RenderingHint(int zIndex) {
        this.zIndex = zIndex;
    }

    @Override
    public String toString() {
        return "RenderingHint{"
            + "zIndex=" + zIndex
            + '}';
    }
}
