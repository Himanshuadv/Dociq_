-- =============================================================================
-- V7__create_qa_sessions.sql
-- =============================================================================
-- Stores each question-and-answer turn a user has about a specific document.
--
-- DEPENDENCIES:
--   V1__create_users.sql      (users.id)
--   V3__create_documents.sql  (documents.id)
--
-- KEY DESIGN DECISIONS:
--
-- 1. One row per Q&A turn (not per "session"). "session" here is a naming
--    carry-over from the domain model; functionally each row is a single
--    (question, answer) pair with its citations. Threaded conversations can
--    be rebuilt later by adding a nullable parent_id, without a migration
--    on this one.
--
-- 2. cited_chunk_ids is UUID[] — citations are a short, bounded list (the
--    retriever's top-k), always read together with the answer. A join table
--    would be over-engineered here; the array keeps reads to one row. If we
--    later need "which answers cite chunk X?" we can add a GIN index on
--    the array.
--
-- 3. ON DELETE CASCADE from both users and documents — deleting a user or
--    their document removes their Q&A history for it. No cross-user leakage
--    through dangling rows.
--
-- 4. question and answer are TEXT (unbounded). Model answers are not a
--    fixed length and truncation would silently corrupt audit history.
--
-- 5. No UNIQUE constraint on (user_id, document_id, question) — users do
--    re-ask the same question, and we want each turn stored with its own
--    answer and timestamp.
-- =============================================================================

CREATE TABLE IF NOT EXISTS qa_sessions (
    id                UUID       PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID       NOT NULL REFERENCES users(id)     ON DELETE CASCADE,
    document_id       UUID       NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    question          TEXT       NOT NULL,
    answer            TEXT       NOT NULL,
    cited_chunk_ids   UUID[]     NOT NULL DEFAULT '{}',
    created_at        TIMESTAMP  NOT NULL DEFAULT NOW(),

    CONSTRAINT qa_sessions_question_not_blank CHECK (length(btrim(question)) > 0),
    CONSTRAINT qa_sessions_answer_not_blank   CHECK (length(btrim(answer))   > 0)
);

-- "Show me my Q&A history, newest first" — the main user-facing query.
CREATE INDEX IF NOT EXISTS ix_qa_sessions_user_created
    ON qa_sessions (user_id, created_at DESC);

-- "Show me all Q&A for this document" — used by the document detail page.
CREATE INDEX IF NOT EXISTS ix_qa_sessions_document_id
    ON qa_sessions (document_id);
