package ca.kieve.ssss.content;

import java.util.List;

public record MapEntityDefinition(String id, List<ComponentDefinition> components) {
    public MapEntityDefinition {
        components = components != null ? components : List.of();
    }
}
