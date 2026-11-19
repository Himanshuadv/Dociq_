import { Card, CardHeader, CardBody } from "@/components/ui/card";
import { Lightbulb } from "lucide-react";

export function KeyPointsSection({ keyPoints }: { keyPoints: string[] }) {
  if (!keyPoints || keyPoints.length === 0) {
    return null;
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <Lightbulb className="h-5 w-5 text-yellow-500" />
          <h2 className="text-base font-semibold text-gray-900">
            Key Points
          </h2>
          <span className="ml-auto text-xs text-gray-400">
            {keyPoints.length} found
          </span>
        </div>
      </CardHeader>
      <CardBody>
        <ul className="space-y-3">
          {keyPoints.map((point, i) => (
            <li key={i} className="flex gap-3 text-sm text-gray-700">
              <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-yellow-50 text-xs font-medium text-yellow-700">
                {i + 1}
              </span>
              <span className="leading-relaxed">{point}</span>
            </li>
          ))}
        </ul>
      </CardBody>
    </Card>
  );
}
