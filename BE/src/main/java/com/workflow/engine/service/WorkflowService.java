package com.workflow.engine.service;

import com.workflow.engine.dto.workflow.EdgeRequest;
import com.workflow.engine.dto.workflow.NodeRequest;
import com.workflow.engine.dto.workflow.WorkflowRequest;
import com.workflow.engine.model.Edge;
import com.workflow.engine.model.Node;
import com.workflow.engine.model.Workflow;
import com.workflow.engine.repository.EdgeRepository;
import com.workflow.engine.repository.NodeRepository;
import com.workflow.engine.repository.WorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;
    private final CycleDetectionService cycleDetectionService;

    public WorkflowService(WorkflowRepository workflowRepository, NodeRepository nodeRepository,
                           EdgeRepository edgeRepository, CycleDetectionService cycleDetectionService) {
        this.workflowRepository = workflowRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.cycleDetectionService = cycleDetectionService;
    }

    // --- WORKFLOW CRUD ---
    public Workflow createWorkflow(WorkflowRequest req) {
        Workflow w = new Workflow();
        w.setName(req.getName());
        w.setDescription(req.getDescription());
        return workflowRepository.save(w);
    }

    public List<Workflow> getAllWorkflows() {
        return workflowRepository.findAll();
    }

    // --- NODE CRUD ---
    public Node addNode(UUID workflowId, NodeRequest req) {
        Workflow w = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow không tồn tại"));

        Node n = new Node();
        n.setWorkflow(w);
        n.setName(req.getName());
        n.setIsStart(req.getIsStart());
        n.setIsEnd(req.getIsEnd());
        return nodeRepository.save(n);
    }

    public List<Node> getNodesByWorkflow(UUID workflowId) {
        return nodeRepository.findByWorkflowId(workflowId);
    }

    // --- EDGE CRUD ---
    @Transactional
    public Edge addEdge(UUID workflowId, EdgeRequest req) {
        Workflow w = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow không tồn tại"));

        Node source = nodeRepository.findById(req.getSourceNodeId())
                .orElseThrow(() -> new RuntimeException("Source Node không tồn tại"));
        
        Node target = nodeRepository.findById(req.getTargetNodeId())
                .orElseThrow(() -> new RuntimeException("Target Node không tồn tại"));

        if (!source.getWorkflow().getId().equals(workflowId) || 
            !target.getWorkflow().getId().equals(workflowId)) {
            throw new IllegalArgumentException("Các Node không thuộc cùng một Workflow");
        }

        if (edgeRepository.existsBySourceNodeIdAndTargetNodeId(source.getId(), target.getId())) {
            throw new IllegalArgumentException("Edge này đã tồn tại.");
        }

        // Chặn Cycle (Graph Cycle Detection)
        cycleDetectionService.validateNoCycle(workflowId, source.getId(), target.getId());

        Edge edge = new Edge();
        edge.setWorkflow(w);
        edge.setSourceNode(source);
        edge.setTargetNode(target);
        if (req.getRules() != null) {
            edge.setRules(req.getRules());
        }

        return edgeRepository.save(edge);
    }

    public List<Edge> getEdgesByWorkflow(UUID workflowId) {
        return edgeRepository.findByWorkflowId(workflowId);
    }
}
