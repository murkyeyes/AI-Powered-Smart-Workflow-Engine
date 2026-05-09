package com.workflow.engine.repository;

import com.workflow.engine.model.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistory, UUID> {
}
