-- =============================================================================
-- V4__create_analysis_results_table.sql
-- =============================================================================
-- Creates the analysis_results table.
--
-- DEPENDENCIES:
--   V1__create_users_table.sql        (users)
--   V2__create_documents_table.sql    (documents)
--   V3__create_jobs_table.sql         (jobs)
--
-- KEY DESIGN DECISIONS:
--
-- 1. key_points, risk_flags → jsonb (not TEXT[])
--    jsonb is preferred over TEXT[] in PostgreSQL because:
--    - GIN index support for future full-text search inside arrays
--    - Better query operators (@>, ?, #>>)
--    - Stores compressed binary, faster reads than TEXT[]
--    - Hibernate @JdbcTypeCode(SqlTypes.JSON) maps cleanly to jsonb
--
-- 2. UNIQUE(document_id) — one result per document at any time.
--    Reprocessing UPDATES the existing row (replaceWith() domain method).
--    This avoids growing the table unboundedly on re-analysis.
--
-- 3. confidence_score → NUMERIC(5,4) stores values like 0.9823 cleanly.
--    Nullable: not all analyzers provide it.
--
-- 4. processing_duration_ms → BIGINT (not INT).
--    Some documents take >2,147,483,647ms? No. But BIGINT is future-safe
--    and matches Java Long exactly — no casting needed.
--
-- 5. word_count → INT. Max ~2.1 billion words — more than enough for any doc.
--
-- 6. Separate GIN index on risk_flags for Phase 5 risk dashboard queries:
--    WHERE risk_flags @> '["AUTO_RENEWAL"]'::jsonb
-- =============================================================================

CREATE TABLE IF NOT EXISTS analysis_results (

    -- ── Identity ──────────────────────────────────────────────────────────
                                                id                      UUID            NOT NULL DEFAULT gen_random_uuid(),

    -- ── Relationships ─────────────────────────────────────────────────────
    -- UNIQUE: one active result per document at any time
    document_id             UUID            NOT NULL UNIQUE,

    -- Which job produced this result (traceable)
    job_id                  UUID            NOT NULL,

    -- ── Core Analysis Output ───────────────────────────────────────────────
    -- TEXT: no length limit — summaries can be long for large documents
    summary                 TEXT            NOT NULL,

    -- jsonb: binary JSON, supports GIN index, @> operators
    -- Default '[]'::jsonb ensures no NULL issues on insert
    key_points              JSONB           NOT NULL DEFAULT '[]'::jsonb,
    risk_flags              JSONB           NOT NULL DEFAULT '[]'::jsonb,

    -- ── Classification ─────────────────────────────────────────────────────
    domain                  VARCHAR(100)    NOT NULL,

    -- ── Quality / Observability ────────────────────────────────────────────
    -- NUMERIC(5,4): stores up to 9.9999 — perfect for [0.0, 1.0] scores
    confidence_score        NUMERIC(5, 4)   CHECK (confidence_score BETWEEN 0 AND 1),

    -- Wall-clock time of entire pipeline: extraction + LLM + DB write
    processing_duration_ms  BIGINT,

    -- Word count of source document text (denormalized for fast display)
    word_count              INT,

    -- ── Audit Timestamps ───────────────────────────────────────────────────
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Updated when document is reprocessed and result row is replaced
    updated_at              TIMESTAMPTZ,

    -- ── Constraints ────────────────────────────────────────────────────────
    CONSTRAINT pk_analysis_results
    PRIMARY KEY (id),

    CONSTRAINT fk_analysis_results_document
    FOREIGN KEY (document_id)
    REFERENCES documents(id)
    ON DELETE CASCADE,   -- deleting a document removes its result

    CONSTRAINT fk_analysis_results_job
    FOREIGN KEY (job_id)
    REFERENCES jobs(id)
    ON DELETE RESTRICT   -- cannot delete a job that has a result
    );

-- =============================================================================
-- INDEXES
-- =============================================================================

-- Primary lookup: fetch result for a given document (most common query)
CREATE INDEX IF NOT EXISTS idx_analysis_results_document_id
    ON analysis_results(document_id);

-- Job traceability: find result produced by a specific job
CREATE INDEX IF NOT EXISTS idx_analysis_results_job_id
    ON analysis_results(job_id);

-- Analytics: filter/group by domain
CREATE INDEX IF NOT EXISTS idx_analysis_results_domain
    ON analysis_results(domain);

-- Pagination: sort by creation time
CREATE INDEX IF NOT EXISTS idx_analysis_results_created_at
    ON analysis_results(created_at DESC);

-- Risk dashboard (Phase 5):
-- Supports: WHERE risk_flags @> '["AUTO_RENEWAL"]'::jsonb
-- GIN index is required for jsonb containment (@>) queries
CREATE INDEX IF NOT EXISTS idx_analysis_results_risk_flags_gin
    ON analysis_results USING GIN (risk_flags);

-- Key points search (Phase 5 — full-text inside JSON array)
CREATE INDEX IF NOT EXISTS idx_analysis_results_key_points_gin
    ON analysis_results USING GIN (key_points);

-- =============================================================================
-- COMMENTS (visible in pg_catalog, used by DB admin tools)
-- =============================================================================

COMMENT ON TABLE  analysis_results                      IS 'AI analysis results — one row per document (upserted on reprocessing)';
COMMENT ON COLUMN analysis_results.id                   IS 'Primary key — stable UUID across reprocessing';
COMMENT ON COLUMN analysis_results.document_id          IS 'FK to documents — UNIQUE enforces one result per document';
COMMENT ON COLUMN analysis_results.job_id               IS 'FK to job that produced this result — for traceability';
COMMENT ON COLUMN analysis_results.summary              IS 'AI-generated plain-text summary of the document';
COMMENT ON COLUMN analysis_results.key_points           IS 'JSON array of key points extracted by the analyzer';
COMMENT ON COLUMN analysis_results.risk_flags           IS 'JSON array of risk flags identified (e.g. AUTO_RENEWAL, GDPR)';
COMMENT ON COLUMN analysis_results.domain               IS 'Document domain: legal, medical, financial, general';
COMMENT ON COLUMN analysis_results.confidence_score     IS 'Analyzer confidence [0.0, 1.0] — NULL if analyzer does not provide';
COMMENT ON COLUMN analysis_results.processing_duration_ms IS 'Total pipeline time in ms (extraction + LLM + DB write)';
COMMENT ON COLUMN analysis_results.word_count           IS 'Word count of source document (denormalized for display)';
COMMENT ON COLUMN analysis_results.created_at           IS 'When result was first created (immutable)';
COMMENT ON COLUMN analysis_results.updated_at           IS 'When result was last replaced by reprocessing';