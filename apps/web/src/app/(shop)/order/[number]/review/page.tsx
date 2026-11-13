import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { getOrder } from "@/app/(shop)/cart/actions";
import { getReviewedProductIds } from "@/app/(shop)/order/actions";
import { ReviewForm } from "@/components/OrderFeedbackForms";

export const metadata: Metadata = { title: "Review your items", robots: { index: false } };

async function Content({ params, searchParams }: PageProps<"/order/[number]/review">) {
  const [{ number }, { t }] = await Promise.all([params, searchParams]);
  if (typeof t !== "string") notFound();
  const [order, reviewed] = await Promise.all([getOrder(number, t), getReviewedProductIds(number, t)]);
  if (!order) notFound();
  // One review per product, even if several sizes were bought.
  const products = [...new Map(order.lines.filter((l) => l.productId).map((l) => [l.productId!, l])).values()];

  return (
    <>
      <Link href={`/order/${order.number}?t=${order.accessToken}`} className="text-sm text-stone-500 hover:underline">
        ← Order {order.number}
      </Link>
      <h1 className="mb-2 mt-1 font-serif text-4xl">Review your items</h1>
      <p className="mb-8 text-sm text-stone-600">Your review helps others choose — especially how the size fits.</p>
      {order.status !== "DELIVERED" ? (
        <p className="text-sm">You can review your items once they&apos;ve been delivered.</p>
      ) : (
        <ul className="space-y-10">
          {products.map((l) => (
            <li key={l.productId} className="flex gap-6 border-b border-stone-200 pb-10">
              <div className="relative h-32 w-24 shrink-0 bg-stone-100">
                {l.imageUrl && <Image src={l.imageUrl} alt="" fill sizes="96px" className="object-cover" />}
              </div>
              <div className="flex-1">
                <p className="mb-3 font-medium">{l.title}</p>
                {reviewed.includes(l.productId!) ? (
                  <p className="text-sm text-stone-600">You&apos;ve reviewed this — thank you!</p>
                ) : (
                  <ReviewForm number={order.number} token={order.accessToken} productId={l.productId!} title={l.title} />
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}

export default function ReviewPage(props: PageProps<"/order/[number]/review">) {
  return (
    <div className="mx-auto max-w-3xl px-4 py-12">
      <Suspense fallback={<div className="h-64 animate-pulse bg-stone-100" />}>
        <Content {...props} />
      </Suspense>
    </div>
  );
}
