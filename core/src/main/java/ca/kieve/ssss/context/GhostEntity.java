package ca.kieve.ssss.context;

import ca.kieve.ssss.system.ExamineSystem.ExamineItem.ItemType;

public record GhostEntity(String name, String composedDescription, ItemType type) {
}
