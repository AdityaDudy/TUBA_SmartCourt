CREATE TABLE IF NOT EXISTS report_generations (
    id              BIGSERIAL    PRIMARY KEY,
    report_type     VARCHAR(150) NOT NULL,
    filters_used    VARCHAR(500),
    generated_by    VARCHAR(150) DEFAULT 'Adv. Amit Sharma',
    generated_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    format          VARCHAR(50)  NOT NULL
);

CREATE TABLE IF NOT EXISTS report_schedules (
    id              BIGSERIAL    PRIMARY KEY,
    report_type     VARCHAR(150) NOT NULL,
    filters_used    VARCHAR(500),
    frequency       VARCHAR(100) NOT NULL,
    email_recipient VARCHAR(250) NOT NULL,
    last_run        TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
