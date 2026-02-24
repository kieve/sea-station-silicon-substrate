package ca.kieve.ssss.content;

import java.util.Map;

import ca.kieve.ssss.ai.condition.Condition;
import ca.kieve.ssss.ai.reset.ResetCondition;
import ca.kieve.ssss.ai.state.AiState;

/**
 * Centralized factory for reflection-based instantiation of YAML-defined classes.
 * Handles class lookup, instantiation, and initialization for AI states, conditions,
 * and reset conditions.
 */
public final class ReflectionFactory {
    private static final String STATE_PACKAGE = "ca.kieve.ssss.ai.state.";
    private static final String CONDITION_PACKAGE = "ca.kieve.ssss.ai.condition.";
    private static final String RESET_PACKAGE = "ca.kieve.ssss.ai.reset.";

    private ReflectionFactory() {}

    /**
     * Creates an AiState instance from its class name.
     *
     * @param stateName The state class name (e.g., "ChaseState" or "IdleState")
     * @param properties Properties to initialize the state with
     * @return The instantiated and initialized AiState
     */
    public static AiState createState(String stateName, Map<String, Object> properties) {
        return create(STATE_PACKAGE, stateName, null, AiState.class, properties);
    }

    /**
     * Creates a Condition instance from its type name.
     *
     * @param typeName The condition type (e.g., "IsDead" or "IsDeadCondition")
     * @param properties Properties to initialize the condition with
     * @return The instantiated and initialized Condition
     */
    public static Condition createCondition(String typeName, Map<String, Object> properties) {
        return create(CONDITION_PACKAGE, typeName, "Condition", Condition.class, properties);
    }

    /**
     * Creates a ResetCondition instance from its type name.
     *
     * @param typeName The reset condition type (e.g., "AttackerChange" or "AttackerChangeReset")
     * @param properties Properties to initialize the reset condition with
     * @return The instantiated and initialized ResetCondition
     */
    public static ResetCondition createResetCondition(
        String typeName,
        Map<String, Object> properties
    ) {
        return create(RESET_PACKAGE, typeName, "Reset", ResetCondition.class, properties);
    }

    /**
     * Generic method to create and initialize a YamlInitializable instance.
     *
     * @param packagePrefix The package prefix for the class
     * @param className The class name (may or may not include suffix)
     * @param suffix Optional suffix to append if not already present (null for no suffix)
     * @param expectedType The expected type of the created instance
     * @param properties Properties to initialize the instance with
     * @param <T> The type of YamlInitializable to create
     * @return The instantiated and initialized instance
     */
    private static <T extends YamlInitializable> T create(
        String packagePrefix,
        String className,
        String suffix,
        Class<T> expectedType,
        Map<String, Object> properties
    ) {
        try {
            String fullClassName = buildClassName(packagePrefix, className, suffix);
            Class<?> clazz = Class.forName(fullClassName);

            if (!expectedType.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException(
                    "Class " + fullClassName + " does not implement " + expectedType.getSimpleName()
                );
            }

            @SuppressWarnings("unchecked")
            T instance = (T) clazz.getDeclaredConstructor().newInstance();
            instance.initialize(properties);
            return instance;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance: " + className, e);
        }
    }

    private static String buildClassName(String packagePrefix, String className, String suffix) {
        if (suffix != null && !className.endsWith(suffix)) {
            return packagePrefix + className + suffix;
        }
        return packagePrefix + className;
    }
}
