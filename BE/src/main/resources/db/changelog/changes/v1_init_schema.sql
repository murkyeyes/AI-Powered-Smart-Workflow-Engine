-- liquibase formatted sql
-- changeset admin:1
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workflows (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_version INTEGER DEFAULT 1
);

CREATE TABLE nodes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workflow_id UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    is_start BOOLEAN DEFAULT false,
    is_end BOOLEAN DEFAULT false
);
CREATE INDEX idx_nodes_workflow_id ON nodes(workflow_id);

CREATE TABLE edges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workflow_id UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    source_node_id UUID NOT NULL REFERENCES nodes(id) ON DELETE CASCADE,
    target_node_id UUID NOT NULL REFERENCES nodes(id) ON DELETE CASCADE,
    rules JSONB DEFAULT '{}'::jsonb
);
CREATE INDEX idx_edges_workflow_id ON edges(workflow_id);
CREATE INDEX idx_edges_source_target ON edges(source_node_id, target_node_id);
CREATE INDEX idx_edges_rules_gin ON edges USING GIN (rules);

CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workflow_id UUID NOT NULL REFERENCES workflows(id),
    current_node_id UUID NOT NULL REFERENCES nodes(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    version INTEGER NOT NULL DEFAULT 0,
    assigned_to UUID REFERENCES users(id),
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

CREATE TABLE task_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    from_node_id UUID REFERENCES nodes(id),
    to_node_id UUID REFERENCES nodes(id),
    changed_by UUID REFERENCES users(id),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reason TEXT
);
