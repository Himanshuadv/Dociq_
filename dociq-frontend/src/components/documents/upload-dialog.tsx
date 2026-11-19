"use client";

import { useState, useCallback } from "react";
import { useDropzone } from "react-dropzone";
import { Upload, X, FileText } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useUploadDocument } from "@/hooks/use-documents";
import { useRouter } from "next/navigation";

interface UploadDialogProps {
  open: boolean;
  onClose: () => void;
}

export function UploadDialog({ open, onClose }: UploadDialogProps) {
  const [file, setFile] = useState<File | null>(null);
  const [title, setTitle] = useState("");
  const router = useRouter();

  const upload = useUploadDocument();

  const onDrop = useCallback((accepted: File[]) => {
    if (accepted.length > 0) {
      const f = accepted[0];
      setFile(f);
      if (!title) {
        setTitle(f.name.replace(/\.[^/.]+$/, ""));
      }
    }
  }, [title]);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      "application/pdf": [".pdf"],
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document": [".docx"],
    },
    maxFiles: 1,
    maxSize: 10 * 1024 * 1024, // 10MB — matches backend
  });

  const handleSubmit = () => {
    if (!file || !title.trim()) return;

    upload.mutate(
      { file, title: title.trim() },
      {
        onSuccess: (data) => {
          onClose();
          setFile(null);
          setTitle("");
          // Navigate to the document page where we'll poll the job
          router.push(`/documents/${data.documentId}?jobId=${data.jobId}`);
        },
      }
    );
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />

      {/* Modal */}
      <div className="relative w-full max-w-lg rounded-xl bg-white p-6 shadow-xl">
        <button
          onClick={onClose}
          className="absolute right-4 top-4 text-gray-400 hover:text-gray-600"
        >
          <X className="h-5 w-5" />
        </button>

        <h2 className="text-lg font-semibold text-gray-900 mb-4">
          Upload Document
        </h2>

        {/* Dropzone */}
        <div
          {...getRootProps()}
          className={`flex flex-col items-center justify-center rounded-lg border-2 border-dashed p-8 transition-colors cursor-pointer ${
            isDragActive
              ? "border-brand-400 bg-brand-50"
              : "border-gray-300 hover:border-gray-400"
          }`}
        >
          <input {...getInputProps()} />
          {file ? (
            <div className="flex items-center gap-3">
              <FileText className="h-8 w-8 text-brand-600" />
              <div>
                <p className="text-sm font-medium text-gray-900">{file.name}</p>
                <p className="text-xs text-gray-500">
                  {(file.size / 1024 / 1024).toFixed(2)} MB
                </p>
              </div>
            </div>
          ) : (
            <>
              <Upload className="h-10 w-10 text-gray-400 mb-3" />
              <p className="text-sm text-gray-600">
                Drop a PDF or DOCX here, or click to browse
              </p>
              <p className="text-xs text-gray-400 mt-1">Max 10 MB</p>
            </>
          )}
        </div>

        {/* Title */}
        <div className="mt-4">
          <Input
            id="doc-title"
            label="Document Title"
            placeholder="e.g. Q1 Revenue Report"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />
        </div>

        {/* Error */}
        {upload.error && (
          <p className="mt-3 text-sm text-red-600">
            Upload failed. Please try again.
          </p>
        )}

        {/* Actions */}
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            onClick={handleSubmit}
            loading={upload.isPending}
            disabled={!file || !title.trim()}
          >
            Upload & Analyze
          </Button>
        </div>
      </div>
    </div>
  );
}
