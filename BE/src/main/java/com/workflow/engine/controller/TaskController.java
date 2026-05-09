package com.workflow.engine.controller;

import com.workflow.engine.dto.common.ApiResponse;
import com.workflow.engine.dto.task.TaskTransitionRequest;
import com.workflow.engine.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/{taskId}/transition")
    public ResponseEntity<ApiResponse<String>> transitionTask(
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskTransitionRequest request,
            Authentication authentication) {

        String currentUsername = authentication != null ? authentication.getName() : null;
        taskService.transitionTask(taskId, request, currentUsername);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Chuyển trạng thái công việc thành công")
                .data("Task " + taskId + " đã ghi nhận yêu cầu di chuyển qua Edge " + request.getEdgeId())
                .build());
    }
}
