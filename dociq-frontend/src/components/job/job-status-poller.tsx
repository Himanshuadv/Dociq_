"use client";

import { useJobStatus } from "@/hooks/use-job-status";
import { Card, CardBody } from "@/components/ui/card";
import { Badge, statusBadgeVariant } from "@/components/ui/badge";
import { Spinner } from "@/components/ui/spinner";
import { AlertCircle, CheckCircle2, Clock } from "lucide-react";

interface JobStatusPollerProps {
  jobId: string;
  onComplete: (documentId: string) => void;
}

export function JobStatusPoller({ jobId, onComplete }: JobStatusPollerProps) {
  const { data: job, isLoading } = useJobStatus(jobId);

  // Notify parent when job completes
  if (job?.status === "COMPLETED") {
    // Use setTimeout to avoid calling onComplete during render
    setTimeout(() => onComplete(job.documentId), 0);
  }

  if (isLoading || !job) {
    return (
      <Card>
        <CardBody className="flex items-center justify-center py-12">
          <Spinner className="h-8 w-8 text-brand-600" />
          <p className="ml-3 text-sm text-gray-500">Checking job status...</p>
        </CardBody>
      </Card>
    );
  }

  return (
    <Card>
      <CardBody className="space-y-4">
        <div className="flex items-center gap-3">
          {job.status === "RUNNING" && (
            <Spinner className="h-6 w-6 text-brand-600" />
          )}
          {job.status === "PENDING" && (
            <Clock className="h-6 w-6 text-yellow-500" />
          )}
          {job.status === "COMPLETED" && (
            <CheckCircle2 className="h-6 w-6 text-green-500" />
          )}
          {job.status === "FAILED" && (
            <AlertCircle className="h-6 w-6 text-red-500" />
          )}

          <div>
            <h3 className="text-sm font-medium text-gray-900">
              {job.status === "RUNNING" && "Analyzing your document..."}
              {job.status === "PENDING" && "Waiting in queue..."}
              {job.status === "COMPLETED" && "Analysis complete"}
              {job.status === "FAILED" && "Analysis failed"}
            </h3>
            <p className="text-xs text-gray-500">
              Attempt {job.attemptCount} of {job.maxAttempts}
            </p>
          </div>

          <Badge
            variant={statusBadgeVariant(job.status)}
            className="ml-auto"
          >
            {job.status}
          </Badge>
        </div>

        {job.status === "FAILED" && job.errorMessage && (
          <div className="rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
            {job.errorMessage}
          </div>
        )}

        {(job.status === "RUNNING" || job.status === "PENDING") && (
          <div className="w-full bg-gray-200 rounded-full h-1.5">
            <div
              className="bg-brand-600 h-1.5 rounded-full transition-all duration-500 animate-pulse"
              style={{
                width: job.status === "RUNNING" ? "60%" : "20%",
              }}
            />
          </div>
        )}
      </CardBody>
    </Card>
  );
}
