-- src/main/resources/db/migration/V3__create_jobs_table.sql

CREATE TYPE job_status AS ENUM ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED');

CREATE TABLE jobs (
                      id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                      document_id     UUID         NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                      status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                      attempt_count   INTEGER      NOT NULL DEFAULT 0,
                      max_attempts    INTEGER      NOT NULL DEFAULT 3,
                      error_message   TEXT,
                      started_at      TIMESTAMPTZ,
                      completed_at    TIMESTAMPTZ,
                      created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                      updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Partial index: only indexes PENDING rows (the only ones workers query)
-- Much faster and smaller than a full index on all rows
CREATE INDEX idx_jobs_status_created
    ON jobs (created_at ASC)
    WHERE status = 'PENDING';

CREATE INDEX idx_jobs_document_id ON jobs (document_id);