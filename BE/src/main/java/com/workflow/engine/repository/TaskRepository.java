package com.workflow.engine.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.workflow.engine.model.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
}
