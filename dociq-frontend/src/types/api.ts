// ─── Enums ───────────────────────────────────────────────
export type Profession = "STUDENT" | "TEACHER" | "LAWYER" | "PERSON";

export type JobStatus =
  | "PENDING"
  | "RUNNING"
  | "COMPLETED"
  | "FAILED";

// ─── Auth ────────────────────────────────────────────────
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  profession?: Profession;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface AuthResponse {
  id: string;
  email: string;
  profession: Profession;
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  message: string;
}

// ─── Documents ───────────────────────────────────────────
export interface DocumentResponse {
  documentId: string;
  userId: string;
  jobId: string;
  originalFilename: string;
  fileSizeBytes: number;
  status: string;
  mimeType: string;
  createdAt: string;
  updatedAt: string;
}

// Dummy DTO from GET /api/v1/document (backend returns hardcoded data)
export interface DocumentDto {
  id: string;
  title: string;
  content: string;
  status: string;
  createdAt: string;
}

// ─── Jobs ────────────────────────────────────────────────
export interface JobStatusResponse {
  jobId: string;
  documentId: string;
  status: JobStatus;
  attemptCount: number;
  maxAttempts: number;
  errorMessage: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
}

// ─── Analysis ────────────────────────────────────────────
export interface AnalysisResponse {
  id: string;
  documentId: string;
  summary: string;
  keyPoints: string[];
  riskFlags: string[];
  domain: string;
  confidenceScore: number;
  wordCount: number;
}

export interface SummaryResponse {
  summary: string;
}

export interface RiskFlagsResponse {
  riskFlags: string[];
  count: number;
  hasRisks: boolean;
}

export interface KeyPointsResponse {
  keyPoints: string[];
  count: number;
}

// ─── Error ───────────────────────────────────────────────
export interface ApiError {
  message: string;
  status: number;
  timestamp?: string;
}
