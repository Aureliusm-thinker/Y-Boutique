import "server-only";
import { cacheLife, cacheTag } from "next/cache";
import type { Category, Page, ProductDetail, ProductSummary } from "./types";

// Server-side base URL of the Java API (see .env.example).
const API_URL = process.env.API_URL ?? "http://localhost:8080";

async function getJson<T>(path: string): Promise<T | null> {
  const res = await fetch(`${API_URL}${path}`, { headers: { Accept: "application/json" } });
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`API ${path} failed with ${res.status}`);
  return (await res.json()) as T;
}

// Catalogue data is the same for every shopper, so it is cached and shared.
// Tags let the admin invalidate on product changes later (revalidateTag("products")).

export async function getCategories(): Promise<Category[]> {
  "use cache";
  cacheLife("hours");
  cacheTag("categories");
  return (await getJson<Category[]>("/api/v1/catalog/categories")) ?? [];
}

export async function getProducts(category?: string, page = 0): Promise<Page<ProductSummary>> {
  "use cache";
  cacheLife("minutes");
  cacheTag("products");
  const qs = new URLSearchParams({ page: String(page), size: "24" });
  if (category) qs.set("category", category);
  const result = await getJson<Page<ProductSummary>>(`/api/v1/catalog/products?${qs}`);
  return result ?? { items: [], page: 0, size: 24, totalItems: 0, totalPages: 0 };
}

export async function getProduct(slug: string): Promise<ProductDetail | null> {
  "use cache";
  cacheLife("minutes");
  cacheTag("products", `product:${slug}`);
  return getJson<ProductDetail>(`/api/v1/catalog/products/${encodeURIComponent(slug)}`);
}
