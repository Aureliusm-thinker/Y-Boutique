// Mirrors apps/api .../catalog/AdminCatalogDtos.java, staff/StaffDtos.java, inventory/*

export type StaffRole = "OWNER" | "MANAGER" | "INVENTORY" | "FULFILMENT" | "SUPPORT" | "MARKETING";
export const STAFF_ROLES: StaffRole[] = ["OWNER", "MANAGER", "INVENTORY", "FULFILMENT", "SUPPORT", "MARKETING"];

export type Staff = {
  id: number;
  email: string;
  fullName: string;
  role: StaffRole;
  active: boolean;
  lastLoginAt: string | null;
  mfaEnabled: boolean;
};

/** The signed-in staff member (GET /api/v1/admin/me). */
export type Me = Staff & { mfaRequired: boolean };

export type ProductStatus = "DRAFT" | "ACTIVE" | "ARCHIVED";

export type AdminProductSummary = {
  id: number;
  title: string;
  slug: string;
  status: ProductStatus;
  categoryName: string | null;
  variantCount: number;
  totalOnHand: number;
  priceMin: number | null;
  priceMax: number | null;
  currency: string;
  imageUrl: string | null;
  updatedAt: string;
};

export type AdminVariant = {
  id: number;
  sku: string;
  size: string | null;
  colour: string | null;
  price: number;
  compareAtPrice: number | null;
  currency: string;
  onHand: number;
  reserved: number;
  available: number;
  lowStockThreshold: number;
  low: boolean;
};

export type AdminProductDetail = {
  id: number;
  title: string;
  slug: string;
  status: ProductStatus;
  description: string | null;
  categoryId: number | null;
  material: string | null;
  fit: string | null;
  occasion: string | null;
  season: string | null;
  styleTags: string[];
  seoTitle: string | null;
  seoDescription: string | null;
  variants: AdminVariant[];
  images: { url: string; altText: string | null }[];
  publishProblems: string[];
  related: { id: number; title: string; slug: string; status: ProductStatus }[];
  createdAt: string;
  updatedAt: string;
};

export type AdminCategory = { id: number; name: string; slug: string; position: number; productCount: number };

export type LowStockRow = {
  variantId: number;
  sku: string;
  size: string | null;
  colour: string | null;
  productId: number;
  productTitle: string;
  onHand: number;
  reserved: number;
  lowStockThreshold: number;
};

export type AdjustmentReason = "RECEIVED" | "RETURNED" | "DAMAGED" | "LOST" | "CORRECTION" | "OTHER";
export const ADJUSTMENT_REASONS: AdjustmentReason[] = ["RECEIVED", "RETURNED", "DAMAGED", "LOST", "CORRECTION", "OTHER"];

/** Payload sent to POST/PUT /api/v1/admin/products */
export type ProductUpsert = {
  title: string;
  slug?: string;
  description?: string;
  categoryId?: number | null;
  material?: string;
  fit?: string;
  occasion?: string;
  season?: string;
  styleTags: string[];
  seoTitle?: string;
  seoDescription?: string;
  variants: {
    id?: number;
    sku: string;
    size?: string;
    colour?: string;
    price: number;
    compareAtPrice?: number | null;
    lowStockThreshold?: number;
    openingStock?: number;
  }[];
  images: { url: string; altText?: string }[];
};

/** Common result shape for admin server actions. */
export type ActionResult = { ok: boolean; message?: string; errors?: string[] };

/** Which roles may do what — mirrors apps/api .../staff/Roles.java (the API is the real gate). */
export const can = {
  editCatalog: (r: StaffRole) => r === "OWNER" || r === "MANAGER" || r === "INVENTORY",
  viewCatalog: (r: StaffRole) => r === "OWNER" || r === "MANAGER" || r === "INVENTORY" || r === "MARKETING",
  manageStaff: (r: StaffRole) => r === "OWNER",
};
