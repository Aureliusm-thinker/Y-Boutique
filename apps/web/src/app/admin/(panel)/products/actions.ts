"use server";

// Every action re-checks authorisation by calling the Java API with the caller's own token:
// the API enforces the role permissions (docs/03 §4), so these can't be bypassed by posting directly.

import { refresh } from "next/cache";
import { refreshStorefront } from "@/lib/admin/storefront-cache";
import { redirect } from "next/navigation";
import { adminFetch, toActionError } from "@/lib/admin/api";
import type { ActionResult, AdjustmentReason, AdminProductDetail, ProductUpsert } from "@/lib/admin/types";

export async function saveProduct(
  id: number | null,
  payload: ProductUpsert,
): Promise<ActionResult & { product?: AdminProductDetail }> {
  let saved: AdminProductDetail;
  try {
    saved = await adminFetch<AdminProductDetail>(id ? `/api/v1/admin/products/${id}` : "/api/v1/admin/products", {
      method: id ? "PUT" : "POST",
      body: JSON.stringify(payload),
    });
  } catch (e) {
    return toActionError(e);
  }
  refreshStorefront("products");
  if (!id) redirect(`/admin/products/${saved.id}?created=1`);
  refresh();
  return { ok: true, message: "Saved.", product: saved };
}

export async function changeStatus(id: number, action: "publish" | "unpublish" | "archive"): Promise<ActionResult> {
  try {
    await adminFetch(`/api/v1/admin/products/${id}/${action}`, { method: "POST" });
  } catch (e) {
    return toActionError(e);
  }
  refreshStorefront("products");
  refresh();
  const messages = { publish: "Product is live.", unpublish: "Moved back to draft.", archive: "Product archived." };
  return { ok: true, message: messages[action] };
}

export async function uploadImage(formData: FormData): Promise<ActionResult & { url?: string }> {
  const file = formData.get("file");
  if (!(file instanceof File) || file.size === 0) return { ok: false, message: "Choose an image first." };
  if (file.size > 10 * 1024 * 1024) return { ok: false, message: "Images must be 10 MB or smaller." };
  const body = new FormData();
  body.append("file", file);
  try {
    const { url } = await adminFetch<{ url: string }>("/api/v1/admin/media", { method: "POST", body });
    return { ok: true, url };
  } catch (e) {
    return toActionError(e);
  }
}

export async function adjustStock(
  variantId: number,
  delta: number,
  reason: AdjustmentReason,
  note: string,
): Promise<ActionResult> {
  if (!Number.isInteger(delta) || delta === 0) return { ok: false, message: "Enter a whole number other than 0." };
  try {
    await adminFetch("/api/v1/admin/inventory/adjustments", {
      method: "POST",
      body: JSON.stringify({ variantId, delta, reason, note: note.trim() || null }),
    });
  } catch (e) {
    return toActionError(e);
  }
  refreshStorefront("products"); // in-stock badges on the shop
  refresh();
  return { ok: true, message: "Stock updated." };
}

/** Sets the curated "complete the look" products for a product page. */
export async function setRelatedProducts(id: number, productIds: number[]): Promise<ActionResult> {
  try {
    await adminFetch(`/api/v1/admin/products/${id}/related`, {
      method: "PUT",
      body: JSON.stringify({ productIds }),
    });
  } catch (e) {
    return toActionError(e);
  }
  refreshStorefront("products");
  refresh();
  return { ok: true, message: "Saved." };
}

/** Product search for pickers in the back office. */
export async function findProducts(q: string): Promise<{ id: number; title: string; status: string }[]> {
  const qs = new URLSearchParams({ q, size: "8" });
  const page = await adminFetch<{ items: { id: number; title: string; status: string }[] }>(
    `/api/v1/admin/products?${qs}`,
  );
  return page.items.map(({ id, title, status }) => ({ id, title, status }));
}
