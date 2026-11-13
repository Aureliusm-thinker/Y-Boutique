import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { getOrder } from "@/app/(shop)/cart/actions";
import { PrintButton } from "@/components/PrintButton";
import { formatPrice } from "@/lib/format";

export const metadata: Metadata = { title: "Invoice", robots: { index: false } };

const date = (iso: string) => new Intl.DateTimeFormat("en-KE", { dateStyle: "long", timeZone: "Africa/Nairobi" }).format(new Date(iso));

/** Printable invoice / receipt (FR-CA-04). "Print" → "Save as PDF" gives the PDF. */
async function Invoice({ params, searchParams }: PageProps<"/order/[number]/invoice">) {
  const [{ number }, { t }] = await Promise.all([params, searchParams]);
  if (typeof t !== "string") notFound();
  const order = await getOrder(number, t);
  if (!order || order.paymentStatus === "PENDING" || order.paymentStatus === "FAILED") notFound();

  return (
    <article className="bg-white p-8 text-sm print:p-0">
      <div className="flex items-start justify-between">
        <div>
          <p className="font-serif text-3xl">Yvette</p>
          <p className="mt-1 text-stone-500">Modern boutique · Nairobi, Kenya</p>
        </div>
        <div className="text-right">
          <p className="text-lg">Invoice</p>
          <p>{order.number}</p>
          <p className="text-stone-500">{date(order.paidAt ?? order.placedAt)}</p>
        </div>
      </div>

      <div className="mt-8 grid grid-cols-2 gap-6">
        <div>
          <p className="text-xs uppercase tracking-widest text-stone-500">Billed to</p>
          <p className="mt-1">{order.shipTo.name}</p>
          <p>{order.email}</p>
          <p>0{order.phone.slice(3)}</p>
        </div>
        {order.shipTo.line1 && (
          <div>
            <p className="text-xs uppercase tracking-widest text-stone-500">Delivered to</p>
            <p className="mt-1">{order.shipTo.line1}</p>
            {order.shipTo.line2 && <p>{order.shipTo.line2}</p>}
            <p>
              {order.shipTo.city}, {order.shipTo.county}
            </p>
          </div>
        )}
      </div>

      <table className="mt-8 w-full text-left">
        <thead className="border-b border-stone-300 text-xs uppercase tracking-widest text-stone-500">
          <tr>
            <th className="py-2">Item</th>
            <th>SKU</th>
            <th className="text-right">Qty</th>
            <th className="text-right">Unit price</th>
            <th className="text-right">Amount</th>
          </tr>
        </thead>
        <tbody>
          {order.lines.map((l) => (
            <tr key={l.id} className="border-b border-stone-100">
              <td className="py-2">
                {l.title} {[l.size, l.colour].filter(Boolean).length > 0 && <span className="text-stone-500">({[l.size, l.colour].filter(Boolean).join(", ")})</span>}
              </td>
              <td>{l.sku}</td>
              <td className="text-right">{l.quantity}</td>
              <td className="text-right">{formatPrice(l.unitPrice, order.currency)}</td>
              <td className="text-right">{formatPrice(l.lineTotal, order.currency)}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <dl className="ml-auto mt-4 w-64 space-y-1">
        <div className="flex justify-between"><dt>Subtotal</dt><dd>{formatPrice(order.subtotal, order.currency)}</dd></div>
        {order.discountTotal > 0 && (
          <div className="flex justify-between"><dt>Discount</dt><dd>−{formatPrice(order.discountTotal, order.currency)}</dd></div>
        )}
        <div className="flex justify-between"><dt>Delivery</dt><dd>{formatPrice(order.shippingTotal, order.currency)}</dd></div>
        <div className="flex justify-between border-t border-stone-300 pt-1 font-medium"><dt>Total paid</dt><dd>{formatPrice(order.total, order.currency)}</dd></div>
        {order.refundedTotal > 0 && (
          <div className="flex justify-between"><dt>Refunded</dt><dd>−{formatPrice(order.refundedTotal, order.currency)}</dd></div>
        )}
      </dl>
      <p className="mt-8 text-xs text-stone-500">
        Paid by {order.paymentMethod === "MPESA" ? "M-Pesa" : "card"}. Prices include VAT where applicable.
      </p>
      <div className="mt-8 print:hidden">
        <PrintButton />
      </div>
    </article>
  );
}

export default function InvoicePage(props: PageProps<"/order/[number]/invoice">) {
  return (
    <div className="mx-auto max-w-3xl px-4 py-12 print:max-w-none print:py-0">
      <Suspense fallback={<div className="h-96 animate-pulse bg-stone-100" />}>
        <Invoice {...props} />
      </Suspense>
    </div>
  );
}
