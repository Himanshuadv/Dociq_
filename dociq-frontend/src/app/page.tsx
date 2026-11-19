"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/stores/auth-store";
import { Spinner } from "@/components/ui/spinner";

export default function Home() {
  const router = useRouter();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  useEffect(() => {
    // Small delay to let hydration finish
    const timer = setTimeout(() => {
      router.replace(isAuthenticated ? "/dashboard" : "/login");
    }, 100);
    return () => clearTimeout(timer);
  }, [isAuthenticated, router]);

  return (
    <div className="flex h-screen items-center justify-center">
      <Spinner className="h-8 w-8 text-brand-600" />
    </div>
  );
}
