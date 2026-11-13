import Image from "next/image";
import { formatPrice } from "@/lib/format";
import type { Order } from "@/lib/types";

const STEPS = [
  { key: "PLACED", label: "Placed" },
  { key: "PAID", label: "Paid" },
  { key: "SHIPPED", label: "On its way" },
  { key: "DELIVERED", label: "Delivered" },
] as const;

function reached(order: Order): number {
  if (order.status === "DELIVERED") return 3;
  if (order.status === "SHIPPED") return 2;
  if (order.paymentStatus !== "PENDING" && order.paymentStatus !== "FAILED") return 1;
  return 0;
}

export const statusLabel: Record<string, string> = {
  PENDING_PAYMENT: "Awaiting payment",
  PAID: "Preparing your order",
  SHIPPED: "On its way",
  DELIVERED: "Delivered",
  CANCELLED: "Cancelled",
};

const dateTime = (iso: string) =>
  new Intl.DateTimeFormat("en-KE", { dateStyle: "medium", timeStyle: "short", timeZone: "Africa/Nairobi" }).format(new Date(iso));

/** Order status timeline (FR-CA-04), items, totals and delivery details. */
export function OrderDetails({ order }: { order: Order }) {
  const step = reached(order);
  return (
    <div className="space-y-10">
      {order.status !== "CANCELLED" ? (
        <ol className="grid grid-cols-4 gap-2 text-center text-xs" aria-label="Order progress">
          {STEPS.map((s, i) => (
            <li key={s.key} aria-current={i === step ? "step" : undefined}>
              <div className={`h-1 ${i <= step ? "bg-stone-900" : "bg-stone-200"}`} />
              <p className={`mt-2 ${i <= step ? "text-stone-900" : "text-stone-400"}`}>{s.label}</p>
            </li>
          ))}
        </ol>
      ) : (
        <p className="border border-stone-300 bg-stone-50 p-4 text-sm">This order was cancelled.</p>
      )}

      {(order.trackingNumber || order.trackingUrl) && (
        <div className="border border-stone-200 bg-white p-4 text-sm">
          <p>
            {order.carrier ?? "Courier"}
            {order.trackingNumber && <> · Tracking number <strong>{order.trackingNumber}</strong></>}
          </p>
          {order.trackingUrl && (
            <a href={order.trackingUrl} target="_blank" rel="noopener noreferrer" className="mt-2 inline-block underline">
              Track your parcel
            </a>
          )}
        </div>
      )}

      <section>
        <h2 className="mb-4 font-serif text-2xl">Items</h2>
        <ul className="divide-y divide-stone-200 border-y border-stone-200">
          {order.lines.map((l) => (
            <li key={l.id} className="flex gap-4 py-4 text-sm">
              <div className="relative h-24 w-20 shrink-0 bg-stone-100">
                {l.imageUrl && <Image src={l.imageUrl} alt="" fill sizes="80px" className="object-cover" />}
              </div>
              <div className="flex-1">
                <p>{l.title}</p>
                <p className="text-stone-500">
                  {[l.size, l.colour].filter(Boolean).join(" · ")} · Qty {l.quantity}
                </p>
                {l.returnedQuantity > 0 && <p className="text-stone-500">{l.returnedQuantity} returned</p>}
              </div>
              <p>{formatPrice(l.lineTotal, order.currency)}</p>
            </li>
          ))}
        </ul>
        <dl className="ml-auto mt-4 max-w-xs space-y-1 text-sm">
          <div className="flex justify-between">
            <dt>Subtotal</dt>
            <dd>{formatPrice(order.subtotal, order.currency)}</dd>
          </div>
          {order.discountTotal > 0 && (
            <div className="flex justify-between text-green-800">
              <dt>Discount {order.discountCode && `(${order.discountCode})`}</dt>
              <dd>−{formatPrice(order.discountTotal, order.currency)}</dd>
            </div>
          )}
          <div className="flex justify-between">
            <dt>Delivery</dt>
            <dd>{order.shippingTotal === 0 ? "Free" : formatPrice(order.shippingTotal, order.currency)}</dd>
          </div>
          <div className="flex justify-between border-t border-stone-200 pt-1 text-base">
            <dt>Total</dt>
            <dd>{formatPrice(order.total, order.currency)}</dd>
          </div>
          {order.refundedTotal > 0 && (
            <div className="flex justify-between text-stone-600">
              <dt>Refunded</dt>
              <dd>{formatPrice(order.refundedTotal, order.currency)}</dd>
            </div>
          )}
        </dl>
      </section>

      <section className="grid gap-6 text-sm sm:grid-cols-3">
        <div>
          <h3 className="mb-1 text-xs uppercase tracking-widest text-stone-500">Delivery</h3>
          <p>{order.shippingMethod}</p>
          {order.shipTo.line1 && (
            <p className="text-stone-600">
              {order.shipTo.name}
              <br />
              {order.shipTo.line1}
              {order.shipTo.line2 && (
                <>
                  <br />
                  {order.shipTo.line2}
                </>
              )}
              <br />
              {order.shipTo.city}, {order.shipTo.county}
            </p>
          )}
        </div>
        <div>
          <h3 className="mb-1 text-xs uppercase tracking-widest text-stone-500">Contact</h3>
          <p className="break-all text-stone-600">{order.email}</p>
          <p className="text-stone-600">0{order.phone.slice(3)}</p>
        </div>
        <div>
          <h3 className="mb-1 text-xs uppercase tracking-widest text-stone-500">Payment</h3>
          <p className="text-stone-600">{order.paymentMethod === "MPESA" ? "M-Pesa" : "Card"}</p>
          <p className="text-stone-600">Placed {dateTime(order.placedAt)}</p>
        </div>
      </section>

      {order.timeline.length > 0 && (
        <section>
          <h2 className="mb-3 font-serif text-2xl">History</h2>
          <ul className="space-y-2 text-sm">
            {[...order.timeline].reverse().map((e, i) => (
              <li key={i} className="flex gap-4">
                <span className="w-40 shrink-0 text-stone-500">{dateTime(e.at)}</span>
                <span>{e.message}</span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}
