package com.workflow.engine.controller;

import com.workflow.engine.dto.workflow.EdgeRequest;
import com.workflow.engine.dto.workflow.NodeRequest;
import com.workflow.engine.dto.workflow.WorkflowRequest;
import com.workflow.engine.model.Edge;
import com.workflow.engine.model.Node;
import com.workflow.engine.model.Workflow;
import com.workflow.engine.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:3000", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    // --- WORKFLOW APIs ---
    @PostMapping
    public ResponseEntity<Workflow> createWorkflow(@Valid @RequestBody WorkflowRequest request) {
        return ResponseEntity.ok(workflowService.createWorkflow(request));
    }

    @GetMapping
    public ResponseEntity<List<Workflow>> getAllWorkflows() {
        return ResponseEntity.ok(workflowService.getAllWorkflows());
    }

    // --- NODE APIs ---
    @PostMapping("/{workflowId}/nodes")
    public ResponseEntity<Node> addNodeToWorkflow(
            @PathVariable UUID workflowId, 
            @Valid @RequestBody NodeRequest request) {
        return ResponseEntity.ok(workflowService.addNode(workflowId, request));
    }

    @GetMapping("/{workflowId}/nodes")
    public ResponseEntity<List<Node>> getNodes(@PathVariable UUID workflowId) {
        return ResponseEntity.ok(workflowService.getNodesByWorkflow(workflowId));
    }

    // --- EDGE APIs ---
    @PostMapping("/{workflowId}/edges")
    public ResponseEntity<Edge> addEdgeToWorkflow(
            @PathVariable UUID workflowId, 
            @Valid @RequestBody EdgeRequest request) {
        return ResponseEntity.ok(workflowService.addEdge(workflowId, request));
    }

    @GetMapping("/{workflowId}/edges")
    public ResponseEntity<List<Edge>> getEdges(@PathVariable UUID workflowId) {
        return ResponseEntity.ok(workflowService.getEdgesByWorkflow(workflowId));
    }
}
