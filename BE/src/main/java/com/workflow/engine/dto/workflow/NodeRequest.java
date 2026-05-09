package com.workflow.engine.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.UUID;

@Data
public class NodeRequest {
    @NotBlank(message = "Tên Node không được để trống")
    private String name;

    private Boolean isStart = false;
    private Boolean isEnd = false;
}
