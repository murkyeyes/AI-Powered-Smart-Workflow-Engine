package com.workflow.engine.service;

import com.workflow.engine.model.Edge;
import com.workflow.engine.repository.EdgeRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CycleDetectionService {

    private final EdgeRepository edgeRepository;

    public CycleDetectionService(EdgeRepository edgeRepository) {
        this.edgeRepository = edgeRepository;
    }

    /**
     * Kiem tra xem neu them (sourceNodeId -> targetNodeId) thi có sinh ra chu trinh ko.
     * DFS (Depth First Search) check graph cycles.
     */
    public void validateNoCycle(UUID workflowId, UUID newSourceNodeId, UUID newTargetNodeId) {
        if (newSourceNodeId.equals(newTargetNodeId)) {
            throw new IllegalArgumentException("Không thể nối một Node với chính nó (Self-loop cycle).");
        }

        List<Edge> existingEdges = edgeRepository.findByWorkflowId(workflowId);

        // Xây dựng Danh sách Kề (Adjacency List)
        Map<UUID, List<UUID>> graph = new HashMap<>();
        for (Edge edge : existingEdges) {
            graph.computeIfAbsent(edge.getSourceNode().getId(), k -> new ArrayList<>())
                 .add(edge.getTargetNode().getId());
        }

        // Bổ sung Edge giả định vào đồ thị.
        graph.computeIfAbsent(newSourceNodeId, k -> new ArrayList<>())
             .add(newTargetNodeId);

        // Duyệt toàn bộ Graph để phát hiện Cycle
        Set<UUID> visited = new HashSet<>();
        Set<UUID> recursionStack = new HashSet<>();

        for (UUID node : graph.keySet()) {
            if (hasCycleDFS(node, graph, visited, recursionStack)) {
                throw new IllegalArgumentException(
                        "Lỗi nối Edge: Hành động này sẽ tạo ra Vòng lặp vô tận (Cycle) giữa các Node, dẫn đến Infinite Loop."
                );
            }
        }
    }

    private boolean hasCycleDFS(UUID current, Map<UUID, List<UUID>> graph, 
                                Set<UUID> visited, Set<UUID> recursionStack) {
        if (recursionStack.contains(current)) {
            return true; // Gặp lại đỉnh đang nằm trong stack -> Phát hiện chu trình
        }
        if (visited.contains(current)) {
            return false; // Đã duyệt qua đỉnh này và ko có chu trình
        }

        visited.add(current);
        recursionStack.add(current);

        List<UUID> neighbors = graph.getOrDefault(current, Collections.emptyList());
        for (UUID neighbor : neighbors) {
            if (hasCycleDFS(neighbor, graph, visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(current);
        return false; // Backtrack an toàn
    }
}
