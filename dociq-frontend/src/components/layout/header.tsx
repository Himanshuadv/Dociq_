"use client";

import { useAuthStore } from "@/stores/auth-store";

export function Header({ title }: { title?: string }) {
  const user = useAuthStore((s) => s.user);

  return (
    <header className="flex h-16 items-center justify-between border-b border-gray-200 bg-white px-8">
      <h1 className="text-lg font-semibold text-gray-900">
        {title || "Dashboard"}
      </h1>
      {user && (
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-100 text-sm font-medium text-brand-700">
            {user.email[0].toUpperCase()}
          </div>
        </div>
      )}
    </header>
  );
}
