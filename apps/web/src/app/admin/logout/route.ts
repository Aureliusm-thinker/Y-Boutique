import { NextResponse } from "next/server";
import { clearToken } from "@/lib/admin/session";

/**
 * Sign out with a real (non-client-side) navigation: the browser loads the login page fresh,
 * so no admin pages kept in the client router's back/forward cache survive in the tab.
 */
export async function POST(request: Request) {
  await clearToken();
  return NextResponse.redirect(new URL("/admin/login", request.url), 303);
}
