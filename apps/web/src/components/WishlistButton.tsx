"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState, useTransition } from "react";
import { toggleWishlist, wishlistIds } from "@/app/(shop)/account/actions";
import { track } from "@/lib/analytics";

/** Heart toggle on the product page (FR-CA-05). Guests are sent to sign in first. */
export function WishlistButton({ productId, slug }: { productId: number; slug: string }) {
  const router = useRouter();
  const [saved, setSaved] = useState(false);
  const [pending, start] = useTransition();

  useEffect(() => {
    wishlistIds().then((ids) => setSaved(ids.includes(productId)));
  }, [productId]);

  return (
    <button
      type="button"
      aria-pressed={saved}
      disabled={pending}
      onClick={() =>
        start(async () => {
          const next = !saved;
          const r = await toggleWishlist(productId, next);
          if (!r.ok && r.message === "signin") {
            router.push(`/account/login?next=/products/${slug}`);
            return;
          }
          if (r.ok) {
            setSaved(next);
            track(next ? "wishlist_add" : "wishlist_remove", { productId });
          }
        })
      }
      className="flex items-center gap-2 text-sm text-stone-700 hover:text-stone-950"
    >
      <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden className={saved ? "fill-stone-900" : "fill-none"}>
        <path
          d="M12 21s-7.5-4.6-9.5-9.2C1 8.2 3.4 4.5 7 4.5c2 0 3.5 1.1 5 3 1.5-1.9 3-3 5-3 3.6 0 6 3.7 4.5 7.3C19.5 16.4 12 21 12 21z"
          stroke="currentColor"
          strokeWidth="1.5"
        />
      </svg>
      {saved ? "Saved to wishlist" : "Save to wishlist"}
    </button>
  );
}
