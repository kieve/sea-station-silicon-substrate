package ca.kieve.ssss.ai.condition;

import java.util.Map;

import ca.kieve.ssss.component.Equipment;
import ca.kieve.ssss.component.Identifier;

/**
 * Condition that checks if entity has a weapon equipped.
 * If weaponId is specified, checks for that specific weapon.
 * If weaponId is not specified, checks for any weapon.
 */
public class HasWeaponCondition implements Condition {
    private String m_weaponId;

    @Override
    public void initialize(Map<String, Object> properties) {
        if (properties != null && properties.containsKey("weaponId")) {
            m_weaponId = (String) properties.get("weaponId");
        }
    }

    @Override
    public boolean evaluate(ConditionContext context) {
        var equipment = context.entity().get(Equipment.class);
        if (equipment == null || equipment.weapon == null) {
            return false;
        }

        // If no specific weapon required, having any weapon is enough
        if (m_weaponId == null) {
            return true;
        }

        var identifier = equipment.weapon.get(Identifier.class);
        if (identifier == null) {
            return false;
        }

        return m_weaponId.equals(identifier.key());
    }
}
