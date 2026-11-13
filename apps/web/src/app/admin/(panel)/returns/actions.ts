"use server";

import { refresh } from "next/cache";
import { refreshStorefront } from "@/lib/admin/storefront-cache";
import { adminFetch, toActionError } from "@/lib/admin/api";
import type { ActionResult } from "@/lib/admin/types";

async function step(id: number, path: string, body: unknown, ok: string, stock = false): Promise<ActionResult> {
  try {
    await adminFetch(`/api/v1/admin/returns/${id}/${path}`, { method: "POST", body: JSON.stringify(body) });
  } catch (e) {
    return toActionError(e);
  }
  if (stock) refreshStorefront("products");
  refresh();
  return { ok: true, message: ok };
}

export async function approveReturn(id: number, _p: ActionResult | null, fd: FormData) {
  return step(id, "approve", { comment: String(fd.get("comment") ?? "").trim() || null }, "Approved. The customer has been emailed.");
}

export async function rejectReturn(id: number, _p: ActionResult | null, fd: FormData) {
  return step(id, "reject", { comment: String(fd.get("comment") ?? "").trim() }, "Rejected. The customer has been emailed.");
}

export async function receiveReturn(id: number, _p: ActionResult | null, fd: FormData) {
  const restockLineIds = fd.getAll("restock").map(Number);
  return step(id, "receive", { restockLineIds }, "Marked as received.", restockLineIds.length > 0);
}

export async function refundReturn(id: number, _p: ActionResult | null, fd: FormData) {
  const amount = Number(fd.get("amount"));
  return step(id, "refund", { amount: amount > 0 ? amount : null }, "Refund issued.");
}
