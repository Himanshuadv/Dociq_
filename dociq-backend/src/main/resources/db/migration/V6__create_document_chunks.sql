-- =============================================================================
-- V6__create_document_chunks.sql
-- =============================================================================
-- One row per text chunk produced from an uploaded document.
--
-- DEPENDENCIES:
--   V3__create_documents.sql  (documents.id)
--
-- KEY DESIGN DECISIONS:
--
-- 1. (document_id, chunk_index) is UNIQUE — reprocessing a document must
--    replace its old chunks rather than silently duplicating them. Enforcing
--    this at the DB level means the application layer cannot forget.
--
-- 2. ON DELETE CASCADE on document_id — when a user deletes a document, all
--    of its chunks go with it. No orphan rows, no cleanup job needed.
--
-- 3. embedding_id is TEXT and NULLABLE — it stores the Qdrant point id
--    (currently a UUID string, but Qdrant also accepts integer ids, so TEXT
--    keeps us flexible). NULL means "chunk exists but has not been embedded
--    yet", which is a legitimate intermediate state during processing.
--
-- 4. content is TEXT (unbounded). Chunk sizes are driven by the chunking
--    strategy, not the schema; VARCHAR(n) would just invite accidental
--    truncation on a strategy change.
--
-- 5. token_count stored alongside content so we can enforce per-request
--    context budgets without re-tokenising at query time.
-- =============================================================================

CREATE TABLE IF NOT EXISTS document_chunks (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id   UUID         NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index   INTEGER      NOT NULL,
    content       TEXT         NOT NULL,
    token_count   INTEGER      NOT NULL,
    embedding_id  TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT document_chunks_index_nonneg    CHECK (chunk_index >= 0),
    CONSTRAINT document_chunks_tokens_nonneg   CHECK (token_count >= 0),
    CONSTRAINT document_chunks_unique_in_doc   UNIQUE (document_id, chunk_index)
);

-- Lookups by document (list all chunks for a doc, ordered by position).
CREATE INDEX IF NOT EXISTS ix_document_chunks_document_id
    ON document_chunks (document_id, chunk_index);

-- Reverse lookup: Qdrant → Postgres row. Partial index skips un-embedded rows.
CREATE INDEX IF NOT EXISTS ix_document_chunks_embedding_id
    ON document_chunks (embedding_id)
    WHERE embedding_id IS NOT NULL;
