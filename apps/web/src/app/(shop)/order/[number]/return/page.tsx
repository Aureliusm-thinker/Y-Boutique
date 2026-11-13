import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { getOrder } from "@/app/(shop)/cart/actions";
import { getReturns } from "@/app/(shop)/order/actions";
import { ReturnForm } from "@/components/OrderFeedbackForms";

export const metadata: Metadata = { title: "Return items", robots: { index: false } };

const STATUS: Record<string, string> = {
  REQUESTED: "Requested — we'll reply within one working day",
  APPROVED: "Approved — please send the items back",
  REJECTED: "Not accepted",
  RECEIVED: "Received — your refund is on its way",
  REFUNDED: "Refunded",
};

async function Content({ params, searchParams }: PageProps<"/order/[number]/return">) {
  const [{ number }, { t }] = await Promise.all([params, searchParams]);
  if (typeof t !== "string") notFound();
  const [order, returns] = await Promise.all([getOrder(number, t), getReturns(number, t)]);
  if (!order) notFound();
  const back = (
    <Link href={`/order/${order.number}?t=${order.accessToken}`} className="text-sm text-stone-500 hover:underline">
      ← Order {order.number}
    </Link>
  );
  const open = new Map<number, number>();
  for (const r of returns.filter((r) => r.status === "REQUESTED" || r.status === "APPROVED")) {
    for (const l of r.lines) open.set(l.orderLineId, (open.get(l.orderLineId) ?? 0) + l.quantity);
  }
  const lines = order.lines.map((l) => ({ ...l, returnable: Math.max(0, l.quantity - l.returnedQuantity - (open.get(l.id) ?? 0)) }));
  const deliveredAt = order.deliveredAt ? new Date(order.deliveredAt) : null;
  const deadline = deliveredAt ? new Date(deliveredAt.getTime() + 14 * 24 * 3600 * 1000) : null;
  const canReturn = order.status === "DELIVERED" && deadline !== null && deadline > new Date();

  return (
    <>
      {back}
      <h1 className="mb-2 mt-1 font-serif text-4xl">Return items</h1>
      {returns.length > 0 && (
        <section className="my-8 space-y-3">
          {returns.map((r) => (
            <div key={r.id} className="border border-stone-200 bg-white p-4 text-sm">
              <p className="font-medium">{STATUS[r.status]}</p>
              <p className="text-stone-600">{r.lines.map((l) => `${l.quantity} × ${l.title}`).join(", ")}</p>
              {r.staffComment && <p className="mt-1 italic text-stone-600">“{r.staffComment}”</p>}
            </div>
          ))}
        </section>
      )}
      {canReturn ? (
        <>
          <p className="mb-6 text-sm text-stone-600">
            You can return items until {deadline!.toLocaleDateString("en-KE", { dateStyle: "long" })}. Items should be
            unworn with tags attached. <Link href="/pages/returns" className="underline">Returns policy</Link>
          </p>
          <ReturnForm number={order.number} token={order.accessToken} lines={lines} />
        </>
      ) : (
        <p className="text-sm text-stone-600">
          {order.status !== "DELIVERED"
            ? "Returns open once your order has been delivered."
            : "The 14-day return window for this order has ended. Contact us if there's a problem with an item."}
        </p>
      )}
    </>
  );
}

export default function ReturnPage(props: PageProps<"/order/[number]/return">) {
  return (
    <div className="mx-auto max-w-3xl px-4 py-12">
      <Suspense fallback={<div className="h-64 animate-pulse bg-stone-100" />}>
        <Content {...props} />
      </Suspense>
    </div>
  );
}
