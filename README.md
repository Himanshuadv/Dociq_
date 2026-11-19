# DocIQ

Document intelligence platform — upload a document, let AI analyze it, ask questions about it.

## Monorepo layout

```
dociq/
├── dociq-backend/    Spring Boot 3.5 · Java 21 · Postgres 16 · Qdrant
├── dociq-frontend/   Next.js · TypeScript · Tailwind
├── docs/             Architecture notes and design docs
├── compose.yaml      (optional root compose, see below)
└── README.md
```

## Quick start

### 1. Infrastructure (Postgres + Qdrant)

```bash
cd dociq-backend
cp .env.example .env            # edit POSTGRES_PASSWORD
docker compose up -d
```

### 2. Backend

```bash
cd dociq-backend
./mvnw spring-boot:run
```

Backend starts on http://localhost:8080 — Swagger UI at `/swagger-ui.html`.

### 3. Frontend

```bash
cd dociq-frontend
npm install
npm run dev
```

Frontend starts on http://localhost:3000.

## Tech stack

**Backend** — Spring Boot 3.5.14, Spring Security + JWT, Spring Data JPA, Flyway, PostgreSQL 16, Qdrant (vector store), Apache Tika (text extraction), LangChain4J (AI orchestration).

**Frontend** — Next.js, TypeScript, Tailwind CSS.

## Development

- Backend conventions, API design, and domain layering are documented under `docs/`.
- Database migrations live in `dociq-backend/src/main/resources/db/migration/` and are applied automatically by Flyway on startup.
- The `.env` file is per-developer and never committed — `.env.example` is the shared template.
