import { Card, CardHeader, CardBody } from "@/components/ui/card";
import { ShieldAlert } from "lucide-react";
import { Badge } from "@/components/ui/badge";

export function RiskFlagsSection({ riskFlags }: { riskFlags: string[] }) {
  if (!riskFlags || riskFlags.length === 0) {
    return (
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <ShieldAlert className="h-5 w-5 text-green-500" />
            <h2 className="text-base font-semibold text-gray-900">
              Risk Flags
            </h2>
          </div>
        </CardHeader>
        <CardBody>
          <p className="text-sm text-green-700">
            No risk flags detected in this document.
          </p>
        </CardBody>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <ShieldAlert className="h-5 w-5 text-red-500" />
          <h2 className="text-base font-semibold text-gray-900">
            Risk Flags
          </h2>
          <Badge variant="red" className="ml-auto">
            {riskFlags.length} found
          </Badge>
        </div>
      </CardHeader>
      <CardBody>
        <ul className="space-y-2">
          {riskFlags.map((flag, i) => (
            <li
              key={i}
              className="flex items-start gap-2 rounded-lg bg-red-50 border border-red-100 px-3 py-2 text-sm text-red-800"
            >
              <ShieldAlert className="h-4 w-4 mt-0.5 shrink-0 text-red-400" />
              <span>{flag}</span>
            </li>
          ))}
        </ul>
      </CardBody>
    </Card>
  );
}
