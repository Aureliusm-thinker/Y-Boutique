// Mirrors the Java API DTOs in apps/api/.../catalog/CatalogDtos.java

export type Category = { id: number; slug: string; name: string };

export type ProductSummary = {
  id: number;
  slug: string;
  title: string;
  categorySlug: string | null;
  priceFrom: number | null;
  compareAtPrice: number | null;
  currency: string | null;
  imageUrl: string | null;
  imageAlt: string | null;
};

export type Variant = {
  id: number;
  sku: string;
  size: string | null;
  colour: string | null;
  price: number;
  compareAtPrice: number | null;
  currency: string;
  inStock: boolean;
};

export type ProductDetail = {
  id: number;
  slug: string;
  title: string;
  description: string | null;
  category: Category | null;
  material: string | null;
  fit: string | null;
  occasion: string | null;
  season: string | null;
  styleTags: string[];
  variants: Variant[];
  images: { url: string; altText: string | null }[];
  seoTitle: string | null;
  seoDescription: string | null;
};

export type Page<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};
