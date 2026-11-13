"use server";

import { refresh } from "next/cache";
import { refreshStorefront } from "@/lib/admin/storefront-cache";
import { redirect } from "next/navigation";
import { adminFetch, toActionError } from "@/lib/admin/api";
import type { ActionResult } from "@/lib/admin/types";

export async function savePageDraft(slug: string, _prev: ActionResult | null, fd: FormData): Promise<ActionResult> {
  try {
    await adminFetch(`/api/v1/admin/content/pages/${slug}`, {
      method: "PUT",
      body: JSON.stringify({ title: String(fd.get("title") ?? ""), body: String(fd.get("body") ?? "") }),
    });
  } catch (e) {
    return toActionError(e);
  }
  refresh();
  return { ok: true, message: "Draft saved. Preview it, then publish." };
}

export async function publishPage(slug: string, publish: boolean): Promise<ActionResult> {
  try {
    await adminFetch(`/api/v1/admin/content/pages/${slug}/${publish ? "publish" : "unpublish"}`, { method: "POST" });
  } catch (e) {
    return toActionError(e);
  }
  refreshStorefront("content");
  refresh();
  return { ok: true, message: publish ? "Published — it's live now." : "Unpublished." };
}

export async function createPage(_prev: ActionResult | null, fd: FormData): Promise<ActionResult> {
  const slug = String(fd.get("slug") ?? "").trim().toLowerCase();
  try {
    await adminFetch("/api/v1/admin/content/pages", {
      method: "POST",
      body: JSON.stringify({ slug, title: String(fd.get("title") ?? "").trim() }),
    });
  } catch (e) {
    return toActionError(e);
  }
  redirect(`/admin/content/pages/${slug}`);
}

export async function saveSiteDraft(key: string, _prev: ActionResult | null, fd: FormData): Promise<ActionResult> {
  const draft: Record<string, unknown> = {};
  for (const [k, v] of fd.entries()) {
    if (typeof v === "string") draft[k] = v.trim() === "" ? null : v.trim();
  }
  if (key === "announcement") draft.enabled = fd.get("enabled") === "on";
  try {
    await adminFetch(`/api/v1/admin/content/site/${key}`, { method: "PUT", body: JSON.stringify(draft) });
  } catch (e) {
    return toActionError(e);
  }
  refresh();
  return { ok: true, message: "Draft saved. Check the preview, then publish." };
}

export async function publishSite(key: string): Promise<ActionResult> {
  try {
    await adminFetch(`/api/v1/admin/content/site/${key}/publish`, { method: "POST" });
  } catch (e) {
    return toActionError(e);
  }
  refreshStorefront("content");
  refresh();
  return { ok: true, message: "Published — it's live now." };
}
