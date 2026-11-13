"use server";

import { refresh } from "next/cache";
import { refreshStorefront } from "@/lib/admin/storefront-cache";
import { adminFetch, toActionError } from "@/lib/admin/api";
import type { ActionResult } from "@/lib/admin/types";

export async function moderateReview(id: number, approve: boolean): Promise<ActionResult> {
  try {
    await adminFetch(`/api/v1/admin/reviews/${id}/${approve ? "approve" : "reject"}`, { method: "POST" });
  } catch (e) {
    return toActionError(e);
  }
  refreshStorefront("reviews"); // product pages show the new review straight away
  refresh();
  return { ok: true };
}
