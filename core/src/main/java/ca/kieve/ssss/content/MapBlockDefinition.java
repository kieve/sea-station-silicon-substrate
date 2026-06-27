package ca.kieve.ssss.content;

import com.fasterxml.jackson.annotation.JsonInclude;

public record MapBlockDefinition(
    String bpId,
    char layoutChar,
    @JsonInclude(JsonInclude.Include.NON_NULL) Double waterFill,
    @JsonInclude(JsonInclude.Include.NON_NULL) Integer waterDepth
) {
    public MapBlockDefinition(String bpId, char layoutChar) {
        this(bpId, layoutChar, null, null);
    }

    public MapBlockDefinition(String bpId, char layoutChar, Double waterFill) {
        this(bpId, layoutChar, waterFill, null);
    }
}
