package com.workflow.engine.service;

import com.workflow.engine.dto.task.TaskTransitionRequest;
import com.workflow.engine.model.Edge;
import com.workflow.engine.model.Node;
import com.workflow.engine.model.Task;
import com.workflow.engine.repository.EdgeRepository;
import com.workflow.engine.repository.NodeRepository;
import com.workflow.engine.repository.TaskHistoryRepository;
import com.workflow.engine.repository.TaskRepository;
import com.workflow.engine.repository.UserRepository;
import com.workflow.engine.service.strategy.RuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private EdgeRepository edgeRepository;
    @Mock private NodeRepository nodeRepository;
    @Mock private TaskHistoryRepository taskHistoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private RuleEngine ruleEngine;

    @InjectMocks
    private TaskService taskService;

    private Task mockTask;
    private Node nodeA;
    private Node nodeB;
    private Edge mockEdge;
    private TaskTransitionRequest mockRequest;
    private final String currentUsername = "tester";

    @BeforeEach
    void setUp() {
        nodeA = new Node();
        nodeA.setId(UUID.randomUUID());

        nodeB = new Node();
        nodeB.setId(UUID.randomUUID());

        mockTask = new Task();
        mockTask.setId(UUID.randomUUID());
        mockTask.setCurrentNode(nodeA);

        mockEdge = new Edge();
        mockEdge.setId(UUID.randomUUID());
        mockEdge.setSourceNode(nodeA);
        mockEdge.setTargetNode(nodeB);

        mockRequest = new TaskTransitionRequest();
        mockRequest.setEdgeId(mockEdge.getId());
    }

    // kịch bản 1: Đạt điều kiện chuyển (Happy Path) -> Verify task() update target node & history generated
    @Test
    void transitionTask_Success_UpdatesNodeAndHistory() {
        when(taskRepository.findById(mockTask.getId())).thenReturn(Optional.of(mockTask));
        when(edgeRepository.findById(mockRequest.getEdgeId())).thenReturn(Optional.of(mockEdge));
        when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.empty());

        taskService.transitionTask(mockTask.getId(), mockRequest, currentUsername);

        assertEquals(nodeB, mockTask.getCurrentNode());
        verify(taskRepository, times(1)).save(mockTask);
        verify(taskHistoryRepository, times(1)).save(any());
        verify(ruleEngine, times(1)).evaluateRules(any(), any(), any()); // Có truyền map dù rỗng
    }

    // kịch bản 2: Fast-Fail: Thả lại chính nó (Current Node == Target Node của URL/Edge)
    @Test
    void transitionTask_FastFail_WhenSourceSameAsTarget() {
        // Gán edge source = target = nodeA -> simulate việc kéo thả nhầm vào chính cục đó
        mockEdge.setTargetNode(nodeA);
        when(taskRepository.findById(mockTask.getId())).thenReturn(Optional.of(mockTask));
        when(edgeRepository.findById(mockRequest.getEdgeId())).thenReturn(Optional.of(mockEdge));

        taskService.transitionTask(mockTask.getId(), mockRequest, currentUsername);

        // Đảm bảo method ngừng ngay, KHÔNG save DB
        verify(taskRepository, never()).save(any());
        verify(taskHistoryRepository, never()).save(any());
    }

    // kịch bản 3: Fraud/Invalid Edge - Gửi lên một EdgeId hợp lệ NHƯNG sourceNode của Edge đó khác CurrentNode
    @Test
    void transitionTask_ThrowsException_WhenEdgeSourceDoesNotMatchCurrentNode() {
        Node nodeC = new Node();
        nodeC.setId(UUID.randomUUID());
        mockEdge.setSourceNode(nodeC); // Edge này xuất phát từ C, trong khi Task đang ở A

        when(taskRepository.findById(mockTask.getId())).thenReturn(Optional.of(mockTask));
        when(edgeRepository.findById(mockRequest.getEdgeId())).thenReturn(Optional.of(mockEdge));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskService.transitionTask(mockTask.getId(), mockRequest, currentUsername));

        assertEquals("Trạng thái chuyển không hợp lệ: Đường đi không xuất phát từ trạng thái hiện tại.", exception.getMessage());
    }
}