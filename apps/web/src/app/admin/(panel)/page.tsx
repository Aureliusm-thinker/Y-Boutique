import Link from "next/link";
import { Suspense } from "react";
import { Loading, PageTitle } from "@/components/admin/ui";
import { adminFetch, getCurrentStaff } from "@/lib/admin/api";
import { can, type AdminProductSummary, type LowStockRow } from "@/lib/admin/types";
import type { Page } from "@/lib/types";

async function countByStatus(status: string) {
  const page = await adminFetch<Page<AdminProductSummary>>(`/api/v1/admin/products?status=${status}&size=1`);
  return page.totalItems;
}

async function Dashboard() {
  const me = await getCurrentStaff();
  if (!can.viewCatalog(me.role)) {
    return <p className="text-stone-600">Welcome, {me.fullName}. Your tools will appear here as they are built.</p>;
  }
  const [live, drafts, archived, lowStock] = await Promise.all([
    countByStatus("ACTIVE"),
    countByStatus("DRAFT"),
    countByStatus("ARCHIVED"),
    adminFetch<LowStockRow[]>("/api/v1/admin/inventory/low-stock"),
  ]);

  const tiles = [
    { label: "Live products", value: live, href: "/admin/products?status=ACTIVE" },
    { label: "Drafts", value: drafts, href: "/admin/products?status=DRAFT" },
    { label: "Archived", value: archived, href: "/admin/products?status=ARCHIVED" },
    { label: "Low-stock variants", value: lowStock.length, href: "/admin/inventory" },
  ];

  return (
    <>
      <p className="mb-6 text-stone-600">Welcome back, {me.fullName.split(" ")[0]}.</p>
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        {tiles.map((t) => (
          <Link key={t.label} href={t.href} className="admin-card hover:border-stone-400">
            <p className="text-sm text-stone-500">{t.label}</p>
            <p className="mt-1 text-3xl tabular-nums">{t.value}</p>
          </Link>
        ))}
      </div>

      <section className="admin-card mt-8">
        <h2 className="mb-3 font-medium">Running low</h2>
        {lowStock.length === 0 ? (
          <p className="text-sm text-stone-500">Nothing is running low.</p>
        ) : (
          <ul className="divide-y divide-stone-100 text-sm">
            {lowStock.slice(0, 8).map((r) => (
              <li key={r.variantId} className="flex justify-between py-2">
                <Link href={`/admin/products/${r.productId}`} className="hover:underline">
                  {r.productTitle} <span className="text-stone-500">· {[r.size, r.colour].filter(Boolean).join(" / ") || r.sku}</span>
                </Link>
                <span className={r.onHand - r.reserved <= 0 ? "text-red-700" : "text-amber-700"}>
                  {r.onHand - r.reserved} left
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}

export default function AdminHome() {
  return (
    <>
      <PageTitle>Dashboard</PageTitle>
      <Suspense fallback={<Loading />}>
        <Dashboard />
      </Suspense>
    </>
  );
}
