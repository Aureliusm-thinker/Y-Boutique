import Link from "next/link";
import { getCart } from "@/lib/cart";

/** Header bag link with item count (reads the cart cookie, so render it inside <Suspense>). */
export async function BagLink() {
  const cart = await getCart().catch(() => null);
  const count = cart?.itemCount ?? 0;
  return (
    <Link href="/cart" className="hover:text-stone-950" aria-label={`Bag, ${count} ${count === 1 ? "item" : "items"}`}>
      Bag{count > 0 && <span className="ml-1 rounded-full bg-stone-900 px-1.5 py-0.5 text-xs text-white">{count}</span>}
    </Link>
  );
}
