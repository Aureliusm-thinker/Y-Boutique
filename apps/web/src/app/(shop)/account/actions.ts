"use server";

import { refresh } from "next/cache";
import { redirect } from "next/navigation";
import { clearCustomerToken, setCustomerToken, type CustomerProfile } from "@/lib/account";
import { getCartToken, setCartToken, shopFetch, ShopApiError } from "@/lib/shop-api";

export type FormState = { ok?: boolean; message?: string; errors?: string[]; values?: Record<string, string> };

type Session = { accessToken: string; expiresAt: string; customer: CustomerProfile };

const str = (fd: FormData, k: string) => String(fd.get(k) ?? "").trim();
const fail = (e: unknown, values?: Record<string, string>): FormState => {
  if (e instanceof ShopApiError) return { ok: false, message: e.message, errors: e.errors, values };
  throw e;
};

/** Only same-site paths are allowed as "next" (no open redirects). */
function safeNext(next: string): string {
  return next.startsWith("/") && !next.startsWith("//") ? next : "/account";
}

/** Stores the session and joins the guest bag to the account (FR-CO-01 "merges on login"). */
async function startSession(session: Session) {
  await setCustomerToken(session.accessToken, session.expiresAt);
  const guestCart = await getCartToken();
  try {
    const { cartToken } = await shopFetch<{ cartToken: string }>("/api/v1/account/cart", {
      method: "POST",
      headers: { Authorization: `Bearer ${session.accessToken}` },
      body: JSON.stringify({ cartToken: guestCart ?? "00000000-0000-0000-0000-000000000000" }),
    });
    await setCartToken(cartToken);
  } catch {
    /* the bag is a convenience: never block sign-in on it */
  }
}

export async function login(_prev: FormState, fd: FormData): Promise<FormState> {
  const email = str(fd, "email");
  const next = safeNext(str(fd, "next") || "/account");
  try {
    const session = await shopFetch<Session>("/api/v1/auth/customer/login", {
      method: "POST",
      headers: { Authorization: "" },
      body: JSON.stringify({ email, password: String(fd.get("password") ?? "") }),
    });
    await startSession(session);
  } catch (e) {
    if (e instanceof ShopApiError && e.status === 401) return { ok: false, message: "Wrong email or password.", values: { email } };
    return fail(e, { email });
  }
  redirect(next);
}

export async function register(_prev: FormState, fd: FormData): Promise<FormState> {
  const values = { email: str(fd, "email"), fullName: str(fd, "fullName") };
  const password = String(fd.get("password") ?? "");
  if (password.length < 8) return { ok: false, message: "Use at least 8 characters for your password.", values };
  try {
    const session = await shopFetch<Session>("/api/v1/auth/customer/register", {
      method: "POST",
      headers: { Authorization: "" },
      body: JSON.stringify({ ...values, password, marketingOptIn: fd.get("marketingOptIn") === "on" }),
    });
    await startSession(session);
  } catch (e) {
    return fail(e, values);
  }
  redirect("/account?welcome=1");
}

export async function logout() {
  await clearCustomerToken();
  redirect("/");
}

export async function verifyEmail(token: string): Promise<FormState> {
  try {
    const session = await shopFetch<Session>("/api/v1/auth/customer/verify-email", {
      method: "POST",
      headers: { Authorization: "" },
      body: JSON.stringify({ token }),
    });
    await startSession(session);
    return { ok: true };
  } catch (e) {
    return fail(e);
  }
}

export async function resendVerification(): Promise<FormState> {
  try {
    await shopFetch("/api/v1/account/me/resend-verification", { method: "POST" });
    return { ok: true, message: "We've sent a new link. Check your inbox." };
  } catch (e) {
    return fail(e);
  }
}

export async function forgotPassword(_prev: FormState, fd: FormData): Promise<FormState> {
  const email = str(fd, "email");
  try {
    await shopFetch("/api/v1/auth/customer/forgot-password", {
      method: "POST",
      headers: { Authorization: "" },
      body: JSON.stringify({ email }),
    });
  } catch (e) {
    return fail(e, { email });
  }
  return { ok: true, message: `If ${email} has an account, we've emailed a link to reset the password.` };
}

export async function resetPassword(_prev: FormState, fd: FormData): Promise<FormState> {
  const password = String(fd.get("password") ?? "");
  if (password.length < 8) return { ok: false, message: "Use at least 8 characters." };
  if (password !== String(fd.get("confirm") ?? "")) return { ok: false, message: "The passwords don't match." };
  try {
    const session = await shopFetch<Session>("/api/v1/auth/customer/reset-password", {
      method: "POST",
      headers: { Authorization: "" },
      body: JSON.stringify({ token: str(fd, "token"), password }),
    });
    await startSession(session);
  } catch (e) {
    return fail(e);
  }
  redirect("/account?reset=1");
}

