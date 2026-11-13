import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { getOrder } from "@/app/(shop)/cart/actions";
import { PaymentPanel } from "@/components/PaymentPanel";

export const metadata: Metadata = { title: "Payment", robots: { index: false } };

async function Payment({ params, searchParams }: PageProps<"/checkout/pay/[number]">) {
  const [{ number }, { t }] = await Promise.all([params, searchParams]);
  if (typeof t !== "string") notFound();
  const order = await getOrder(number, t);
  if (!order) notFound();
  return <PaymentPanel initial={order} />;
}

export default function PayPage(props: PageProps<"/checkout/pay/[number]">) {
  return (
    <div className="mx-auto max-w-lg px-4 py-12">
      <h1 className="mb-6 font-serif text-4xl">Payment</h1>
      <Suspense fallback={<div className="h-64 animate-pulse bg-stone-100" />}>
        <Payment {...props} />
      </Suspense>
    </div>
  );
}
