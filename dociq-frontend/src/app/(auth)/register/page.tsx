"use client";

import { AuthForm } from "@/components/auth/auth-form";
import { useRegister } from "@/hooks/use-auth";
import { AxiosError } from "axios";

export default function RegisterPage() {
  const register = useRegister();

  const errorMessage = register.error
    ? (register.error as AxiosError<{ message?: string }>).response?.data?.message ||
      "Registration failed. Please try again."
    : null;

  return (
    <AuthForm
      mode="register"
      onSubmit={(data) => register.mutate(data)}
      loading={register.isPending}
      error={errorMessage}
    />
  );
}
