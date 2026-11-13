import type { MetadataRoute } from "next";
import { getContentSlugs, getSitemapEntries } from "@/lib/api";

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

// sitemap.xml for search engines (NFR-08): home, shop, categories, live products and content pages.
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const [entries, pages] = await Promise.all([getSitemapEntries(), getContentSlugs()]);
  return [
    { url: `${SITE_URL}/`, changeFrequency: "daily", priority: 1 },
    { url: `${SITE_URL}/shop`, changeFrequency: "daily", priority: 0.9 },
    ...entries.map((e) => ({
      url: `${SITE_URL}${e.path}`,
      lastModified: e.updatedAt ?? undefined,
      changeFrequency: "weekly" as const,
      priority: e.path.startsWith("/products/") ? 0.8 : 0.7,
    })),
    ...pages.map((slug) => ({ url: `${SITE_URL}/pages/${slug}`, changeFrequency: "monthly" as const, priority: 0.3 })),
  ];
}
