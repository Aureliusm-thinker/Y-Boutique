"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState, useTransition } from "react";
import { getOrder, payWithTestCard, retryPayment, simulateMpesa } from "@/app/(shop)/cart/actions";
import { formatPrice } from "@/lib/format";
import type { Order } from "@/lib/types";

const displayPhone = (p: string | null) => (p ? `0${p.slice(3, 6)} ${p.slice(6, 9)} ${p.slice(9)}` : "your phone");

/** Waits for / collects payment for an order, then moves on to the confirmation page. */
export function PaymentPanel({ initial }: { initial: Order }) {
  const router = useRouter();
  const [order, setOrder] = useState(initial);
  const [error, setError] = useState<string | null>(null);
  const [pending, startTransition] = useTransition();
  const confirmation = `/order/${order.number}?t=${order.accessToken}`;
  const payment = order.payment;
  const paid = order.paymentStatus === "PAID";
  const waiting = order.status === "PENDING_PAYMENT" && payment?.status === "PENDING";

  // Poll while waiting for M-Pesa / Stripe to report back.
  useEffect(() => {
    if (paid) {
      router.replace(confirmation);
      // The bag is emptied once paid: refresh the header's bag count too.
      router.refresh();
      return;
    }
    if (!waiting || payment?.nextAction === "ENTER_TEST_CARD") return;
    const timer = setInterval(async () => {
      const latest = await getOrder(order.number, order.accessToken);
      if (latest) setOrder(latest);
    }, 3000);
    return () => clearInterval(timer);
  }, [paid, waiting, payment?.nextAction, order.number, order.accessToken, router, confirmation]);

  const reload = async () => {
    const latest = await getOrder(order.number, order.accessToken);
    if (latest) setOrder(latest);
  };

  const run = (fn: () => Promise<{ ok: boolean; message?: string; redirectUrl?: string }>) =>
    startTransition(async () => {
      setError(null);
      const r = await fn();
      if (!r.ok) setError(r.message ?? "Something went wrong.");
      if (r.redirectUrl) window.location.href = r.redirectUrl;
      else await reload();
    });

  if (order.status === "CANCELLED") {
    return (
      <div className="border border-stone-200 bg-white p-6">
        <p>This order was cancelled because payment wasn&apos;t completed in time. Nothing was charged.</p>
        <a href="/cart" className="mt-4 inline-block underline">
          Back to your bag
        </a>
      </div>
    );
  }

  if (paid) return <p role="status">Payment received — taking you to your order…</p>;

  return (
    <div className="border border-stone-200 bg-white p-6">
      <p className="text-sm text-stone-500">Order {order.number}</p>
      <p className="mt-1 text-2xl">{formatPrice(order.total, order.currency)}</p>

      {waiting && payment?.nextAction === "WAIT_FOR_PHONE" && (
        <div className="mt-6" role="status" aria-live="polite">
          <p className="text-lg">Check your phone</p>
          <p className="mt-1 text-sm text-stone-600">
            We&apos;ve sent an M-Pesa request to <strong>{displayPhone(payment.phone)}</strong>. Enter your M-Pesa PIN to
            pay. This page updates by itself.
          </p>
          <div className="mt-4 h-1 w-full overflow-hidden bg-stone-100">
            <div className="h-full w-1/3 animate-pulse bg-stone-900" />
          </div>
          {payment.testMode && (
            <div className="mt-6 border border-amber-300 bg-amber-50 p-4 text-sm">
              <p className="text-amber-900">Test mode — pretend you are the phone:</p>
              <div className="mt-3 flex gap-2">
                <button
                  disabled={pending}
                  onClick={() => run(() => simulateMpesa(order.number, order.accessToken, true))}
                  className="bg-green-700 px-4 py-2 text-white"
                >
                  Approve payment
                </button>
                <button
                  disabled={pending}
                  onClick={() => run(() => simulateMpesa(order.number, order.accessToken, false))}
                  className="border border-stone-400 px-4 py-2"
                >
                  Cancel on phone
                </button>
              </div>
            </div>
          )}
          <button
            disabled={pending}
            onClick={() => run(() => retryPayment(order.number, order.accessToken))}
            className="mt-6 text-sm underline"
          >
            Didn&apos;t get the prompt? Send it again
          </button>
        </div>
      )}

      {waiting && payment?.nextAction === "ENTER_TEST_CARD" && (
        <form
          className="mt-6 space-y-3"
          onSubmit={(e) => {
            e.preventDefault();
            const card = String(new FormData(e.currentTarget).get("card") ?? "");
            run(() => payWithTestCard(order.number, order.accessToken, card));
          }}
        >
          <p className="border border-amber-300 bg-amber-50 p-3 text-xs text-amber-900">
            Test mode: use 4242 4242 4242 4242 (success) or 4000 0000 0000 0002 (declined). Real card payments use
            Stripe&apos;s secure page once Stripe keys are configured.
          </p>
          <label className="block text-sm">
            Card number
            <input
              name="card"
              inputMode="numeric"
              autoComplete="cc-number"
              defaultValue="4242 4242 4242 4242"
              className="mt-1 w-full border border-stone-300 px-3 py-2"
            />
          </label>
          <div className="grid grid-cols-2 gap-3">
            <label className="block text-sm">
              Expiry
              <input defaultValue="12/30" autoComplete="cc-exp" className="mt-1 w-full border border-stone-300 px-3 py-2" />
            </label>
            <label className="block text-sm">
              CVC
              <input defaultValue="123" autoComplete="cc-csc" className="mt-1 w-full border border-stone-300 px-3 py-2" />
            </label>
          </div>
          <button disabled={pending} className="w-full bg-stone-900 py-3 text-sm uppercase tracking-widest text-white">
            {pending ? "Paying…" : `Pay ${formatPrice(order.total, order.currency)}`}
          </button>
        </form>
      )}

      {waiting && payment?.nextAction === "REDIRECT" && payment.redirectUrl && (
        <a href={payment.redirectUrl} className="mt-6 block bg-stone-900 py-3 text-center text-sm uppercase tracking-widest text-white">
          Continue to secure card payment
        </a>
      )}

      {(!payment || payment.status === "FAILED" || order.paymentStatus === "FAILED") && (
        <form
          className="mt-6 space-y-3"
          onSubmit={(e) => {
            e.preventDefault();
            const phone = String(new FormData(e.currentTarget).get("mpesaPhone") ?? "");
            run(() => retryPayment(order.number, order.accessToken, phone));
          }}
        >
          <p role="alert" className="text-sm text-red-700">
            {payment?.failureMessage ?? "We couldn't start the payment."} Your items are held for 30 minutes.
          </p>
          {order.paymentMethod === "MPESA" && (
            <label className="block text-sm">
              M-Pesa number
              <input
                name="mpesaPhone"
                type="tel"
                defaultValue={payment?.phone ? `0${payment.phone.slice(3)}` : ""}
                className="mt-1 w-full border border-stone-300 px-3 py-2"
              />
            </label>
          )}
          <button disabled={pending} className="w-full bg-stone-900 py-3 text-sm uppercase tracking-widest text-white">
            Try again
          </button>
        </form>
      )}

      {error && (
        <p role="alert" className="mt-4 text-sm text-red-700">
          {error}
        </p>
      )}
    </div>
  );
}
