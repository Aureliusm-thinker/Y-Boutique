import type { Metadata } from "next";
import { Suspense } from "react";
import { Loading } from "@/components/admin/ui";
import { PrintButton } from "@/components/PrintButton";
import { adminFetch } from "@/lib/admin/api";
import type { AdminOrderDetail } from "@/lib/admin/types";

export const metadata: Metadata = { title: "Packing slips" };

/** One printable packing slip per order (E5-2). Print → "Save as PDF" for a PDF. */
async function Slips({ searchParams }: Pick<PageProps<"/admin/orders/packing-slips">, "searchParams">) {
  const { ids } = await searchParams;
  const list = (Array.isArray(ids) ? ids : (ids ?? "").split(",")).filter((x) => /^\d+$/.test(x));
  if (list.length === 0) return <p className="text-sm">Select at least one order on the Orders page.</p>;
  const orders = await adminFetch<AdminOrderDetail[]>(`/api/v1/admin/orders/batch?ids=${list.join(",")}`);
  return (
    <>
      <div className="mb-6 flex items-center justify-between print:hidden">
        <p className="text-sm text-stone-600">{orders.length} packing slip{orders.length === 1 ? "" : "s"}</p>
        <PrintButton label="Print" />
      </div>
      {orders.map(({ id, order: o, deliveryNotes }) => (
        <article key={id} className="mb-8 break-after-page border border-stone-300 bg-white p-8 text-sm print:mb-0 print:border-0">
          <div className="flex justify-between">
            <div>
              <p className="font-serif text-3xl">Yvette</p>
              <p className="text-stone-500">Packing slip</p>
            </div>
            <div className="text-right">
              <p className="text-xl font-medium">{o.number}</p>
              <p>{new Date(o.placedAt).toLocaleDateString("en-KE", { dateStyle: "medium" })}</p>
            </div>
          </div>
          <div className="mt-6 grid grid-cols-2 gap-6">
            <div>
              <p className="text-xs uppercase tracking-widest text-stone-500">Deliver to</p>
              <p className="mt-1 text-base">{o.shipTo.name}</p>
              {o.shipTo.line1 ? (
                <p>
                  {o.shipTo.line1}
                  {o.shipTo.line2 && <><br />{o.shipTo.line2}</>}
                  <br />
                  {o.shipTo.city}, {o.shipTo.county}
                </p>
              ) : (
                <p>Collection from the boutique</p>
              )}
              <p>Tel: 0{o.phone.slice(3)}</p>
            </div>
            <div>
              <p className="text-xs uppercase tracking-widest text-stone-500">Delivery</p>
              <p className="mt-1">{o.shippingMethod}</p>
              {deliveryNotes && <p className="mt-2 italic">“{deliveryNotes}”</p>}
            </div>
          </div>
          <table className="mt-6 w-full text-left">
            <thead className="border-b border-stone-400 text-xs uppercase tracking-widest text-stone-500">
              <tr>
                <th className="w-8 py-2">✓</th>
                <th>Item</th>
                <th>SKU</th>
                <th>Size / colour</th>
                <th className="text-right">Qty</th>
              </tr>
            </thead>
            <tbody>
              {o.lines.map((l) => (
                <tr key={l.id} className="border-b border-stone-200">
                  <td className="py-2">☐</td>
                  <td>{l.title}</td>
                  <td>{l.sku}</td>
                  <td>{[l.size, l.colour].filter(Boolean).join(" / ")}</td>
                  <td className="text-right text-base font-medium">{l.quantity}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <p className="mt-8 text-stone-500">Thank you for shopping with Yvette. Returns within 14 days of delivery — see yvette.co.ke/pages/returns.</p>
        </article>
      ))}
    </>
  );
}

export default function PackingSlipsPage({ searchParams }: PageProps<"/admin/orders/packing-slips">) {
  return (
    <Suspense fallback={<Loading />}>
      <Slips searchParams={searchParams} />
    </Suspense>
  );
}
