import "server-only";
import { cookies } from "next/headers";

// The staff access token lives in an httpOnly cookie: JavaScript in the browser can never read it.
// Only server code (Server Components / Server Actions) uses it to call the Java API.
const COOKIE = "yvette_staff";

export async function getToken(): Promise<string | undefined> {
  return (await cookies()).get(COOKIE)?.value;
}

export async function setToken(token: string, expiresAt: string) {
  (await cookies()).set(COOKIE, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/admin",
    expires: new Date(expiresAt),
  });
}

export async function clearToken() {
  (await cookies()).delete({ name: COOKIE, path: "/admin" });
}
