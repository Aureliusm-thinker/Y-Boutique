import "server-only";
import { getCartToken, shopFetch, ShopApiError } from "./shop-api";
import type { Cart } from "./types";

/** The shopper's cart, or null if they don't have one yet (or it was removed). */
export async function getCart(): Promise<Cart | null> {
  const token = await getCartToken();
  if (!token) return null;
  try {
    return await shopFetch<Cart>(`/api/v1/carts/${token}`);
  } catch (e) {
    if (e instanceof ShopApiError && e.status === 404) return null;
    throw e;
  }
}
