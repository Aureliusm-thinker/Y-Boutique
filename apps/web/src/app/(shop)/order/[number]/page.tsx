import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { getOrder } from "@/app/(shop)/cart/actions";
import { OrderActions } from "@/components/OrderActions";
import { OrderDetails, statusLabel } from "@/components/OrderDetails";
import { getCustomerProfile } from "@/lib/account";

export const metadata: Metadata = { title: "Your order", robots: { index: false } };

async function OrderContent({ params, searchParams }: PageProps<"/order/[number]">) {
  const [{ number }, { t }] = await Promise.all([params, searchParams]);
  if (typeof t !== "string") notFound();
  const [order, profile] = await Promise.all([getOrder(number, t), getCustomerProfile()]);
  if (!order) notFound();

  const justPaid = order.status === "PAID" && order.timeline.length <= 2;
  return (
    <>
      <p className="text-sm text-stone-500">Order {order.number}</p>
      <h1 className="mt-1 font-serif text-4xl">
        {justPaid ? "Thank you for your order" : statusLabel[order.status]}
      </h1>
      {order.status === "PENDING_PAYMENT" && (
        <p className="mt-4">
          <Link href={`/checkout/pay/${order.number}?t=${order.accessToken}`} className="underline">
            Complete your payment
          </Link>
        </p>
      )}
      {justPaid && (
        <p className="mt-3 text-stone-600">
          We&apos;ve emailed a confirmation to {order.email}. We&apos;ll let you know when it&apos;s on its way.
        </p>
      )}

      {!profile && order.paymentStatus !== "PENDING" && (
        <div className="mt-8 border border-stone-200 bg-white p-5 text-sm">
          <p className="font-medium">Save your details for next time</p>
          <p className="mt-1 text-stone-600">
            Create an account to track this order, check out faster and save a wishlist.
          </p>
          <Link
            href={`/account/register?email=${encodeURIComponent(order.email)}`}
            className="mt-3 inline-block bg-stone-900 px-5 py-2 text-xs uppercase tracking-widest text-white"
          >
            Create an account
          </Link>
        </div>
      )}

      <div className="mt-10">
        <OrderDetails order={order} />
      </div>
      <OrderActions order={order} />
    </>
  );
}

export default function OrderPage(props: PageProps<"/order/[number]">) {
  return (
    <div className="mx-auto max-w-3xl px-4 py-12">
      <Suspense fallback={<div className="h-96 animate-pulse bg-stone-100" />}>
        <OrderContent {...props} />
      </Suspense>
    </div>
  );
}
