import { NextResponse } from "next/server";
import { getToken } from "@/lib/admin/session";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

/** Downloads every product as a CSV. The Java API checks the staff member's role. */
export async function GET(request: Request) {
  const token = await getToken();
  if (!token) return NextResponse.redirect(new URL("/admin/login", request.url));

  const res = await fetch(`${API_URL}/api/v1/admin/products/export`, {
    headers: { Authorization: `Bearer ${token}`, Accept: "text/csv" },
    cache: "no-store",
  });
  if (res.status === 401) return NextResponse.redirect(new URL("/admin/login", request.url));
  if (!res.ok) return new NextResponse("Export failed. Please try again.", { status: res.status });

  return new NextResponse(res.body, {
    headers: {
      "Content-Type": "text/csv; charset=utf-8",
      "Content-Disposition": res.headers.get("Content-Disposition") ?? 'attachment; filename="products.csv"',
      "Cache-Control": "no-store",
    },
  });
}
