package com.workflow.engine.service.strategy;

import com.workflow.engine.model.Task;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class CoverageRuleValidator implements RuleValidator {

    @Override
    public String getRuleKey() {
        return "min_coverage"; // Key này sẽ map với key trong cột JSONB rules
    }

    @Override
    public void validate(Task task, Map<String, Object> payloadData, Object expectedRuleValue) {
        // 1. Lấy mức coverage tối thiểu từ Rule cấu hình
        double requiredCoverage = Double.parseDouble(expectedRuleValue.toString());

        // 2. Lấy mức coverage thực tế mà người dùng gửi lên qua API Payload
        if (!payloadData.containsKey("test_coverage")) {
            throw new IllegalArgumentException("Vui lòng cung cấp giá trị 'test_coverage' để chuyển sang trạng thái này.");
        }
        
        double actualCoverage = Double.parseDouble(payloadData.get("test_coverage").toString());

        // 3. So sánh
        if (actualCoverage < requiredCoverage) {
            throw new IllegalArgumentException(
                "Code coverage hiện tại (" + actualCoverage + "%) không đạt tiêu chuẩn tối thiểu cấu hình trong Workflow (" + requiredCoverage + "%)."
            );
        }
    }
}