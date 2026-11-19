"use client";

import { FormEvent, useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { Profession } from "@/types/api";

interface AuthFormProps {
  mode: "login" | "register";
  onSubmit: (data: {
    email: string;
    password: string;
    profession?: Profession;
  }) => void;
  loading?: boolean;
  error?: string | null;
}

const PROFESSIONS: { value: Profession; label: string }[] = [
  { value: "PERSON", label: "General" },
  { value: "STUDENT", label: "Student" },
  { value: "TEACHER", label: "Teacher" },
  { value: "LAWYER", label: "Lawyer" },
];

export function AuthForm({ mode, onSubmit, loading, error }: AuthFormProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [profession, setProfession] = useState<Profession>("PERSON");

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    onSubmit({
      email,
      password,
      ...(mode === "register" ? { profession } : {}),
    });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <div className="text-center mb-8">
        <h1 className="text-2xl font-bold text-gray-900">
          {mode === "login" ? "Welcome back" : "Create your account"}
        </h1>
        <p className="mt-1 text-sm text-gray-500">
          {mode === "login"
            ? "Sign in to access your documents"
            : "Start analyzing documents with DocIQ"}
        </p>
      </div>

      {error && (
        <div className="rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <Input
        id="email"
        label="Email"
        type="email"
        placeholder="you@example.com"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        required
      />

      <Input
        id="password"
        label="Password"
        type="password"
        placeholder="Minimum 8 characters"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        required
        minLength={8}
      />

      {mode === "register" && (
        <div className="space-y-1">
          <label htmlFor="profession" className="block text-sm font-medium text-gray-700">
            Profession
          </label>
          <select
            id="profession"
            value={profession}
            onChange={(e) => setProfession(e.target.value as Profession)}
            className="block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-200"
          >
            {PROFESSIONS.map((p) => (
              <option key={p.value} value={p.value}>
                {p.label}
              </option>
            ))}
          </select>
        </div>
      )}

      <Button type="submit" loading={loading} className="w-full" size="lg">
        {mode === "login" ? "Sign in" : "Create account"}
      </Button>

      <p className="text-center text-sm text-gray-500">
        {mode === "login" ? (
          <>
            Don&apos;t have an account?{" "}
            <Link href="/register" className="text-brand-600 hover:text-brand-700 font-medium">
              Sign up
            </Link>
          </>
        ) : (
          <>
            Already have an account?{" "}
            <Link href="/login" className="text-brand-600 hover:text-brand-700 font-medium">
              Sign in
            </Link>
          </>
        )}
      </p>
    </form>
  );
}
