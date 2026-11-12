import "server-only";
import { redirect } from "next/navigation";
import { getToken } from "./session";
import type { ActionResult, Staff } from "./types";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public errors: string[] = [],
  ) {
    super(message);
  }
}

/**
 * Authenticated call to the Java admin API. Never cached: admin screens always show live data.
 * A missing or expired token sends the user to the login page.
 */
export async function adminFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = await getToken();
  if (!token) redirect("/admin/login");

  const headers = new Headers(init.headers);
  headers.set("Authorization", `Bearer ${token}`);
  headers.set("Accept", "application/json");
  if (init.body && !(init.body instanceof FormData)) headers.set("Content-Type", "application/json");

  const res = await fetch(`${API_URL}${path}`, { ...init, headers, cache: "no-store" });
  if (res.status === 401) redirect("/admin/login");
  if (!res.ok) {
    const problem = await res.json().catch(() => ({}));
    const message =
      res.status === 403 ? "You don't have permission to do that." : (problem.detail ?? `Request failed (${res.status})`);
    throw new ApiError(res.status, message, problem.errors ?? []);
  }
  return (res.status === 204 ? undefined : await res.json()) as T;
}

export async function getCurrentStaff(): Promise<Staff> {
  return adminFetch<Staff>("/api/v1/admin/me");
}

/** Turns an API error into a form-friendly result; rethrows anything else (incl. redirects). */
export function toActionError(e: unknown): ActionResult {
  if (e instanceof ApiError) return { ok: false, message: e.message, errors: e.errors };
  throw e;
}
