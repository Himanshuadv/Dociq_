CREATE TABLE refresh_tokens (
                                id UUID PRIMARY KEY, -- FIXED: Changed type to UUID to match your Java entity
                                token_hash VARCHAR(64) NOT NULL UNIQUE,
                                user_id UUID NOT NULL,
                                device_info VARCHAR(255),
                                created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);