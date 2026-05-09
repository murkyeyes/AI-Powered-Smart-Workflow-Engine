package com.workflow.engine.dto.workflow;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;
import java.util.UUID;

@Data
public class EdgeRequest {
    @NotNull(message = "Source Node ID không được để trống")
    private UUID sourceNodeId;

    @NotNull(message = "Target Node ID không được để trống")
    private UUID targetNodeId;

    private Map<String, Object> rules;
}
