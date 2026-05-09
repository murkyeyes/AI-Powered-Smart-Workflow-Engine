package com.workflow.engine.service;

import com.workflow.engine.dto.task.TaskTransitionRequest;
import com.workflow.engine.model.Edge;
import com.workflow.engine.model.Node;
import com.workflow.engine.model.Task;
import com.workflow.engine.model.TaskHistory;
import com.workflow.engine.model.User;
import com.workflow.engine.repository.EdgeRepository;
import com.workflow.engine.repository.NodeRepository;
import com.workflow.engine.repository.TaskHistoryRepository;
import com.workflow.engine.repository.TaskRepository;
import com.workflow.engine.repository.UserRepository;
import com.workflow.engine.service.strategy.RuleEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final EdgeRepository edgeRepository;
    private final NodeRepository nodeRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final UserRepository userRepository;
    private final RuleEngine ruleEngine;

    public TaskService(TaskRepository taskRepository,
                       EdgeRepository edgeRepository,
                       NodeRepository nodeRepository,
                       TaskHistoryRepository taskHistoryRepository,
                       UserRepository userRepository,
                       RuleEngine ruleEngine) {
        this.taskRepository = taskRepository;
        this.edgeRepository = edgeRepository;
        this.nodeRepository = nodeRepository;
        this.taskHistoryRepository = taskHistoryRepository;
        this.userRepository = userRepository;
        this.ruleEngine = ruleEngine;
    }

    @Transactional
    public void transitionTask(UUID taskId, TaskTransitionRequest request, String currentUsername) {
        // 1. Tìm kiếm Task
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Task với ID: " + taskId));

        Node currentNode = task.getCurrentNode();

        // 2. Tìm kiếm Edge thay vì Target Node (Linh hoạt hóa nhiều đường chuyển)
        Edge edge = edgeRepository.findById(request.getEdgeId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đường nối (Edge) với ID: " + request.getEdgeId()));
                
        Node targetNode = edge.getTargetNode();

        // 3. Fast-Fail: Đỡ đòn do Front-end giật lag user thả lại vị trí cũ
        if (currentNode.getId().equals(targetNode.getId())) {
            return;
        }

        // Đảm bảo Edge được gọi thực sự có Source là Current Node của Task
        if (!edge.getSourceNode().getId().equals(currentNode.getId())) {
            throw new IllegalArgumentException("Trạng thái chuyển không hợp lệ: Đường đi không xuất phát từ trạng thái hiện tại.");
        }

        // 4. Giải mã rules JSONB từ Edge và đẩy cho RuleEngine đánh giá cùng Payload
        if (edge.getRules() != null && !edge.getRules().isEmpty()) {
            Map<String, Object> payloadData = request.getPayloadData() != null ? request.getPayloadData() : Map.of();
            ruleEngine.evaluateRules(task, edge.getRules(), payloadData);
        }

        // 5. Nếu RuleEngine pass, cập nhật Task targetNode
        task.setCurrentNode(targetNode);
        // Lưu ý: @Version ở Task sẽ quản lý đụng độ (OptimisticLockingFailureException) mỗi khi save Task
        taskRepository.save(task);

        // 6. Ghi dấu vết Audit vào TaskHistory
        User changedBy = null;
        if (currentUsername != null) {
            changedBy = userRepository.findByUsername(currentUsername).orElse(null);
        }

        TaskHistory history = new TaskHistory();
        history.setTask(task);
        history.setFromNode(currentNode);
        history.setToNode(targetNode);
        history.setReason(request.getTransitionReason());
        history.setChangedBy(changedBy);
        // changedAt được tự động thêm bởi @CreatedDate

        taskHistoryRepository.save(history);
    }
}
