"use client";

import { useState } from "react";
import { Header } from "@/components/layout/header";
import { DocumentList } from "@/components/documents/document-list";
import { UploadDialog } from "@/components/documents/upload-dialog";
import { Button } from "@/components/ui/button";
import { Upload } from "lucide-react";

export default function DashboardPage() {
  const [showUpload, setShowUpload] = useState(false);

  return (
    <>
      <Header title="Documents" />
      <div className="p-8">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h2 className="text-xl font-semibold text-gray-900">
              Your Documents
            </h2>
            <p className="mt-1 text-sm text-gray-500">
              Upload and analyze PDFs and DOCX files.
            </p>
          </div>
          <Button onClick={() => setShowUpload(true)}>
            <Upload className="mr-2 h-4 w-4" />
            Upload
          </Button>
        </div>

        <DocumentList />
      </div>

      <UploadDialog
        open={showUpload}
        onClose={() => setShowUpload(false)}
      />
    </>
  );
}
