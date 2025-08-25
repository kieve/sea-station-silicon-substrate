package ca.kieve.ssss.component;

import ca.kieve.ssss.util.HasDescription;

public enum Material implements Component, HasDescription {
    WOOD("Wood"),
    STONE("Stone"),
    STEEL("Steel"),
    ;

    private final String description;

    Material(final String description) {
        this.description = description;
    }

    @Override
    public String description() {
        return description;
    }
}
