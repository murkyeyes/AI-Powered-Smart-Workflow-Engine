package com.workflow.engine.service.strategy;

import com.workflow.engine.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoverageRuleValidatorTest {

    private CoverageRuleValidator validator;
    private Task mockTask; // Task trong context validator hiện tại không dùng để modify, giả lập rỗng là đủ

    @BeforeEach
    void setUp() {
        validator = new CoverageRuleValidator();
        mockTask = new Task();
    }

    // kịch bản 1 (True): Payload CÓ test_coverage VÀ >= expected_value -> PASS
    @Test
    void shouldPass_WhenPayloadHasCoverage_And_ValueMeetsRequirement() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("test_coverage", 85.0);

        Object expectedRuleValue = 80.0;

        assertDoesNotThrow(() -> validator.validate(mockTask, payload, expectedRuleValue));
    }

    // kịch bản 2 (False 1): Payload KHÔNG CÓ test_coverage -> Fail
    @Test
    void shouldThrowException_WhenPayloadMissingCoverage() {
        Map<String, Object> payload = new HashMap<>(); // Empty payload

        Object expectedRuleValue = 80.0;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> validator.validate(mockTask, payload, expectedRuleValue));
        
        assertEquals("Vui lòng cung cấp giá trị 'test_coverage' để chuyển sang trạng thái này.", exception.getMessage());
    }

    // kịch bản 3 (False 2): Payload CÓ test_coverage NHƯNG < expected_value -> Fail
    @Test
    void shouldThrowException_WhenCoverageBelowRequirement() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("test_coverage", 75.0);

        Object expectedRuleValue = 80.0;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> validator.validate(mockTask, payload, expectedRuleValue));
        
        assertEquals("Code coverage hiện tại (75.0%) không đạt tiêu chuẩn tối thiểu cấu hình trong Workflow (80.0%).", exception.getMessage());
    }
}
