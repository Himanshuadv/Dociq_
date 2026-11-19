"use client";

import { useAnalysis } from "@/hooks/use-analysis";
import { SummarySection } from "./summary-section";
import { KeyPointsSection } from "./key-points-section";
import { RiskFlagsSection } from "./risk-flags-section";
import { Spinner } from "@/components/ui/spinner";
import { Badge } from "@/components/ui/badge";

export function AnalysisView({ documentId }: { documentId: string }) {
  const { data: analysis, isLoading, error } = useAnalysis(documentId);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Spinner className="h-8 w-8 text-brand-600" />
        <p className="ml-3 text-sm text-gray-500">Loading analysis...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg bg-red-50 border border-red-200 px-4 py-8 text-center">
        <p className="text-sm text-red-700">
          Failed to load analysis. The document may still be processing.
        </p>
      </div>
    );
  }

  if (!analysis) return null;

  return (
    <div className="space-y-6">
      {/* Meta bar */}
      <div className="flex items-center gap-3 text-sm text-gray-500">
        {analysis.domain && (
          <Badge variant="blue">{analysis.domain}</Badge>
        )}
        {analysis.wordCount > 0 && (
          <span>{analysis.wordCount.toLocaleString()} words</span>
        )}
        {analysis.confidenceScore > 0 && (
          <span>Confidence: {(analysis.confidenceScore * 100).toFixed(1)}%</span>
        )}
      </div>

      <SummarySection summary={analysis.summary} />
      <KeyPointsSection keyPoints={analysis.keyPoints} />
      <RiskFlagsSection riskFlags={analysis.riskFlags} />
    </div>
  );
}
