"use server";

import { refresh } from "next/cache";
import { adminFetch, toActionError } from "@/lib/admin/api";
import type { ActionResult } from "@/lib/admin/types";

/** Datetime-local inputs are Nairobi time (UTC+3, no daylight saving). */
const toInstant = (v: FormDataEntryValue | null) => {
  const s = String(v ?? "").trim();
  return s ? new Date(`${s}:00+03:00`).toISOString() : null;
};
const num = (v: FormDataEntryValue | null) => (String(v ?? "").trim() === "" ? null : Number(v));

export async function savePromotion(id: number | null, _prev: ActionResult | null, fd: FormData): Promise<ActionResult> {
  const body = {
    code: String(fd.get("code") ?? "").trim() || null,
    name: String(fd.get("name") ?? "").trim(),
    type: String(fd.get("type")),
    value: num(fd.get("value")),
    minSubtotal: num(fd.get("minSubtotal")),
    startsAt: toInstant(fd.get("startsAt")),
    endsAt: toInstant(fd.get("endsAt")),
    usageLimit: num(fd.get("usageLimit")),
    active: fd.get("active") === "on",
  };
  try {
    await adminFetch(id ? `/api/v1/admin/promotions/${id}` : "/api/v1/admin/promotions", {
      method: id ? "PUT" : "POST",
      body: JSON.stringify(body),
    });
  } catch (e) {
    return toActionError(e);
  }
  refresh();
  return { ok: true, message: id ? "Saved." : "Created." };
}
