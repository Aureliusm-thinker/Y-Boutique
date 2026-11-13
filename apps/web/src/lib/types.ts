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
  soldOut: boolean;
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
  /** Set only when 1–5 are left ("Only N left"). */
  stockLeft: number | null;
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
  updatedAt: string;
  completeTheLook: ProductSummary[];
};

export type Page<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

export type Facets = { sizes: string[]; colours: string[]; minPrice: number | null; maxPrice: number | null };

export type Suggestions = { products: ProductSummary[]; categories: Category[] };

export type SitemapEntry = { path: string; updatedAt: string | null };

export type SortOption = "newest" | "price_asc" | "price_desc" | "relevance";

/** Shop filters as they appear in the URL (/shop?category=dresses&sizes=S,M&sort=price_asc). */
export type ShopFilters = {
  category?: string;
  q?: string;
  sizes?: string[];
  colours?: string[];
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  sort?: SortOption;
  page?: number;
};

// ---- Cart, checkout and orders (apps/api .../cart/CartDtos.java, order/OrderDtos.java)

export type CartLine = {
  variantId: number;
  productSlug: string;
  title: string;
  size: string | null;
  colour: string | null;
  sku: string;
  imageUrl: string | null;
  unitPrice: number;
  compareAtPrice: number | null;
  quantity: number;
  lineTotal: number;
  available: number;
  problem: string | null;
};

export type ShippingOption = {
  code: string;
  name: string;
  description: string | null;
  price: number;
  freeOver: number | null;
  needsAddress: boolean;
};

export type Cart = {
  token: string;
  lines: CartLine[];
  itemCount: number;
  subtotal: number;
  discount: { code: string | null; name: string | null; amount: number; freeShipping: boolean; problem: string | null } | null;
  totalBeforeShipping: number;
  shippingOptions: ShippingOption[];
  currency: string;
  readyForCheckout: boolean;
};

export type CheckoutOptions = { paymentMethods: ("MPESA" | "CARD")[]; testMode: boolean; counties: string[] };

export type NextAction = "ENTER_TEST_CARD" | "WAIT_FOR_PHONE" | "REDIRECT" | "NONE";

export type PaymentInfo = {
  provider: string;
  method: "MPESA" | "CARD";
  status: "PENDING" | "SUCCEEDED" | "FAILED";
  nextAction: NextAction;
  redirectUrl: string | null;
  phone: string | null;
  failureMessage: string | null;
  testMode: boolean;
};

export type OrderStatus = "PENDING_PAYMENT" | "PAID" | "SHIPPED" | "DELIVERED" | "CANCELLED";

export type OrderLine = {
  id: number;
  productId: number | null;
  sku: string;
  title: string;
  size: string | null;
  colour: string | null;
  imageUrl: string | null;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
  returnedQuantity: number;
};

export type Order = {
  number: string;
  accessToken: string;
  status: OrderStatus;
  paymentStatus: "PENDING" | "PAID" | "FAILED" | "REFUNDED" | "PARTIALLY_REFUNDED";
  paymentMethod: "MPESA" | "CARD";
  email: string;
  phone: string;
  shipTo: {
    name: string;
    line1: string | null;
    line2: string | null;
    city: string | null;
    county: string | null;
    postalCode: string | null;
    notes: string | null;
  };
  shippingMethod: string;
  lines: OrderLine[];
  subtotal: number;
  discountTotal: number;
  discountCode: string | null;
  shippingTotal: number;
  total: number;
  refundedTotal: number;
  currency: string;
  carrier: string | null;
  trackingNumber: string | null;
  trackingUrl: string | null;
  placedAt: string;
  paidAt: string | null;
  shippedAt: string | null;
  deliveredAt: string | null;
  cancelledAt: string | null;
  timeline: { type: string; message: string; at: string }[];
  payment: PaymentInfo | null;
};
