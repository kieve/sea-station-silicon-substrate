package ca.kieve.ssss.component;

import ca.kieve.ssss.util.Description;

public enum Material implements Component, Description {
    WOOD("Wood"),
    STONE("Stone"),
    ;

    private final String description;

    Material(final String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
