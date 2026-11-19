import Link from "next/link";
import { FileText } from "lucide-react";
import { Card, CardBody } from "@/components/ui/card";
import { Badge, statusBadgeVariant } from "@/components/ui/badge";
import type { DocumentDto } from "@/types/api";

export function DocumentCard({ doc }: { doc: DocumentDto }) {
  return (
    <Link href={`/documents/${doc.id}`}>
      <Card className="hover:shadow-md transition-shadow cursor-pointer">
        <CardBody>
          <div className="flex items-start gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-brand-50">
              <FileText className="h-5 w-5 text-brand-600" />
            </div>
            <div className="min-w-0 flex-1">
              <h3 className="text-sm font-medium text-gray-900 truncate">
                {doc.title}
              </h3>
              <p className="mt-1 text-xs text-gray-500 line-clamp-2">
                {doc.content}
              </p>
              <div className="mt-3 flex items-center gap-2">
                <Badge variant={statusBadgeVariant(doc.status)}>
                  {doc.status}
                </Badge>
                <span className="text-xs text-gray-400">
                  {new Date(doc.createdAt).toLocaleDateString()}
                </span>
              </div>
            </div>
          </div>
        </CardBody>
      </Card>
    </Link>
  );
}
