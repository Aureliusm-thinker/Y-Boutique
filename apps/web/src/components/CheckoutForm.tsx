"use client";

import { useActionState, useState } from "react";
import { placeOrder, type CheckoutState } from "@/app/(shop)/cart/actions";
import { formatPrice } from "@/lib/format";
import type { Cart, CheckoutOptions } from "@/lib/types";

export type CheckoutDefaults = Partial<{
  email: string;
  phone: string;
  fullName: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  county: string;
  postalCode: string;
}>;

const input = "mt-1 w-full border border-stone-300 bg-white px-3 py-2 text-sm focus:border-stone-900 focus:outline-none";

export function CheckoutForm({
  cart,
  options,
  defaults,
}: {
  cart: Cart;
  options: CheckoutOptions;
  defaults: CheckoutDefaults;
}) {
  const [state, action, pending] = useActionState<CheckoutState, FormData>(placeOrder, { ok: true });
  const v: Record<string, string | undefined> = { ...defaults, ...state.values };
  const [shipping, setShipping] = useState(v.shippingMethod ?? cart.shippingOptions[0]?.code ?? "");
  const [payment, setPayment] = useState<"MPESA" | "CARD">(
    (v.paymentMethod as "MPESA" | "CARD") ?? options.paymentMethods[0] ?? "MPESA",
  );
  const method = cart.shippingOptions.find((o) => o.code === shipping);
  const total = cart.totalBeforeShipping + (method?.price ?? 0);

  return (
    <form action={action} className="grid gap-10 lg:grid-cols-[1fr_22rem]">
      <div className="space-y-10">
        <fieldset className="space-y-4">
          <legend className="mb-2 font-serif text-2xl">Contact</legend>
          <label className="block text-sm">
            Email
            <input name="email" type="email" required autoComplete="email" defaultValue={v.email} className={input} />
          </label>
          <label className="block text-sm">
            Mobile number
            <input
              name="phone"
              type="tel"
              required
              autoComplete="tel"
              placeholder="0712 345 678"
              defaultValue={v.phone}
              className={input}
            />
            <span className="text-xs text-stone-500">For delivery updates and M-Pesa.</span>
          </label>
          <label className="block text-sm">
            Full name
            <input name="fullName" required autoComplete="name" defaultValue={v.fullName} className={input} />
          </label>
        </fieldset>

        <fieldset>
          <legend className="mb-4 font-serif text-2xl">Delivery</legend>
          <div className="space-y-2">
            {cart.shippingOptions.map((o) => (
              <label
                key={o.code}
                className={`flex cursor-pointer items-start justify-between gap-4 border p-4 text-sm ${shipping === o.code ? "border-stone-900" : "border-stone-300"}`}
              >
                <span className="flex gap-3">
                  <input
                    type="radio"
                    name="shippingMethod"
                    value={o.code}
                    checked={shipping === o.code}
                    onChange={() => setShipping(o.code)}
                    className="mt-1"
                  />
                  <span>
                    <span className="block">{o.name}</span>
                    <span className="text-stone-500">{o.description}</span>
                  </span>
                </span>
                <span className="shrink-0">{o.price === 0 ? "Free" : formatPrice(o.price, cart.currency)}</span>
              </label>
            ))}
          </div>

          {method?.needsAddress !== false && (
            <div className="mt-6 grid gap-4 sm:grid-cols-2">
              <label className="block text-sm sm:col-span-2">
                Street address, estate or building
                <input name="addressLine1" required autoComplete="address-line1" defaultValue={v.addressLine1} className={input} />
              </label>
              <label className="block text-sm sm:col-span-2">
                Apartment, house number, floor <span className="text-stone-500">(optional)</span>
                <input name="addressLine2" autoComplete="address-line2" defaultValue={v.addressLine2} className={input} />
              </label>
              <label className="block text-sm">
                Town or city
                <input name="city" required autoComplete="address-level2" defaultValue={v.city} className={input} />
              </label>
              <label className="block text-sm">
                County
                <select name="county" required defaultValue={v.county ?? "Nairobi"} className={input}>
                  {options.counties.map((c) => (
                    <option key={c}>{c}</option>
                  ))}
                </select>
              </label>
              <label className="block text-sm">
                Postal code <span className="text-stone-500">(optional)</span>
                <input name="postalCode" autoComplete="postal-code" defaultValue={v.postalCode} className={input} />
              </label>
            </div>
          )}
          <label className="mt-4 block text-sm">
            Delivery notes <span className="text-stone-500">(optional)</span>
            <textarea
              name="deliveryNotes"
              maxLength={300}
              rows={2}
              placeholder="Gate code, landmark, best time to call…"
              defaultValue={v.deliveryNotes}
              className={input}
            />
          </label>
        </fieldset>

        <fieldset>
          <legend className="mb-4 font-serif text-2xl">Payment</legend>
          {options.paymentMethods.length === 0 && (
            <p className="text-sm text-red-700">Payments are temporarily unavailable. Please try again later.</p>
          )}
          <div className="space-y-2">
            {options.paymentMethods.map((m) => (
              <label
                key={m}
                className={`flex cursor-pointer items-center gap-3 border p-4 text-sm ${payment === m ? "border-stone-900" : "border-stone-300"}`}
              >
                <input type="radio" name="paymentMethod" value={m} checked={payment === m} onChange={() => setPayment(m)} />
                {m === "MPESA" ? (
                  <span>
                    <span className="block">M-Pesa</span>
                    <span className="text-stone-500">You&apos;ll get a prompt on your phone to enter your M-Pesa PIN.</span>
                  </span>
                ) : (
                  <span>
                    <span className="block">Debit or credit card</span>
                    <span className="text-stone-500">Visa or Mastercard, with 3-D Secure.</span>
                  </span>
                )}
              </label>
            ))}
          </div>
          {payment === "MPESA" && (
            <label className="mt-4 block text-sm">
              M-Pesa number <span className="text-stone-500">(if different from your mobile number)</span>
              <input name="mpesaPhone" type="tel" placeholder="07XX XXX XXX" defaultValue={v.mpesaPhone} className={input} />
            </label>
          )}
          {options.testMode && (
            <p className="mt-4 border border-amber-300 bg-amber-50 p-3 text-xs text-amber-900">
              Test mode: no real money moves. You&apos;ll be able to approve the M-Pesa prompt or use the test card
              4242 4242 4242 4242 on the next page.
            </p>
          )}
        </fieldset>
      </div>

      <aside className="h-fit border border-stone-200 bg-white p-6 lg:sticky lg:top-8">
        <h2 className="font-serif text-2xl">Your order</h2>
        <ul className="mt-4 space-y-2 text-sm">
          {cart.lines.map((l) => (
            <li key={l.variantId} className="flex justify-between gap-3">
              <span>
                {l.quantity} × {l.title}
                <span className="block text-xs text-stone-500">{[l.size, l.colour].filter(Boolean).join(" · ")}</span>
              </span>
              <span>{formatPrice(l.lineTotal, cart.currency)}</span>
            </li>
          ))}
        </ul>
        <dl className="mt-4 space-y-2 border-t border-stone-200 pt-4 text-sm">
          <div className="flex justify-between">
            <dt>Subtotal</dt>
            <dd>{formatPrice(cart.subtotal, cart.currency)}</dd>
          </div>
          {cart.discount && cart.discount.amount > 0 && (
            <div className="flex justify-between text-green-800">
              <dt>Discount {cart.discount.code && `(${cart.discount.code})`}</dt>
              <dd>−{formatPrice(cart.discount.amount, cart.currency)}</dd>
            </div>
          )}
          <div className="flex justify-between">
            <dt>Delivery</dt>
            <dd>{method ? (method.price === 0 ? "Free" : formatPrice(method.price, cart.currency)) : "—"}</dd>
          </div>
          <div className="flex justify-between border-t border-stone-200 pt-2 text-base">
            <dt>Total</dt>
            <dd>{formatPrice(total, cart.currency)}</dd>
          </div>
        </dl>

        {!state.ok && state.message && (
          <div role="alert" className="mt-4 border border-red-200 bg-red-50 p-3 text-sm text-red-800">
            {state.message}
            {state.errors && state.errors.length > 0 && (
              <ul className="mt-1 list-disc pl-5">
                {state.errors.map((e) => (
                  <li key={e}>{e}</li>
                ))}
              </ul>
            )}
          </div>
        )}
        <button
          disabled={pending || options.paymentMethods.length === 0}
          className="mt-6 w-full bg-stone-900 py-3 text-sm uppercase tracking-widest text-white hover:bg-stone-700 disabled:opacity-50"
        >
          {pending ? "Placing order…" : `Pay ${formatPrice(total, cart.currency)}`}
        </button>
        <p className="mt-3 text-center text-xs text-stone-500">
          By placing your order you agree to our <a href="/pages/terms" className="underline">terms</a> and{" "}
          <a href="/pages/returns" className="underline">returns policy</a>.
        </p>
      </aside>
    </form>
  );
}
