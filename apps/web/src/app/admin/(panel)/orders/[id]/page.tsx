import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { CancelForm, DeliverButton, NoteForm, RefundForm, ShipForm } from "@/components/admin/OrderPanels";
import { Loading, OrderStatusBadge } from "@/components/admin/ui";
import { adminFetch, ApiError, getCurrentStaff } from "@/lib/admin/api";
import { can, type AdminOrderDetail } from "@/lib/admin/types";
import { formatPrice } from "@/lib/format";

export const metadata: Metadata = { title: "Order" };

const when = (iso: string) => new Date(iso).toLocaleString("en-KE", { dateStyle: "medium", timeStyle: "short" });

async function OrderView({ params }: Pick<PageProps<"/admin/orders/[id]">, "params">) {
  const { id } = await params;
  if (!/^\d+$/.test(id)) notFound();
  let detail: AdminOrderDetail;
  try {
    detail = await adminFetch<AdminOrderDetail>(`/api/v1/admin/orders/${id}`);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }
  const me = await getCurrentStaff();
  const o = detail.order;
  const isPickup = o.shippingMethod.toLowerCase().includes("collect");
  const paidStatus = ["PAID", "PARTIALLY_REFUNDED"].includes(o.paymentStatus);

  return (
    <>
      <div className="mb-6">
        <Link href="/admin/orders" className="text-sm text-stone-500 hover:underline">
          ← Orders
        </Link>
        <div className="mt-1 flex flex-wrap items-center gap-3">
          <h1 className="font-serif text-3xl">{o.number}</h1>
          <OrderStatusBadge status={o.status} />
          <span className="text-sm text-stone-500">
            {o.paymentMethod === "MPESA" ? "M-Pesa" : "Card"} · payment {o.paymentStatus.toLowerCase().replace("_", " ")}
          </span>
        </div>
        <p className="mt-1 text-sm text-stone-500">Placed {when(o.placedAt)}</p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="space-y-6">
          <section className="admin-card">
            <h2 className="mb-3 font-medium">Items</h2>
            <ul className="divide-y divide-stone-100 text-sm">
              {o.lines.map((l) => (
                <li key={l.id} className="flex items-center gap-3 py-2">
                  <div className="relative h-14 w-11 shrink-0 bg-stone-100">
                    {l.imageUrl && <Image src={l.imageUrl} alt="" fill sizes="44px" className="object-cover" />}
                  </div>
                  <div className="flex-1">
                    <p>{l.title}</p>
                    <p className="text-xs text-stone-500">
                      {l.sku} · {[l.size, l.colour].filter(Boolean).join(" / ")}
                      {l.returnedQuantity > 0 && ` · ${l.returnedQuantity} returned`}
                    </p>
                  </div>
                  <p className="w-10 text-right">×{l.quantity}</p>
                  <p className="w-24 text-right">{formatPrice(l.lineTotal, o.currency)}</p>
                </li>
              ))}
            </ul>
            <dl className="ml-auto mt-3 max-w-xs space-y-1 text-sm">
              <div className="flex justify-between"><dt>Subtotal</dt><dd>{formatPrice(o.subtotal, o.currency)}</dd></div>
              {o.discountTotal > 0 && (
                <div className="flex justify-between"><dt>Discount {o.discountCode}</dt><dd>−{formatPrice(o.discountTotal, o.currency)}</dd></div>
              )}
              <div className="flex justify-between"><dt>Delivery</dt><dd>{formatPrice(o.shippingTotal, o.currency)}</dd></div>
              <div className="flex justify-between font-medium"><dt>Total</dt><dd>{formatPrice(o.total, o.currency)}</dd></div>
              {o.refundedTotal > 0 && (
                <div className="flex justify-between text-red-700"><dt>Refunded</dt><dd>−{formatPrice(o.refundedTotal, o.currency)}</dd></div>
              )}
            </dl>
          </section>

          <section className="admin-card grid gap-4 text-sm sm:grid-cols-2">
            <div>
              <h2 className="mb-1 font-medium">Customer</h2>
              <p>{o.shipTo.name}</p>
              <p className="text-stone-600">{o.email}</p>
              <p className="text-stone-600">0{o.phone.slice(3)}</p>
              {detail.customerId && can.viewCustomers(me.role) && (
                <Link href={`/admin/customers/${detail.customerId}`} className="mt-1 inline-block underline">
                  View customer
                </Link>
              )}
            </div>
            <div>
              <h2 className="mb-1 font-medium">{o.shippingMethod}</h2>
              {o.shipTo.line1 ? (
                <p className="text-stone-600">
                  {o.shipTo.line1}
                  {o.shipTo.line2 && <><br />{o.shipTo.line2}</>}
                  <br />
                  {o.shipTo.city}, {o.shipTo.county}
                  {o.shipTo.postalCode && ` ${o.shipTo.postalCode}`}
                </p>
              ) : (
                <p className="text-stone-600">Collect from the boutique</p>
              )}
              {detail.deliveryNotes && <p className="mt-1 italic text-stone-600">“{detail.deliveryNotes}”</p>}
              {o.trackingNumber && (
                <p className="mt-1">
                  {o.carrier} · {o.trackingNumber}
                </p>
              )}
            </div>
          </section>

          <section className="admin-card">
            <h2 className="mb-3 font-medium">Timeline</h2>
            <ul className="space-y-2 text-sm">
              {[...detail.events].reverse().map((e, i) => (
                <li key={i} className="flex gap-3">
                  <span className="w-36 shrink-0 text-stone-500">{when(e.at)}</span>
                  <span className={e.type === "ATTENTION" || e.type === "ACTION_NEEDED" ? "font-medium text-red-700" : ""}>
                    {e.message}
                    {e.staffName && <span className="text-stone-500"> — {e.staffName}</span>}
                    {!e.customerVisible && <span className="ml-1 text-xs text-stone-400">(internal)</span>}
                  </span>
                </li>
              ))}
            </ul>
            {can.noteOrders(me.role) && (
              <div className="mt-4 border-t border-stone-100 pt-4">
                <NoteForm id={detail.id} kind="order" />
              </div>
            )}
          </section>

          <section className="admin-card">
            <h2 className="mb-3 font-medium">Payments</h2>
            <table className="w-full text-left text-sm">
              <tbody>
                {detail.payments.map((p, i) => (
                  <tr key={i} className="border-b border-stone-100">
                    <td className="py-1.5">{when(p.at)}</td>
                    <td>{p.kind === "REFUND" ? "Refund" : "Charge"} · {p.provider}</td>
                    <td>{p.status.toLowerCase()}</td>
                    <td className="text-xs text-stone-500">{p.reference ?? p.failureMessage}</td>
                    <td className="text-right">{formatPrice(p.amount, o.currency)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
        </div>

        <aside className="space-y-6 lg:sticky lg:top-6 lg:self-start">
          {can.fulfilOrders(me.role) && o.status === "PAID" && (
            <section className="admin-card">
              <h2 className="mb-3 font-medium">{isPickup ? "Collection" : "Ship this order"}</h2>
              <a href={`/admin/orders/packing-slips?ids=${detail.id}`} className="admin-btn mb-4 block text-center">
                Print packing slip
              </a>
              {isPickup ? <DeliverButton orderId={detail.id} label="Mark as collected" /> : <ShipForm orderId={detail.id} />}
            </section>
          )}
          {can.fulfilOrders(me.role) && o.status === "SHIPPED" && (
            <section className="admin-card">
              <h2 className="mb-3 font-medium">Delivery</h2>
              <DeliverButton orderId={detail.id} label="Mark as delivered" />
            </section>
          )}
          {can.refund(me.role) && paidStatus && detail.refundable > 0 && (
            <section className="admin-card">
              <h2 className="mb-3 font-medium">Refund</h2>
              <RefundForm detail={detail} />
            </section>
          )}
          {can.refund(me.role) && (o.status === "PENDING_PAYMENT" || o.status === "PAID") && (
            <section className="admin-card">
              <h2 className="mb-3 font-medium">Cancel order</h2>
              <CancelForm orderId={detail.id} paid={o.status === "PAID"} />
            </section>
          )}
        </aside>
      </div>
    </>
  );
}

export default function AdminOrderPage({ params }: PageProps<"/admin/orders/[id]">) {
  return (
    <Suspense fallback={<Loading />}>
      <OrderView params={params} />
    </Suspense>
  );
}
