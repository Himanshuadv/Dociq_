import { useQuery } from "@tanstack/react-query";
import api from "@/lib/api-client";
import { ENDPOINTS } from "@/lib/constants";
import type { AnalysisResponse } from "@/types/api";

export function useAnalysis(documentId: string | null) {
  return useQuery({
    queryKey: ["analysis", documentId],
    queryFn: async () => {
      if (!documentId) throw new Error("No document ID");
      const res = await api.get<AnalysisResponse>(
        ENDPOINTS.analysis.full(documentId)
      );
      return res.data;
    },
    enabled: !!documentId,
    retry: 1,
  });
}
