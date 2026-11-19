"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { useParams } from "next/navigation";
import { Header } from "@/components/layout/header";
import { JobStatusPoller } from "@/components/job/job-status-poller";
import { AnalysisView } from "@/components/analysis/analysis-view";
import { Button } from "@/components/ui/button";
import { ArrowLeft } from "lucide-react";
import Link from "next/link";

export default function DocumentPage() {
  const params = useParams();
  const searchParams = useSearchParams();
  const documentId = params.id as string;
  const jobId = searchParams.get("jobId");

  const [showAnalysis, setShowAnalysis] = useState(!jobId);

  return (
    <>
      <Header title="Document Analysis" />
      <div className="p-8 max-w-4xl mx-auto">
        <div className="mb-6">
          <Link href="/dashboard">
            <Button variant="ghost" size="sm">
              <ArrowLeft className="mr-1 h-4 w-4" />
              Back to Dashboard
            </Button>
          </Link>
        </div>

        {/* Show job poller if we have a jobId and analysis isn't ready yet */}
        {jobId && !showAnalysis && (
          <JobStatusPoller
            jobId={jobId}
            onComplete={() => setShowAnalysis(true)}
          />
        )}

        {/* Show analysis results */}
        {showAnalysis && <AnalysisView documentId={documentId} />}
      </div>
    </>
  );
}
