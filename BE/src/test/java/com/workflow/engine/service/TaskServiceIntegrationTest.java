package com.workflow.engine.service;

import com.workflow.engine.dto.task.TaskTransitionRequest;
import com.workflow.engine.model.Edge;
import com.workflow.engine.model.Node;
import com.workflow.engine.model.Task;
import com.workflow.engine.model.User;
import com.workflow.engine.model.Workflow;
import com.workflow.engine.repository.EdgeRepository;
import com.workflow.engine.repository.NodeRepository;
import com.workflow.engine.repository.TaskRepository;
import com.workflow.engine.repository.UserRepository;
import com.workflow.engine.repository.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;

@SpringBootTest
@Testcontainers

class TaskServiceIntegrationTest {

    // Gotcha 2: Sử dụng Testcontainers để dựng 1 DB PostgreSQL thật thay vì H2 (giải quyết lỗi JSONB)
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("workflow_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update"); // Tự động tạo bảng theo Entity cho test 
    }

    @Autowired private TaskService taskService;
    @Autowired private TaskRepository taskRepository;
    @Autowired private EdgeRepository edgeRepository;
    @Autowired private NodeRepository nodeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WorkflowRepository workflowRepository;

    private Task testTask;
    private Edge testEdge;

    @BeforeEach
    void setUp() {
        // Clean up dữ liệu test cũ
        edgeRepository.deleteAll();
        taskRepository.deleteAll();
        nodeRepository.deleteAll();
        workflowRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Tạo User
        User user = new User();
        user.setUsername("testadmin");
        user.setPasswordHash("hashed_pass");
        user = userRepository.save(user);

        // 2. Tạo Workflow
        Workflow workflow = new Workflow();
        workflow.setName("Test Workflow");
        workflow = workflowRepository.save(workflow);

        // 3. Tạo 2 Node (Source, Target)
        Node sourceNode = new Node();
        sourceNode.setWorkflow(workflow);
        sourceNode.setName("TODO");
        sourceNode = nodeRepository.save(sourceNode);

        Node targetNode = new Node();
        targetNode.setWorkflow(workflow);
        targetNode.setName("DONE");
        targetNode = nodeRepository.save(targetNode);

        // 4. Tạo Edge nối TODO -> DONE (Có rule null để bỏ qua validation trong DB)
        testEdge = new Edge();
        testEdge.setWorkflow(workflow);
        testEdge.setSourceNode(sourceNode);
        testEdge.setTargetNode(targetNode);
        testEdge = edgeRepository.save(testEdge);

        // 5. Tạo 1 Task đang nằm ở TODO
        testTask = new Task();
        testTask.setWorkflow(workflow);
        testTask.setCurrentNode(sourceNode);
        testTask.setTitle("Spam Click Test Task");
        testTask.setAssignedTo(user);
        testTask = taskRepository.save(testTask);
    }

    // Gotcha 1: Sử dụng @WithMockUser để Spring Security tự động thiết lập SecurityContext, tránh NullPointerException
    // Gotcha 3: Testing Race Condition (Optimistic Locking) bằng Concurrent Threading
    @Test
    @WithMockUser(username = "testadmin")
    void transitionTask_ShouldThrowOptimisticLockingFailure_OnRaceCondition() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1); // Block tất cả thread chờ hiệu lệnh
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads); // Đợi tất cả thread chạy xong

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    latch.await(); // Luồng chờ tại đây cho đến khi cờ được mở

                    TaskTransitionRequest request = new TaskTransitionRequest();
                    request.setEdgeId(testEdge.getId());

                    // Gọi API - giả lập spam click
                    taskService.transitionTask(testTask.getId(), request, "testadmin");
                    successCount.incrementAndGet(); // Chỉ 1 thread được lọt
                } catch (OptimisticLockingFailureException e) {
                    conflictCount.incrementAndGet(); // 9 Threads còn lại sẽ văng lỗi này
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // KICK OFF: 10 Thread cùng lúc lao vào chạy
        latch.countDown();
        
        // Đợi cả 10 thread chạy xong
        doneLatch.await();

        // Kiểm tra kết quả: Có ĐÚNG 1 thread thành công ghi DB, 9 thread bị đá văng!
        assertEquals(1, successCount.get(), "Chỉ có 1 request được xử lý thành công nhờ Version chặn lại");
        assertEquals(numberOfThreads - 1, conflictCount.get(), "Số lượng request bị đá ra phải là 9");

        // Verify lại dòng dữ liệu dưới Testcontainers DB thật
        Optional<Task> savedTask = taskRepository.findById(testTask.getId());
        assertEquals(1, savedTask.get().getVersion(), "Version của Task phải được update từ 0 lên 1");
    }
}
