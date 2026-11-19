import { useQuery } from "@tanstack/react-query";
import api from "@/lib/api-client";
import { ENDPOINTS, JOB_POLL_INTERVAL_MS } from "@/lib/constants";
import type { JobStatusResponse } from "@/types/api";

export function useJobStatus(jobId: string | null) {
  return useQuery({
    queryKey: ["job-status", jobId],
    queryFn: async () => {
      if (!jobId) throw new Error("No job ID");
      const res = await api.get<JobStatusResponse>(ENDPOINTS.jobs.status(jobId));
      return res.data;
    },
    enabled: !!jobId,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status === "COMPLETED" || status === "FAILED") return false;
      return JOB_POLL_INTERVAL_MS;
    },
  });
}
