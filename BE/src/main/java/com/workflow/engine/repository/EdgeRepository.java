package com.workflow.engine.repository;

import com.workflow.engine.model.Edge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EdgeRepository extends JpaRepository<Edge, UUID> {
    List<Edge> findByWorkflowId(UUID workflowId);
    boolean existsBySourceNodeIdAndTargetNodeId(UUID sourceNodeId, UUID targetNodeId);
    java.util.Optional<Edge> findBySourceNodeIdAndTargetNodeId(UUID sourceNodeId, UUID targetNodeId);
}
