import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { Loading, PageTitle } from "@/components/admin/ui";
import { adminFetch } from "@/lib/admin/api";
import type { LowStockRow } from "@/lib/admin/types";

export const metadata: Metadata = { title: "Inventory" };

async function LowStock() {
  const rows = await adminFetch<LowStockRow[]>("/api/v1/admin/inventory/low-stock");
  return (
    <section className="admin-card overflow-x-auto p-0">
      <div className="p-5 pb-3">
        <h2 className="font-medium">Low stock ({rows.length})</h2>
        <p className="text-sm text-stone-500">
          Variants at or below their &ldquo;low at&rdquo; level. Open a product to record a delivery or correction.
        </p>
      </div>
      <table className="w-full text-left text-sm">
        <thead className="border-y border-stone-200 text-stone-500">
          <tr>
            <th className="px-5 py-2 font-normal">Product</th>
            <th className="px-5 py-2 font-normal">Variant</th>
            <th className="px-5 py-2 font-normal">SKU</th>
            <th className="px-5 py-2 text-right font-normal">Available</th>
            <th className="px-5 py-2 text-right font-normal">Low at</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-stone-100">
          {rows.map((r) => {
            const available = r.onHand - r.reserved;
            return (
              <tr key={r.variantId}>
                <td className="px-5 py-2">
                  <Link href={`/admin/products/${r.productId}`} className="hover:underline">
                    {r.productTitle}
                  </Link>
                </td>
                <td className="px-5 py-2">{[r.size, r.colour].filter(Boolean).join(" / ") || "—"}</td>
                <td className="px-5 py-2 font-mono text-xs">{r.sku}</td>
                <td className={`px-5 py-2 text-right tabular-nums ${available <= 0 ? "text-red-700" : "text-amber-700"}`}>
                  {available <= 0 ? "Sold out" : available}
                </td>
                <td className="px-5 py-2 text-right tabular-nums">{r.lowStockThreshold}</td>
              </tr>
            );
          })}
          {rows.length === 0 && (
            <tr>
              <td colSpan={5} className="px-5 py-8 text-center text-stone-500">
                Everything is well stocked.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </section>
  );
}

export default function InventoryPage() {
  return (
    <>
      <PageTitle>Inventory</PageTitle>
      <Suspense fallback={<Loading />}>
        <LowStock />
      </Suspense>
    </>
  );
}
