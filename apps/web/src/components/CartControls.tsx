"use client";

import { useActionState, useState, useTransition } from "react";
import { applyDiscount, removeFromCart, setQuantity, type CartActionResult } from "@/app/(shop)/cart/actions";
import { track } from "@/lib/analytics";

export function CartLineControls({ variantId, quantity, max }: { variantId: number; quantity: number; max: number }) {
  const [pending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);
  const options = Array.from({ length: Math.max(max, quantity) }, (_, i) => i + 1);
  return (
    <div className="flex items-center gap-4 text-sm">
      <label className="flex items-center gap-2">
        <span className="text-stone-500">Qty</span>
        <select
          value={quantity}
          disabled={pending}
          onChange={(e) =>
            startTransition(async () => {
              const r = await setQuantity(variantId, Number(e.target.value));
              setError(r.ok ? null : (r.message ?? "Couldn't update"));
            })
          }
          className="border border-stone-300 bg-white px-2 py-1"
        >
          {options.map((n) => (
            <option key={n} value={n}>
              {n}
            </option>
          ))}
        </select>
      </label>
      <button
        type="button"
        disabled={pending}
        onClick={() =>
          startTransition(async () => {
            await removeFromCart(variantId);
            track("remove_from_cart", { variantId });
          })
        }
        className="text-stone-500 underline hover:text-stone-900"
      >
        Remove
      </button>
      {error && <span className="text-red-700">{error}</span>}
    </div>
  );
}

export function DiscountForm({ current, problem }: { current: string | null; problem: string | null }) {
  const [result, action, pending] = useActionState<CartActionResult | null, FormData>(applyDiscount, null);
  return (
    <form action={action} className="mt-6">
      <label className="text-sm" htmlFor="discount-code">
        Discount code
      </label>
      <div className="mt-1 flex gap-2">
        <input
          id="discount-code"
          name="code"
          defaultValue={current ?? ""}
          autoCapitalize="characters"
          className="w-full border border-stone-300 px-3 py-2 text-sm uppercase"
        />
        <button disabled={pending} className="border border-stone-900 px-4 text-sm hover:bg-stone-900 hover:text-white">
          Apply
        </button>
      </div>
      {(result?.message || problem) && (
        <p role={result?.ok === false || problem ? "alert" : "status"} className={`mt-2 text-xs ${result?.ok === false || problem ? "text-red-700" : "text-green-800"}`}>
          {result?.message ?? problem}
        </p>
      )}
    </form>
  );
}
