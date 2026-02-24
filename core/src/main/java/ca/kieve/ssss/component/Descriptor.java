package ca.kieve.ssss.component;

import ca.kieve.ssss.util.HasDescription;
import ca.kieve.ssss.util.HasName;

public record Descriptor(
    String name,
    String description
) implements Component, HasDescription, HasName {
}
