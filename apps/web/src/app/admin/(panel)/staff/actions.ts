"use server";

import { refresh } from "next/cache";
import { adminFetch, toActionError } from "@/lib/admin/api";
import type { ActionResult } from "@/lib/admin/types";

export async function createStaff(_prev: ActionResult | null, formData: FormData): Promise<ActionResult> {
  const payload = {
    email: String(formData.get("email") ?? "").trim(),
    fullName: String(formData.get("fullName") ?? "").trim(),
    role: String(formData.get("role") ?? ""),
    password: String(formData.get("password") ?? ""),
  };
  if (payload.password.length < 12) return { ok: false, message: "Use a temporary password of at least 12 characters." };
  try {
    await adminFetch("/api/v1/admin/staff", { method: "POST", body: JSON.stringify(payload) });
  } catch (e) {
    return toActionError(e);
  }
  refresh();
  return { ok: true, message: `Account created for ${payload.email}. Share the temporary password with them securely.` };
}

export async function updateStaff(_prev: ActionResult | null, formData: FormData): Promise<ActionResult> {
  const id = String(formData.get("id"));
  const payload = {
    fullName: String(formData.get("fullName") ?? "").trim(),
    role: String(formData.get("role") ?? ""),
    active: formData.get("active") === "on",
  };
  try {
    await adminFetch(`/api/v1/admin/staff/${id}`, { method: "PUT", body: JSON.stringify(payload) });
  } catch (e) {
    return toActionError(e);
  }
  refresh();
  return { ok: true, message: "Saved." };
}
