"use server";

import QRCode from "qrcode";
import { refresh } from "next/cache";
import { adminFetch, toActionError } from "@/lib/admin/api";
import { setToken } from "@/lib/admin/session";
import type { ActionResult } from "@/lib/admin/types";

export type MfaSetupState = ActionResult & { secret?: string; qrSvg?: string };

/** Creates a new authenticator secret and returns it as a QR code to scan. */
export async function startMfaSetup(): Promise<MfaSetupState> {
  try {
    const { secret, otpauthUri } = await adminFetch<{ secret: string; otpauthUri: string }>(
      "/api/v1/admin/me/mfa/setup",
      { method: "POST" },
    );
    const qrSvg = await QRCode.toString(otpauthUri, { type: "svg", margin: 1, width: 200 });
    return { ok: true, secret, qrSvg };
  } catch (e) {
    return toActionError(e);
  }
}

export async function enableMfa(prev: MfaSetupState, formData: FormData): Promise<MfaSetupState> {
  const code = String(formData.get("code") ?? "").replace(/\s/g, "");
  if (!/^\d{6}$/.test(code)) return { ...prev, ok: false, message: "Enter the 6-digit code from the app." };
  try {
    // The API returns a fresh session that counts as two-step, so keep using it from now on.
    const session = await adminFetch<{ accessToken: string; expiresAt: string }>("/api/v1/admin/me/mfa/enable", {
      method: "POST",
      body: JSON.stringify({ code }),
    });
    await setToken(session.accessToken, session.expiresAt);
  } catch (e) {
    return { ...prev, ...toActionError(e) };
  }
  refresh();
  return { ok: true, message: "Two-step sign-in is on. You'll be asked for a code each time you sign in." };
}

export async function disableMfa(_prev: ActionResult | null, formData: FormData): Promise<ActionResult> {
  const code = String(formData.get("code") ?? "").replace(/\s/g, "");
  try {
    await adminFetch("/api/v1/admin/me/mfa/disable", { method: "POST", body: JSON.stringify({ code }) });
  } catch (e) {
    return toActionError(e);
  }
  refresh();
  return { ok: true, message: "Two-step sign-in is off." };
}

export async function resetStaffMfa(staffId: number): Promise<ActionResult> {
  try {
    await adminFetch(`/api/v1/admin/staff/${staffId}/mfa/reset`, { method: "POST" });
  } catch (e) {
    return toActionError(e);
  }
  refresh();
  return { ok: true, message: "Two-step sign-in reset. They'll set it up again next time." };
}
