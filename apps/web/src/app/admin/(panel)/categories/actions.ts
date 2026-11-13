"use server";

import { refresh } from "next/cache";
import { refreshStorefront } from "@/lib/admin/storefront-cache";
import { adminFetch, toActionError } from "@/lib/admin/api";
import type { ActionResult } from "@/lib/admin/types";

export async function saveCategory(_prev: ActionResult | null, formData: FormData): Promise<ActionResult> {
  const id = String(formData.get("id") ?? "");
  const name = String(formData.get("name") ?? "").trim();
  const slug = String(formData.get("slug") ?? "").trim();
  const position = Number(formData.get("position") ?? 0) || 0;
  if (!name) return { ok: false, message: "Enter a name." };
  try {
    await adminFetch(id ? `/api/v1/admin/categories/${id}` : "/api/v1/admin/categories", {
      method: id ? "PUT" : "POST",
      body: JSON.stringify({ name, slug: slug || null, position }),
    });
  } catch (e) {
    return toActionError(e);
  }
  refreshStorefront("categories", "products"); // storefront navigation
  refresh();
  return { ok: true, message: id ? "Saved." : `Category "${name}" added.` };
}
