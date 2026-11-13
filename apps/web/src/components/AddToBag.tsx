"use client";

import Link from "next/link";
import { useState, useTransition } from "react";
import { addToCart } from "@/app/(shop)/cart/actions";
import { track } from "@/lib/analytics";

export function AddToBag({ variantId, label, productTitle }: { variantId: number | null; label: string; productTitle: string }) {
  const [pending, startTransition] = useTransition();
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null);

  return (
    <div>
      <button
        type="button"
        disabled={!variantId || pending}
        onClick={() =>
          startTransition(async () => {
            if (!variantId) return;
            const result = await addToCart(variantId, 1);
            if (result.ok) {
              track("add_to_cart", { variantId, quantity: 1 });
              setMessage({ ok: true, text: `${productTitle} is in your bag.` });
            } else {
              setMessage({ ok: false, text: result.message ?? "Couldn't add this item." });
            }
          })
        }
        className="mt-2 w-full bg-stone-900 py-3 text-sm uppercase tracking-widest text-white hover:bg-stone-700 disabled:opacity-50"
      >
        {pending ? "Adding…" : label}
      </button>
      <p role="status" className={`mt-3 min-h-5 text-sm ${message?.ok === false ? "text-red-700" : "text-stone-700"}`}>
        {message?.text}{" "}
        {message?.ok && (
          <Link href="/cart" className="underline">
            View bag
          </Link>
        )}
      </p>
    </div>
  );
}
