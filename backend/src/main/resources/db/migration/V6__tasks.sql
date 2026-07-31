-- V6: Tasks

CREATE TABLE IF NOT EXISTS tasks (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL DEFAULT 'default',
    title       VARCHAR(500) NOT NULL,
    description TEXT,
    matter_id   BIGINT       REFERENCES matters(id) ON DELETE SET NULL,
    matter_title VARCHAR(500),
    assigned_to VARCHAR(200),
    type        VARCHAR(100),
    priority    VARCHAR(20)  NOT NULL DEFAULT 'Medium',
    due_date    DATE,
    done        BOOLEAN      NOT NULL DEFAULT FALSE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'Open',
    created_by  VARCHAR(200),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_tasks_tenant ON tasks(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tasks_matter ON tasks(matter_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_tasks_due    ON tasks(due_date);

INSERT INTO tasks (tenant_id, title, matter_title, assigned_to, type, priority, due_date, done, status) VALUES
('default', 'File Rejoinder in Rahul Gupta case', 'Rahul Gupta vs State of Delhi', 'Adv. Priya Kapoor', 'Filing', 'Urgent', CURRENT_DATE, FALSE, 'Overdue'),
('default', 'Prepare Written Arguments', 'Infosys Ltd. Tax Appeal', 'Adv. Amit Sharma', 'Document Drafting', 'High', CURRENT_DATE + 2, FALSE, 'Open'),
('default', 'Client meeting - TechStart', 'TechStart IP Dispute', 'Adv. Amit Sharma', 'Client Meeting', 'Medium', CURRENT_DATE + 3, FALSE, 'Open'),
('default', 'Research on IP precedents', 'TechStart IP Dispute', 'Ravi Mehta', 'Research', 'Medium', CURRENT_DATE + 5, FALSE, 'Open'),
('default', 'Draft Vakalatnama', 'Sunita Sharma Matrimonial Case', 'Ravi Mehta', 'Document Drafting', 'Low', CURRENT_DATE + 7, TRUE, 'Closed');
