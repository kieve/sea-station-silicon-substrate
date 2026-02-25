package ca.kieve.ssss.content;

public record SystemConfig(String launchMap) {
    private static final String DEFAULT_LAUNCH_MAP = "static_test_map.yaml";

    public SystemConfig {
        launchMap = launchMap != null ? launchMap : DEFAULT_LAUNCH_MAP;
    }
}
