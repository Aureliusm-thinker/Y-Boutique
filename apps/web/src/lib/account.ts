import "server-only";
import { cookies } from "next/headers";
import { CUSTOMER_COOKIE, shopFetch, ShopApiError } from "./shop-api";

// Mirrors apps/api .../customer/CustomerDtos.java

export type Address = {
  id: number;
  label: string | null;
  fullName: string;
  phone: string | null;
  line1: string;
  line2: string | null;
  city: string;
  county: string;
  postalCode: string | null;
  isDefault: boolean;
};

export type StylePreferences = { sizes: string[]; colours: string[]; styles: string[] };

export type CustomerProfile = {
  id: number;
  email: string;
  fullName: string;
  phone: string | null;
  emailVerified: boolean;
  marketingOptIn: boolean;
  preferences: StylePreferences;
  addresses: Address[];
  createdAt: string;
};

/** The signed-in customer, or null for guests (or an expired session). */
export async function getCustomerProfile(): Promise<CustomerProfile | null> {
  if (!(await cookies()).get(CUSTOMER_COOKIE)?.value) return null;
  try {
    return await shopFetch<CustomerProfile>("/api/v1/account/me");
  } catch (e) {
    if (e instanceof ShopApiError && [401, 403, 404].includes(e.status)) return null;
    throw e;
  }
}

export async function setCustomerToken(token: string, expiresAt: string) {
  (await cookies()).set(CUSTOMER_COOKIE, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    expires: new Date(expiresAt),
  });
}

export async function clearCustomerToken() {
  (await cookies()).delete({ name: CUSTOMER_COOKIE, path: "/" });
}
