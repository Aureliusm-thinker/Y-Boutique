import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { CUSTOMER_COOKIE } from "@/lib/shop-api";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

/** Streams the signed-in customer's data export as a download (FR-CA-08). */
export async function GET(request: Request) {
  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) return NextResponse.redirect(new URL("/account/login?next=/account/privacy", request.url));
  const res = await fetch(`${API_URL}/api/v1/account/export`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
  if (!res.ok) return NextResponse.redirect(new URL("/account/login?next=/account/privacy", request.url));
  return new NextResponse(res.body, {
    headers: {
      "Content-Type": "application/json",
      "Content-Disposition": 'attachment; filename="yvette-my-data.json"',
      "Cache-Control": "no-store",
    },
  });
}
