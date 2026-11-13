import "server-only";
import { cookies } from "next/headers";

// Per-shopper API calls (cart, checkout, orders, account). Never cached.
const API_URL = process.env.API_URL ?? "http://localhost:8080";

export const CART_COOKIE = "yvette_cart";
export const CUSTOMER_COOKIE = "yvette_customer";

export class ShopApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public errors: string[] = [],
  ) {
    super(message);
  }
}

/** Calls the Java API, sending the signed-in customer's token when there is one. */
export async function shopFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (init.body && !(init.body instanceof FormData)) headers.set("Content-Type", "application/json");
  const customer = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (customer && !headers.has("Authorization")) headers.set("Authorization", `Bearer ${customer}`);

  const res = await fetch(`${API_URL}${path}`, { ...init, headers, cache: "no-store" }).catch(() => null);
  if (!res) throw new ShopApiError(503, "The shop is temporarily unavailable. Please try again.");
  if (res.status === 401 && customer) {
    // Expired customer session: retry as a guest so shopping isn't interrupted.
    headers.delete("Authorization");
    const retry = await fetch(`${API_URL}${path}`, { ...init, headers, cache: "no-store" });
    return handle<T>(retry);
  }
  return handle<T>(res);
}

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const problem = await res.json().catch(() => ({}));
    throw new ShopApiError(res.status, problem.detail ?? `Request failed (${res.status})`, problem.errors ?? []);
  }
  return (res.status === 204 ? undefined : await res.json()) as T;
}

export async function getCartToken(): Promise<string | undefined> {
  const token = (await cookies()).get(CART_COOKIE)?.value;
  return token && /^[0-9a-f-]{36}$/i.test(token) ? token : undefined;
}

export async function setCartToken(token: string) {
  (await cookies()).set(CART_COOKIE, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: 60 * 60 * 24 * 60, // 60 days
  });
}
