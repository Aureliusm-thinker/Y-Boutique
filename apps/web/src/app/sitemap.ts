import type { MetadataRoute } from "next";
import { getSitemapEntries } from "@/lib/api";

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

// sitemap.xml for search engines (NFR-08): home, shop, categories, live products and content pages.
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const entries = await getSitemapEntries();
  return [
    { url: `${SITE_URL}/`, changeFrequency: "daily", priority: 1 },
    { url: `${SITE_URL}/shop`, changeFrequency: "daily", priority: 0.9 },
    ...entries.map((e) => ({
      url: `${SITE_URL}${e.path}`,
      lastModified: e.updatedAt ?? undefined,
      changeFrequency: "weekly" as const,
      priority: e.path.startsWith("/products/") ? 0.8 : 0.7,
    })),
  ];
}
