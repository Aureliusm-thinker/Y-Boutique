"use server";

import { refresh } from "next/cache";
import { redirect } from "next/navigation";
import { getCartToken, setCartToken, shopFetch, ShopApiError } from "@/lib/shop-api";
import type { Cart, Order, PaymentInfo } from "@/lib/types";

export type CartActionResult = { ok: boolean; message?: string; errors?: string[] };

function failure(e: unknown): CartActionResult {
  if (e instanceof ShopApiError) return { ok: false, message: e.message, errors: e.errors };
  throw e;
}

/** The shopper's cart token, creating a cart (and its cookie) the first time. */
async function ensureCart(): Promise<string> {
  const existing = await getCartToken();
  if (existing) return existing;
  const cart = await shopFetch<Cart>("/api/v1/carts", { method: "POST" });
  await setCartToken(cart.token);
  return cart.token;
}

export async function addToCart(variantId: number, quantity = 1): Promise<CartActionResult & { itemCount?: number }> {
  try {
    let token = await ensureCart();
    let cart: Cart;
    try {
      cart = await shopFetch<Cart>(`/api/v1/carts/${token}/items`, {
        method: "POST",
        body: JSON.stringify({ variantId, quantity }),
      });
    } catch (e) {
      // The cart behind the cookie is gone (e.g. database reset): start a fresh one.
      if (!(e instanceof ShopApiError && e.status === 404 && e.message === "Cart not found")) throw e;
      const fresh = await shopFetch<Cart>("/api/v1/carts", { method: "POST" });
      await setCartToken(fresh.token);
      token = fresh.token;
      cart = await shopFetch<Cart>(`/api/v1/carts/${token}/items`, {
        method: "POST",
        body: JSON.stringify({ variantId, quantity }),
      });
    }
    refresh();
    return { ok: true, itemCount: cart.itemCount };
  } catch (e) {
    return failure(e);
  }
}

export async function setQuantity(variantId: number, quantity: number): Promise<CartActionResult> {
  const token = await getCartToken();
  if (!token) return { ok: false, message: "Your bag is empty." };
  try {
    await shopFetch(`/api/v1/carts/${token}/items/${variantId}`, {
      method: "PUT",
      body: JSON.stringify({ quantity }),
    });
  } catch (e) {
    return failure(e);
  }
  refresh();
  return { ok: true };
}

export async function removeFromCart(variantId: number): Promise<CartActionResult> {
  const token = await getCartToken();
  if (!token) return { ok: true };
  try {
    await shopFetch(`/api/v1/carts/${token}/items/${variantId}`, { method: "DELETE" });
  } catch (e) {
    return failure(e);
  }
  refresh();
  return { ok: true };
}

export async function applyDiscount(_prev: CartActionResult | null, formData: FormData): Promise<CartActionResult> {
  const token = await getCartToken();
  if (!token) return { ok: false, message: "Your bag is empty." };
  const code = String(formData.get("code") ?? "").trim();
  try {
    await shopFetch(`/api/v1/carts/${token}/discount`, { method: "PUT", body: JSON.stringify({ code }) });
  } catch (e) {
    return failure(e);
  }
  refresh();
  return { ok: true, message: code ? `Code ${code.toUpperCase()} applied.` : "Code removed." };
}

// ------------------------------------------------------------------ checkout

export type CheckoutState = CartActionResult & { values?: Record<string, string> };

export async function placeOrder(_prev: CheckoutState, formData: FormData): Promise<CheckoutState> {
  const token = await getCartToken();
  const values = Object.fromEntries(
    [...formData.entries()].filter(([, v]) => typeof v === "string").map(([k, v]) => [k, String(v)]),
  );
  if (!token) return { ok: false, message: "Your bag is empty.", values };

  const field = (name: string) => values[name]?.trim() || undefined;
  let result: { orderNumber: string; accessToken: string; payment: PaymentInfo | null };
  try {
    result = await shopFetch("/api/v1/checkout", {
      method: "POST",
      body: JSON.stringify({
        cartToken: token,
        email: field("email"),
        phone: field("phone"),
        fullName: field("fullName"),
        addressLine1: field("addressLine1"),
        addressLine2: field("addressLine2"),
        city: field("city"),
        county: field("county"),
        postalCode: field("postalCode"),
        deliveryNotes: field("deliveryNotes"),
        shippingMethod: field("shippingMethod"),
        paymentMethod: field("paymentMethod"),
        mpesaPhone: field("mpesaPhone"),
      }),
    });
  } catch (e) {
    if (e instanceof ShopApiError) return { ok: false, message: e.message, errors: e.errors, values };
    throw e;
  }
  if (result.payment?.nextAction === "REDIRECT" && result.payment.redirectUrl) redirect(result.payment.redirectUrl);
  redirect(`/checkout/pay/${result.orderNumber}?t=${result.accessToken}`);
}

// ------------------------------------------------------------------ payment page

export async function getOrder(number: string, accessToken: string): Promise<Order | null> {
  try {
    return await shopFetch<Order>(`/api/v1/orders/${encodeURIComponent(number)}?token=${encodeURIComponent(accessToken)}`);
  } catch (e) {
    if (e instanceof ShopApiError && (e.status === 404 || e.status === 400)) return null;
    throw e;
  }
}

export async function retryPayment(number: string, accessToken: string, mpesaPhone?: string): Promise<CartActionResult & { redirectUrl?: string }> {
  try {
    const payment = await shopFetch<PaymentInfo>(`/api/v1/orders/${encodeURIComponent(number)}/pay`, {
      method: "POST",
      body: JSON.stringify({ token: accessToken, mpesaPhone: mpesaPhone || undefined }),
    });
    return { ok: true, redirectUrl: payment.nextAction === "REDIRECT" ? (payment.redirectUrl ?? undefined) : undefined };
  } catch (e) {
    return failure(e);
  }
}

export async function payWithTestCard(number: string, accessToken: string, cardNumber: string): Promise<CartActionResult> {
  try {
    await shopFetch(`/api/v1/orders/${encodeURIComponent(number)}/test-card`, {
      method: "POST",
      body: JSON.stringify({ token: accessToken, cardNumber }),
    });
    return { ok: true };
  } catch (e) {
    return failure(e);
  }
}

export async function simulateMpesa(number: string, accessToken: string, approve: boolean): Promise<CartActionResult> {
  try {
    await shopFetch(`/api/v1/orders/${encodeURIComponent(number)}/test-mpesa`, {
      method: "POST",
      body: JSON.stringify({ token: accessToken, approve }),
    });
    return { ok: true };
  } catch (e) {
    return failure(e);
  }
}
