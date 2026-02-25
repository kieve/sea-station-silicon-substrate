package ca.kieve.ssss.content;

import ca.kieve.ssss.util.Vec3i;

import com.badlogic.gdx.graphics.Color;

import java.lang.reflect.Field;

public class PropertyParser {

    public static Object parseValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isInstance(value)) {
            return value;
        }

        if (targetType == Color.class && value instanceof String) {
            return parseColor((String) value);
        }

        if (targetType == Vec3i.class && value instanceof String) {
            return parseVec3i((String) value);
        }

        if (targetType == int.class || targetType == Integer.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        }

        if (targetType == long.class || targetType == Long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(value.toString());
        }

        if (targetType == float.class || targetType == Float.class) {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            return Float.parseFloat(value.toString());
        }

        if (targetType == double.class || targetType == Double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        }

        if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            return Boolean.parseBoolean(value.toString());
        }

        if (targetType == String.class) {
            return value.toString();
        }

        if (targetType.isEnum()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object enumValue = Enum.valueOf((Class<Enum>) targetType, value.toString());
            return enumValue;
        }

        throw new IllegalArgumentException(
            "Cannot convert " + value.getClass().getSimpleName()
                + " to " + targetType.getSimpleName());
    }

    private static Color parseColor(String str) {
        if (str.startsWith("#")) {
            return Color.valueOf(str);
        }

        String[] parts = str.split(",");
        if (parts.length == 3) {
            float r = Float.parseFloat(parts[0].trim()) / 255f;
            float g = Float.parseFloat(parts[1].trim()) / 255f;
            float b = Float.parseFloat(parts[2].trim()) / 255f;
            return new Color(r, g, b, 1f);
        } else if (parts.length == 4) {
            float r = Float.parseFloat(parts[0].trim()) / 255f;
            float g = Float.parseFloat(parts[1].trim()) / 255f;
            float b = Float.parseFloat(parts[2].trim()) / 255f;
            float a = Float.parseFloat(parts[3].trim()) / 255f;
            return new Color(r, g, b, a);
        }

        // Try named color (e.g., "BLUE", "gold", "Scarlet")
        String upperName = str.toUpperCase();
        try {
            Field field = Color.class.getField(upperName);
            return new Color((Color) field.get(null));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException("Unknown color: " + str);
        }
    }

    private static Vec3i parseVec3i(String str) {
        String[] parts = str.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid Vec3i format: " + str);
        }

        int x = Integer.parseInt(parts[0].trim());
        int y = Integer.parseInt(parts[1].trim());
        int z = Integer.parseInt(parts[2].trim());
        return new Vec3i(x, y, z);
    }
}
