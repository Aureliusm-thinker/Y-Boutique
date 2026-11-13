"use server";

import { refresh } from "next/cache";
import { shopFetch, ShopApiError } from "@/lib/shop-api";

export type ReturnRequestState = { ok?: boolean; message?: string; errors?: string[] };

export type ReturnView = {
  id: number;
  status: "REQUESTED" | "APPROVED" | "REJECTED" | "RECEIVED" | "REFUNDED";
  comment: string | null;
  staffComment: string | null;
  refundAmount: number | null;
  createdAt: string;
  lines: { id: number; orderLineId: number; title: string; size: string | null; quantity: number; reason: string }[];
};

export async function getReturns(number: string, token: string): Promise<ReturnView[]> {
  try {
    return await shopFetch<ReturnView[]>(`/api/v1/orders/${encodeURIComponent(number)}/returns?token=${encodeURIComponent(token)}`);
  } catch {
    return [];
  }
}

export async function getReviewedProductIds(number: string, token: string): Promise<number[]> {
  try {
    return await shopFetch<number[]>(`/api/v1/orders/${encodeURIComponent(number)}/reviews?token=${encodeURIComponent(token)}`);
  } catch {
    return [];
  }
}

export async function requestReturn(number: string, token: string, _prev: ReturnRequestState, fd: FormData): Promise<ReturnRequestState> {
  const lines = [...fd.keys()]
    .filter((k) => k.startsWith("qty-"))
    .map((k) => {
      const lineId = Number(k.slice(4));
      return { lineId, quantity: Number(fd.get(k)), reason: String(fd.get(`reason-${lineId}`) ?? "") };
    })
    .filter((l) => l.quantity > 0);
  if (lines.length === 0) return { ok: false, message: "Choose at least one item to return." };
  if (lines.some((l) => !l.reason)) return { ok: false, message: "Tell us why you're returning each item." };
  try {
    await shopFetch(`/api/v1/orders/${encodeURIComponent(number)}/returns`, {
      method: "POST",
      body: JSON.stringify({ token, lines, comment: String(fd.get("comment") ?? "").trim() || null }),
    });
  } catch (e) {
    if (e instanceof ShopApiError) return { ok: false, message: e.message, errors: e.errors };
    throw e;
  }
  refresh();
  return { ok: true, message: "Thanks — we've received your return request and emailed you the next steps." };
}

export async function submitReview(
  number: string,
  token: string,
  productId: number,
  _prev: ReturnRequestState,
  fd: FormData,
): Promise<ReturnRequestState> {
  const rating = Number(fd.get("rating"));
  if (!(rating >= 1 && rating <= 5)) return { ok: false, message: "Choose a star rating." };
  try {
    await shopFetch(`/api/v1/orders/${encodeURIComponent(number)}/reviews`, {
      method: "POST",
      body: JSON.stringify({
        token,
        productId,
        rating,
        title: String(fd.get("title") ?? "").trim() || null,
        body: String(fd.get("body") ?? "").trim() || null,
        fit: String(fd.get("fit") ?? "") || null,
      }),
    });
  } catch (e) {
    if (e instanceof ShopApiError) return { ok: false, message: e.message };
    throw e;
  }
  return { ok: true, message: "Thank you! Your review will appear once our team has checked it." };
}
