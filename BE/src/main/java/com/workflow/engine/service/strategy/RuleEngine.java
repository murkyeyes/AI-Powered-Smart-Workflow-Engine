package com.workflow.engine.service.strategy;

import com.workflow.engine.model.Task;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RuleEngine {

    private final Map<String, RuleValidator> validators;

    // Spring tự động tiêm toàn bộ các class RuleValidator vào đây bằng DI
    public RuleEngine(List<RuleValidator> validatorList) {
        this.validators = validatorList.stream()
                .collect(Collectors.toMap(RuleValidator::getRuleKey, Function.identity()));
    }

    public void evaluateRules(Task task, Map<String, Object> definedRules, Map<String, Object> clientPayload) {
        if (definedRules == null || definedRules.isEmpty()) return;

        // Quét từng luật trong chuỗi cấu hình JSONB
        for (Map.Entry<String, Object> rule : definedRules.entrySet()) {
            RuleValidator validator = validators.get(rule.getKey());
            
            if (validator != null) {
                // Đưa thông tin cho class Validator tương ứng phân tích và đánh giá
                // Nếu quy tắc bị vi phạm, validator sẽ tự throw IllegalArgumentException
                validator.validate(task, clientPayload, rule.getValue());
            }
        }
    }
}
