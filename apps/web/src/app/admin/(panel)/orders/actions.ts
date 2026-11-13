"use server";

import { refresh, updateTag } from "next/cache";
import { adminFetch, toActionError } from "@/lib/admin/api";
import type { ActionResult } from "@/lib/admin/types";

const s = (fd: FormData, k: string) => String(fd.get(k) ?? "").trim();

async function run(fn: () => Promise<unknown>, ok: string, restocks = false): Promise<ActionResult> {
  try {
    const res = (await fn()) as { message?: string } | undefined;
    // Restocking changes what the shop shows as available.
    if (restocks) updateTag("products");
    refresh();
    return { ok: true, message: res?.message ?? ok };
  } catch (e) {
    return toActionError(e);
  }
}

export async function shipOrder(id: number, _prev: ActionResult | null, fd: FormData): Promise<ActionResult> {
  return run(
    () =>
      adminFetch(`/api/v1/admin/orders/${id}/ship`, {
        method: "POST",
        body: JSON.stringify({ carrier: s(fd, "carrier") || null, trackingNumber: s(fd, "trackingNumber") || null, trackingUrl: s(fd, "trackingUrl") || null }),
      }),
    "Marked as shipped. The customer has been emailed.",
  );
}

export async function deliverOrder(id: number): Promise<ActionResult> {
  return run(() => adminFetch(`/api/v1/admin/orders/${id}/deliver`, { method: "POST" }), "Marked as delivered.");
}

export async function addOrderNote(id: number, _prev: ActionResult | null, fd: FormData): Promise<ActionResult> {
  return run(
    () => adminFetch(`/api/v1/admin/orders/${id}/notes`, { method: "POST", body: JSON.stringify({ note: s(fd, "note") }) }),
    "Note added.",
  );
}

export async function refundOrder(id: number, _prev: ActionResult | null, fd: FormData): Promise<ActionResult> {
  const restock = [...fd.entries()]
    .filter(([k, v]) => k.startsWith("restock-") && Number(v) > 0)
    .map(([k, v]) => ({ lineId: Number(k.slice("restock-".length)), quantity: Number(v) }));
  return run(
    () =>
      adminFetch(`/api/v1/admin/orders/${id}/refund`, {
        method: "POST",
        body: JSON.stringify({ amount: Number(s(fd, "amount")), reason: s(fd, "reason"), restock }),
      }),
    "Refund issued.",
    restock.length > 0,
  );
}

export async function cancelOrder(id: number, _prev: ActionResult | null, fd: FormData): Promise<ActionResult> {
  return run(
    () => adminFetch(`/api/v1/admin/orders/${id}/cancel`, { method: "POST", body: JSON.stringify({ reason: s(fd, "reason") }) }),
    "Order cancelled.",
    true,
  );
}

export async function addCustomerNote(id: number, _prev: ActionResult | null, fd: FormData): Promise<ActionResult> {
  return run(
    () => adminFetch(`/api/v1/admin/customers/${id}/notes`, { method: "POST", body: JSON.stringify({ note: s(fd, "note") }) }),
    "Note added.",
  );
}
