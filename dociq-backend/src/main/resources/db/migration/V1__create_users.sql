CREATE TABLE users (
                       id UUID NOT NULL PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       active BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                       updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                       profession VARCHAR(50) NOT NULL DEFAULT 'PERSON'
);