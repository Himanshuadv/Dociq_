import { Card, CardHeader, CardBody } from "@/components/ui/card";
import { FileText } from "lucide-react";

export function SummarySection({ summary }: { summary: string }) {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <FileText className="h-5 w-5 text-brand-600" />
          <h2 className="text-base font-semibold text-gray-900">Summary</h2>
        </div>
      </CardHeader>
      <CardBody>
        <p className="text-sm leading-relaxed text-gray-700 whitespace-pre-wrap">
          {summary}
        </p>
      </CardBody>
    </Card>
  );
}
