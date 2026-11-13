import "server-only";
import { cacheLife, cacheTag } from "next/cache";
import { filtersToQuery } from "./shop-filters";
import type {
  Category,
  Facets,
  Page,
  ProductDetail,
  ProductSummary,
  ShopFilters,
  SitemapEntry,
} from "./types";

// Server-side base URL of the Java API (see .env.example).
const API_URL = process.env.API_URL ?? "http://localhost:8080";

async function getJson<T>(path: string): Promise<T | null> {
  const res = await fetch(`${API_URL}${path}`, { headers: { Accept: "application/json" } });
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`API ${path} failed with ${res.status}`);
  return (await res.json()) as T;
}

// Catalogue data is the same for every shopper, so it is cached and shared.
// The back office invalidates these tags when products or categories change.

export async function getCategories(): Promise<Category[]> {
  "use cache";
  cacheLife("hours");
  cacheTag("categories");
  return (await getJson<Category[]>("/api/v1/catalog/categories")) ?? [];
}

export const PAGE_SIZE = 24;

export async function getProducts(filters: ShopFilters = {}): Promise<Page<ProductSummary>> {
  "use cache";
  cacheLife("minutes");
  cacheTag("products");
  const qs = filtersToQuery(filters);
  qs.set("size", String(PAGE_SIZE));
  const result = await getJson<Page<ProductSummary>>(`/api/v1/catalog/products?${qs}`);
  return result ?? { items: [], page: 0, size: PAGE_SIZE, totalItems: 0, totalPages: 0 };
}

export async function getFacets(category?: string): Promise<Facets> {
  "use cache";
  cacheLife("minutes");
  cacheTag("products");
  const qs = category ? `?category=${encodeURIComponent(category)}` : "";
  return (
    (await getJson<Facets>(`/api/v1/catalog/facets${qs}`)) ?? { sizes: [], colours: [], minPrice: null, maxPrice: null }
  );
}

export async function getProduct(slug: string): Promise<ProductDetail | null> {
  "use cache";
  cacheLife("minutes");
  cacheTag("products", `product:${slug}`);
  return getJson<ProductDetail>(`/api/v1/catalog/products/${encodeURIComponent(slug)}`);
}

export async function getSitemapEntries(): Promise<SitemapEntry[]> {
  "use cache";
  cacheLife("hours");
  cacheTag("products", "categories");
  return (await getJson<SitemapEntry[]>("/api/v1/catalog/sitemap")) ?? [];
}

// ---- Reviews and staff-edited content (cached; the back office refreshes the tags on change)

export type ProductReviews = {
  average: number | null;
  count: number;
  fit: { runsSmall: number; trueToSize: number; runsLarge: number };
  reviews: {
    id: number;
    authorName: string;
    rating: number;
    title: string | null;
    body: string | null;
    fit: string | null;
    sizeBought: string | null;
    verifiedBuyer: boolean;
    createdAt: string;
  }[];
};

export async function getReviews(slug: string): Promise<ProductReviews | null> {
  "use cache";
  cacheLife("minutes");
  cacheTag("reviews", `reviews:${slug}`);
  return getJson<ProductReviews>(`/api/v1/catalog/products/${encodeURIComponent(slug)}/reviews`);
}

export type ContentPage = { slug: string; title: string; body: string; publishedAt: string | null };

export async function getContentPage(slug: string): Promise<ContentPage | null> {
  "use cache";
  cacheLife("hours");
  cacheTag("content", `page:${slug}`);
  return getJson<ContentPage>(`/api/v1/content/pages/${encodeURIComponent(slug)}`);
}

export async function getContentSlugs(): Promise<string[]> {
  "use cache";
  cacheLife("hours");
  cacheTag("content");
  return (await getJson<string[]>("/api/v1/content/pages")) ?? [];
}

export type HomeContent = {
  heroEyebrow?: string;
  heroTitle?: string;
  heroText?: string;
  heroCta?: string;
  heroLink?: string;
  heroImage?: string | null;
  featuredCategory?: string | null;
  featuredTitle?: string;
};

export type Announcement = { text?: string; link?: string | null; enabled?: boolean };

export async function getSiteContent(): Promise<{ home: HomeContent; announcement: Announcement }> {
  "use cache";
  cacheLife("hours");
  cacheTag("content");
  const site = await getJson<{ home?: HomeContent; announcement?: Announcement }>("/api/v1/content/site").catch(() => null);
  return { home: site?.home ?? {}, announcement: site?.announcement ?? {} };
}
