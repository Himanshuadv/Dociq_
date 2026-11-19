import { clsx } from "clsx";

type BadgeVariant = "gray" | "blue" | "green" | "yellow" | "red";

interface BadgeProps {
  variant?: BadgeVariant;
  children: React.ReactNode;
  className?: string;
}

const variantStyles: Record<BadgeVariant, string> = {
  gray: "bg-gray-100 text-gray-700",
  blue: "bg-blue-100 text-blue-700",
  green: "bg-green-100 text-green-700",
  yellow: "bg-yellow-100 text-yellow-700",
  red: "bg-red-100 text-red-700",
};

export function Badge({ variant = "gray", children, className }: BadgeProps) {
  return (
    <span
      className={clsx(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium",
        variantStyles[variant],
        className
      )}
    >
      {children}
    </span>
  );
}

export function statusBadgeVariant(status: string): BadgeVariant {
  switch (status) {
    case "COMPLETED":
      return "green";
    case "RUNNING":
      return "blue";
    case "PENDING":
      return "yellow";
    case "FAILED":
      return "red";
    default:
      return "gray";
  }
}
