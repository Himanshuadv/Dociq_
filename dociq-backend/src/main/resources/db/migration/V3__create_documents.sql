CREATE TABLE documents (
                           id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           original_filename VARCHAR(500) NOT NULL,
                           stored_filename   VARCHAR(500) NOT NULL,
                           file_path         VARCHAR(1000) NOT NULL,
                           file_size_bytes   BIGINT NOT NULL,
                           mime_type         VARCHAR(100) NOT NULL,
                           page_count        INTEGER,
                           word_count        INTEGER,
                           status            VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
                           domain            VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
                           created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
                           updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
                           processed_at      TIMESTAMP
);