package ca.kieve.ssss.render.layer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.util.Vec3i;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaterDepthLayerTest {
    private final Vec3i m_pos = new Vec3i(1, 1, 0);
    private FluidContext m_fluid;

    @BeforeEach
    void setUp() {
        m_fluid = new FluidContext();
        m_fluid.init(new Vec3i(4, 4, 2));
    }

    private String label(double mass) {
        m_fluid.setMass(m_pos, mass);
        return WaterDepthLayer.label(m_fluid, m_pos);
    }

    @Test
    void sourceCellsLabelledS() {
        m_fluid.setMass(m_pos, 0.5);
        m_fluid.setSource(m_pos, 3.0);
        assertEquals("S", WaterDepthLayer.label(m_fluid, m_pos), "source cells show S");
    }

    @Test
    void largeMassRoundedToInteger() {
        assertEquals("12", label(12.3), "mass >= 10 is shown as a rounded integer");
    }

    @Test
    void wholeMassDropsDecimal() {
        assertEquals("1", label(1.0), "a whole mass drops its .0 suffix");
    }

    @Test
    void fractionalMassDropsLeadingZero() {
        assertEquals(".5", label(0.5), "fractional masses below 1 drop the leading zero");
    }

    @Test
    void mixedMassKeepsBothDigits() {
        assertEquals("2.5", label(2.5), "a mass with whole and fractional parts keeps both");
    }
}
