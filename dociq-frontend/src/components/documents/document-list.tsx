"use client";

import { useDocumentList } from "@/hooks/use-documents";
import { DocumentCard } from "./document-card";
import { Spinner } from "@/components/ui/spinner";

export function DocumentList() {
  const { data: documents, isLoading, error } = useDocumentList();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Spinner className="h-8 w-8 text-brand-600" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg bg-red-50 border border-red-200 px-4 py-8 text-center">
        <p className="text-sm text-red-700">Failed to load documents.</p>
      </div>
    );
  }

  if (!documents || documents.length === 0) {
    return (
      <div className="rounded-lg border-2 border-dashed border-gray-200 px-4 py-16 text-center">
        <p className="text-sm text-gray-500">
          No documents yet. Upload one to get started.
        </p>
      </div>
    );
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {documents.map((doc) => (
        <DocumentCard key={doc.id} doc={doc} />
      ))}
    </div>
  );
}
