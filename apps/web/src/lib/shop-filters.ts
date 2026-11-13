import type { ShopFilters, SortOption } from "./types";

const SORTS: SortOption[] = ["newest", "price_asc", "price_desc", "relevance"];

type Params = Record<string, string | string[] | undefined>;

const one = (v: string | string[] | undefined) => (Array.isArray(v) ? v[0] : v);
const list = (v: string | string[] | undefined) =>
  (Array.isArray(v) ? v : (v ?? "").split(","))
    .flatMap((s) => s.split(","))
    .map((s) => s.trim())
    .filter(Boolean)
    .slice(0, 20);
const num = (v: string | string[] | undefined) => {
  const n = Number(one(v));
  return one(v) && Number.isFinite(n) && n >= 0 ? n : undefined;
};

/** Reads shop filters from URL search params (tolerant of junk values). */
export function parseShopFilters(sp: Params): ShopFilters {
  const sort = one(sp.sort) as SortOption | undefined;
  const page = num(sp.page);
  return {
    category: one(sp.category) || undefined,
    q: one(sp.q)?.trim().slice(0, 100) || undefined,
    sizes: list(sp.sizes),
    colours: list(sp.colours),
    minPrice: num(sp.minPrice),
    maxPrice: num(sp.maxPrice),
    inStock: one(sp.inStock) === "true",
    sort: sort && SORTS.includes(sort) ? sort : undefined,
    page: page ? Math.floor(page) : undefined,
  };
}

/** Query string for filters (empty values are dropped). Used for /shop URLs and the API call. */
export function filtersToQuery(f: ShopFilters): URLSearchParams {
  const qs = new URLSearchParams();
  if (f.category) qs.set("category", f.category);
  if (f.q) qs.set("q", f.q);
  if (f.sizes?.length) qs.set("sizes", f.sizes.join(","));
  if (f.colours?.length) qs.set("colours", f.colours.join(","));
  if (f.minPrice != null) qs.set("minPrice", String(f.minPrice));
  if (f.maxPrice != null) qs.set("maxPrice", String(f.maxPrice));
  if (f.inStock) qs.set("inStock", "true");
  if (f.sort) qs.set("sort", f.sort);
  if (f.page) qs.set("page", String(f.page));
  return qs;
}

/** Builds a /shop URL from filters. */
export function shopHref(f: ShopFilters): string {
  const s = filtersToQuery(f).toString();
  return s ? `/shop?${s}` : "/shop";
}
