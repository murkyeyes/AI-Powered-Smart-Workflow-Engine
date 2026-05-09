package com.workflow.engine.dto.task;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class TaskTransitionRequest {
    @NotNull(message = "Edge ID không được để trống")
    private UUID edgeId;

    // Chứa các dữ liệu động để validate (ví dụ: {"test_coverage": 85, "commit_url": "..."})
    private Map<String, Object> payloadData;

    // Lý do chuyển trạng thái (tùy chọn)
    private String transitionReason;
}
