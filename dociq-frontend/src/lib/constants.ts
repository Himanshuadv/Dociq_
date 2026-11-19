// All API calls go through Next.js rewrites → localhost:8080
export const API_BASE = "/api/v1";

export const ENDPOINTS = {
  auth: {
    login: `${API_BASE}/auth/login`,
    register: `${API_BASE}/auth/register`,
    refresh: `${API_BASE}/auth/refresh`,
    logout: `${API_BASE}/auth/logout`,
  },
  documents: {
    list: `${API_BASE}/document`,
    upload: `${API_BASE}/document/uploads`,
    byId: (id: string) => `${API_BASE}/document/${id}`,
  },
  jobs: {
    status: (jobId: string) => `${API_BASE}/jobs/${jobId}/status`,
  },
  analysis: {
    full: (docId: string) => `${API_BASE}/documents/${docId}/analysis`,
    summary: (docId: string) => `${API_BASE}/documents/${docId}/analysis/summary`,
    riskFlags: (docId: string) => `${API_BASE}/documents/${docId}/analysis/risk-flags`,
    keyPoints: (docId: string) => `${API_BASE}/documents/${docId}/analysis/key-points`,
  },
} as const;

export const JOB_POLL_INTERVAL_MS = 3000;
