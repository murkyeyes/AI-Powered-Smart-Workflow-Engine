package com.workflow.engine.service.strategy;

import com.workflow.engine.model.Task;
import java.util.Map;

public interface RuleValidator {
    // Tên của quy tắc trong chuỗi JSON (ví dụ: "min_coverage")
    String getRuleKey(); 
    
    // Hàm thực thi kiểm tra
    void validate(Task task, Map<String, Object> payloadData, Object expectedRuleValue);
}
