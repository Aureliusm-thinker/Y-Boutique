"use server";

import { redirect } from "next/navigation";
import { setToken } from "@/lib/admin/session";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

// `email` is echoed back so the form keeps it after a failed attempt (React resets forms after actions).
// `ticket` is set after a correct password when the account uses two-step sign-in: the form then asks for a code.
export type LoginState = { error?: string; email?: string; ticket?: string };

type LoginResponse = { accessToken?: string; expiresAt?: string; mfaRequired: boolean; mfaTicket?: string };

async function post(path: string, body: unknown): Promise<Response | null> {
  return fetch(`${API_URL}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
    cache: "no-store",
  }).catch(() => null);
}

export async function login(_prev: LoginState, formData: FormData): Promise<LoginState> {
  const ticket = String(formData.get("ticket") ?? "");
  const email = String(formData.get("email") ?? "").trim();

  // Step 2: authenticator code.
  if (ticket) {
    const code = String(formData.get("code") ?? "").replace(/\s/g, "");
    if (!/^\d{6}$/.test(code)) return { error: "Enter the 6-digit code from your authenticator app.", email, ticket };
    const res = await post("/api/v1/auth/staff/login/mfa", { mfaTicket: ticket, code });
    if (!res) return { error: "The shop server is not reachable. Please try again.", email, ticket };
    if (!res.ok) {
      const problem = await res.json().catch(() => ({}));
      const expired = String(problem.detail ?? "").includes("expired");
      return expired
        ? { error: "Your sign-in expired. Please enter your password again.", email }
        : { error: "That code didn't work. Check your authenticator app and try again.", email, ticket };
    }
    const { accessToken, expiresAt } = (await res.json()) as LoginResponse;
    await setToken(accessToken!, expiresAt!);
    redirect("/admin");
  }

  // Step 1: email + password.
  const password = String(formData.get("password") ?? "");
  if (!email || !password) return { error: "Enter your email and password.", email };
  const res = await post("/api/v1/auth/staff/login", { email, password });
  if (!res) return { error: "The shop server is not reachable. Please try again.", email };
  if (res.status === 429) return { error: "Too many failed attempts. Try again in 15 minutes.", email };
  if (!res.ok) return { error: "Invalid email or password.", email };

  const body = (await res.json()) as LoginResponse;
  if (body.mfaRequired) return { email, ticket: body.mfaTicket };
  await setToken(body.accessToken!, body.expiresAt!);
  redirect("/admin");
}