// ------------------------------------------------------------------ profile

export async function updateProfile(_prev: FormState, fd: FormData): Promise<FormState> {
  try {
    await shopFetch("/api/v1/account/me", {
      method: "PUT",
      body: JSON.stringify({ fullName: str(fd, "fullName"), phone: str(fd, "phone") || null, marketingOptIn: fd.get("marketingOptIn") === "on" }),
    });
  } catch (e) {
    return fail(e);
  }
  refresh();
  return { ok: true, message: "Saved." };
}

export async function updatePreferences(_prev: FormState, fd: FormData): Promise<FormState> {
  try {
    await shopFetch("/api/v1/account/me/preferences", {
      method: "PUT",
      body: JSON.stringify({
        sizes: fd.getAll("sizes").map(String),
        colours: str(fd, "colours").split(",").map((s) => s.trim()).filter(Boolean),
        styles: fd.getAll("styles").map(String),
      }),
    });
  } catch (e) {
    return fail(e);
  }
  refresh();
  return { ok: true, message: "Style preferences saved." };
}

export async function changePassword(_prev: FormState, fd: FormData): Promise<FormState> {
  const newPassword = String(fd.get("newPassword") ?? "");
  if (newPassword.length < 8) return { ok: false, message: "Use at least 8 characters." };
  try {
    await shopFetch("/api/v1/account/me/password", {
      method: "PUT",
      body: JSON.stringify({ currentPassword: String(fd.get("currentPassword") ?? ""), newPassword }),
    });
  } catch (e) {
    return fail(e);
  }
  return { ok: true, message: "Password changed." };
}

export async function saveAddress(_prev: FormState, fd: FormData): Promise<FormState> {
  const id = str(fd, "id");
  const body = {
    label: str(fd, "label") || null,
    fullName: str(fd, "fullName"),
    phone: str(fd, "phone") || null,
    line1: str(fd, "line1"),
    line2: str(fd, "line2") || null,
    city: str(fd, "city"),
    county: str(fd, "county"),
    postalCode: str(fd, "postalCode") || null,
    makeDefault: fd.get("makeDefault") === "on",
  };
  try {
    await shopFetch(id ? `/api/v1/account/addresses/${id}` : "/api/v1/account/addresses", {
      method: id ? "PUT" : "POST",
      body: JSON.stringify(body),
    });
  } catch (e) {
    return fail(e);
  }
  refresh();
  return { ok: true, message: "Address saved." };
}

export async function deleteAddress(id: number): Promise<FormState> {
  try {
    await shopFetch(`/api/v1/account/addresses/${id}`, { method: "DELETE" });
  } catch (e) {
    return fail(e);
  }
  refresh();
  return { ok: true };
}

export async function deleteAccount(_prev: FormState, fd: FormData): Promise<FormState> {
  if (str(fd, "confirm").toUpperCase() !== "DELETE") return { ok: false, message: "Type DELETE to confirm." };
  try {
    await shopFetch("/api/v1/account/delete", {
      method: "POST",
      body: JSON.stringify({ password: String(fd.get("password") ?? "") }),
    });
  } catch (e) {
    return fail(e);
  }
  await clearCustomerToken();
  redirect("/?account-deleted=1");
}

// ------------------------------------------------------------------ wishlist & alerts

export async function toggleWishlist(productId: number, add: boolean): Promise<FormState> {
  try {
    await shopFetch(`/api/v1/account/wishlist/${productId}`, { method: add ? "PUT" : "DELETE" });
  } catch (e) {
    if (e instanceof ShopApiError && e.status === 401) return { ok: false, message: "signin" };
    return fail(e);
  }
  if (add) refresh();
  return { ok: true };
}

export async function wishlistIds(): Promise<number[]> {
  try {
    return await shopFetch<number[]>("/api/v1/account/wishlist/ids");
  } catch {
    return [];
  }
}

export async function notifyWhenBack(variantId: number, email: string): Promise<FormState> {
  try {
    await shopFetch("/api/v1/catalog/stock-alerts", { method: "POST", body: JSON.stringify({ variantId, email }) });
  } catch (e) {
    return fail(e);
  }
  return { ok: true, message: "We'll email you when it's back." };
}

export async function lookupOrder(_prev: FormState, fd: FormData): Promise<FormState> {
  const values = { number: str(fd, "number"), email: str(fd, "email") };
  let found: { number: string; accessToken: string };
  try {
    found = await shopFetch("/api/v1/orders/lookup", { method: "POST", body: JSON.stringify(values) });
  } catch (e) {
    return fail(e, values);
  }
  redirect(`/order/${found.number}?t=${found.accessToken}`);
}
