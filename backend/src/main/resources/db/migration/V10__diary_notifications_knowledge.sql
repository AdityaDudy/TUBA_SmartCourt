-- V10: Diary Events + Notifications + Knowledge Base

CREATE TABLE IF NOT EXISTS diary_events (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       VARCHAR(100) NOT NULL DEFAULT 'default',
    title           VARCHAR(500) NOT NULL,
    event_date      DATE         NOT NULL,
    event_time      VARCHAR(10),
    type            VARCHAR(50)  NOT NULL DEFAULT 'hearing',
    matter_id       BIGINT       REFERENCES matters(id) ON DELETE SET NULL,
    matter_title    VARCHAR(500),
    client_id       BIGINT,
    client_name     VARCHAR(255),
    owner_id        BIGINT,
    owner_name      VARCHAR(255),
    created_by      BIGINT,
    court           VARCHAR(200),
    notes           TEXT,
    urgent          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_diary_tenant ON diary_events(tenant_id, event_date);

CREATE TABLE IF NOT EXISTS notifications (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL DEFAULT 'default',
    user_id     BIGINT       REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(300) NOT NULL,
    message     TEXT,
    type        VARCHAR(50)  DEFAULT 'info',
    link        VARCHAR(500),
    read        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_notif_user ON notifications(user_id, read);

CREATE TABLE IF NOT EXISTS knowledge_items (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL DEFAULT 'default',
    category    VARCHAR(50)  NOT NULL,
    title       VARCHAR(500) NOT NULL,
    court       VARCHAR(200),
    citation    VARCHAR(300),
    year        INT,
    author      VARCHAR(200),
    date        DATE,
    summary     TEXT,
    content     TEXT,
    tags        TEXT[],
    doc_type    VARCHAR(100),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_ki_tenant   ON knowledge_items(tenant_id, category);
CREATE INDEX IF NOT EXISTS idx_ki_fts      ON knowledge_items USING GIN(to_tsvector('english', title || ' ' || COALESCE(summary, '')));

-- Seed diary events
INSERT INTO diary_events (tenant_id, title, event_date, event_time, type, matter_title, court, urgent) VALUES
('default', 'Delhi HC Hearing - WP/1234/2025', CURRENT_DATE, '10:30', 'hearing', 'Rahul Gupta vs State of Delhi', 'Delhi High Court', TRUE),
('default', 'ITAT Hearing - Infosys Appeal', CURRENT_DATE, '11:00', 'hearing', 'Infosys Ltd. Tax Appeal', 'ITAT Delhi', TRUE),
('default', 'Client Meeting - TechStart', CURRENT_DATE + 3, '14:00', 'meeting', 'TechStart IP Dispute', NULL, FALSE);

-- Seed notifications
INSERT INTO notifications (tenant_id, user_id, title, message, type, read)
SELECT 'default', id, '3 urgent hearings today', 'SC Bench 4, NCLT Mumbai, Delhi HC need your attention', 'warning', FALSE
FROM users WHERE email = 'amit@tubalaw.com';

-- Seed knowledge items
INSERT INTO knowledge_items (tenant_id, category, title, court, citation, year, summary) VALUES
('default', 'judgment', 'Maneka Gandhi vs Union of India', 'Supreme Court of India', 'AIR 1978 SC 597', 1978, 'Landmark judgment expanding the scope of Article 21 — right to life and personal liberty.'),
('default', 'judgment', 'K.S. Puttaswamy vs Union of India', 'Supreme Court of India', '(2017) 10 SCC 1', 2017, 'Right to Privacy declared a fundamental right under Article 21.'),
('default', 'template', 'Writ Petition Template', NULL, NULL, 2024, 'Standard writ petition format for High Courts and Supreme Court.'),
('default', 'template', 'Vakalatnama Template', NULL, NULL, 2024, 'Standard vakalatnama format for all courts.'),
('default', 'article', 'DPDP Act 2023 — Key Compliance Obligations', NULL, NULL, 2024, 'Comprehensive analysis of Digital Personal Data Protection Act obligations for law firms.');
