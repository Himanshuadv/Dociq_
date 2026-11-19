"use client";

import { AuthForm } from "@/components/auth/auth-form";
import { useLogin } from "@/hooks/use-auth";
import { AxiosError } from "axios";

export default function LoginPage() {
  const login = useLogin();

  const errorMessage = login.error
    ? (login.error as AxiosError<{ message?: string }>).response?.data?.message ||
      "Login failed. Please check your credentials."
    : null;

  return (
    <AuthForm
      mode="login"
      onSubmit={(data) => login.mutate(data)}
      loading={login.isPending}
      error={errorMessage}
    />
  );
}
