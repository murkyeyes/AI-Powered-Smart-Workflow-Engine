package com.workflow.engine.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "nodes")
public class Node {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "is_start")
    private Boolean isStart = false;

    @Column(name = "is_end")
    private Boolean isEnd = false;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Workflow getWorkflow() { return workflow; }
    public void setWorkflow(Workflow workflow) { this.workflow = workflow; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Boolean getIsStart() { return isStart; }
    public void setIsStart(Boolean start) { isStart = start; }
    public Boolean getIsEnd() { return isEnd; }
    public void setIsEnd(Boolean end) { isEnd = end; }
}
