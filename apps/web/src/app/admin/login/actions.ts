"use server";

import { redirect } from "next/navigation";
import { setToken } from "@/lib/admin/session";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

// `email` is echoed back so the form keeps it after a failed attempt (React resets forms after actions).
export type LoginState = { error?: string; email?: string };

export async function login(_prev: LoginState, formData: FormData): Promise<LoginState> {
  const email = String(formData.get("email") ?? "").trim();
  const password = String(formData.get("password") ?? "");
  if (!email || !password) return { error: "Enter your email and password.", email };

  const res = await fetch(`${API_URL}/api/v1/auth/staff/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
    cache: "no-store",
  }).catch(() => null);

  if (!res) return { error: "The shop server is not reachable. Please try again.", email };
  if (res.status === 429) return { error: "Too many failed attempts. Try again in 15 minutes.", email };
  if (!res.ok) return { error: "Invalid email or password.", email };

  const { accessToken, expiresAt } = await res.json();
  await setToken(accessToken, expiresAt);
  redirect("/admin");
}
