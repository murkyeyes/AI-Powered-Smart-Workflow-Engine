package com.workflow.engine.service.strategy;

import com.workflow.engine.model.Task;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class RequiredCommentRuleValidator implements RuleValidator {

    @Override
    public String getRuleKey() {
        return "require_comment"; // Key map với JSONB
    }

    @Override
    public void validate(Task task, Map<String, Object> payloadData, Object expectedRuleValue) {
        boolean isRequired = Boolean.parseBoolean(expectedRuleValue.toString());
        
        if (isRequired) {
            if (!payloadData.containsKey("comment") || payloadData.get("comment").toString().trim().isEmpty()) {
                throw new IllegalArgumentException("Luồng công việc bắt buộc tham số 'comment' (Giải trình/ghi chú) trước khi tiến hành chuyển đổi trạng thái này.");
            }
        }
    }
}
