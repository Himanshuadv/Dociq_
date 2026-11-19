import { useQuery, useMutation } from "@tanstack/react-query";
import api from "@/lib/api-client";
import { ENDPOINTS } from "@/lib/constants";
import type { DocumentDto, DocumentResponse } from "@/types/api";

export function useDocumentList() {
  return useQuery({
    queryKey: ["documents"],
    queryFn: async () => {
      const res = await api.get<DocumentDto[]>(ENDPOINTS.documents.list);
      return res.data;
    },
  });
}

export function useUploadDocument() {
  return useMutation({
    mutationFn: async ({ file, title }: { file: File; title: string }) => {
      const formData = new FormData();
      formData.append("file", file);
      formData.append("title", title);

      const res = await api.post<DocumentResponse>(
        ENDPOINTS.documents.upload,
        formData,
        { headers: { "Content-Type": "multipart/form-data" } }
      );
      return res.data;
    },
  });
}
