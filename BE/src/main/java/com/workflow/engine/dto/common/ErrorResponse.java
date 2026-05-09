package com.workflow.engine.dto.common;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    
    // Dành riêng cho lỗi validation (ví dụ: email sai format, pass ngắn)
    private List<String> validationErrors; 
}
