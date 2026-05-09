package com.workflow.engine.repository;

import com.workflow.engine.model.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NodeRepository extends JpaRepository<Node, UUID> {
    List<Node> findByWorkflowId(UUID workflowId);
}
