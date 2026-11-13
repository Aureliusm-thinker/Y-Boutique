import Link from "next/link";
import type { Order } from "@/lib/types";

/** What the customer can do with an order: invoice now; returns and reviews once delivered. */
export function OrderActions({ order }: { order: Order }) {
  const q = `?t=${order.accessToken}`;
  const paid = order.paymentStatus !== "PENDING" && order.paymentStatus !== "FAILED";
  if (!paid) return null;
  return (
    <div className="mt-10 flex flex-wrap gap-3 border-t border-stone-200 pt-6 text-sm">
      <Link href={`/order/${order.number}/invoice${q}`} className="border border-stone-300 px-4 py-2 hover:border-stone-900">
        Invoice (print or save as PDF)
      </Link>
      {order.status === "DELIVERED" && (
        <>
          <Link href={`/order/${order.number}/return${q}`} className="border border-stone-300 px-4 py-2 hover:border-stone-900">
            Return an item
          </Link>
          <Link href={`/order/${order.number}/review${q}`} className="border border-stone-300 px-4 py-2 hover:border-stone-900">
            Review your items
          </Link>
        </>
      )}
    </div>
  );
}
