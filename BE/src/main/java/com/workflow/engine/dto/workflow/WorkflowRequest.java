package com.workflow.engine.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkflowRequest {
    @NotBlank(message = "Tên Workflow không được để trống")
    private String name;

    private String description;
}
