package ca.kieve.ssss.content;

import java.util.Map;

/**
 * Interface for classes that can be instantiated via reflection from YAML definitions.
 * Implementations receive their configuration properties from YAML via the initialize method.
 */
public interface YamlInitializable {
    /**
     * Initializes this instance with properties parsed from YAML.
     *
     * @param properties Map of property names to values from the YAML definition
     */
    void initialize(Map<String, Object> properties);
}
