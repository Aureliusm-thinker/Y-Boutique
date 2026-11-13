"use client";

import { useActionState, useTransition, useState } from "react";
import {
  addCustomerNote,
  addOrderNote,
  cancelOrder,
  deliverOrder,
  refundOrder,
  shipOrder,
} from "@/app/admin/(panel)/orders/actions";
import type { ActionResult, AdminOrderDetail } from "@/lib/admin/types";
import { formatPrice } from "@/lib/format";
import { ActionMessage } from "./ui";

const CARRIERS = ["G4S", "Wells Fargo Kenya", "Sendy", "Fargo Courier", "Posta Kenya", "Own rider"];

export function ShipForm({ orderId }: { orderId: number }) {
  const [result, action, pending] = useActionState<ActionResult | null, FormData>(shipOrder.bind(null, orderId), null);
  return (
    <form action={action} className="space-y-3 text-sm">
      <label className="block">
        Courier
        <input name="carrier" list="carriers" className="admin-input mt-1" />
        <datalist id="carriers">
          {CARRIERS.map((c) => (
            <option key={c} value={c} />
          ))}
        </datalist>
      </label>
      <label className="block">
        Tracking number <span className="text-stone-500">(optional)</span>
        <input name="trackingNumber" className="admin-input mt-1" />
      </label>
      <label className="block">
        Tracking link <span className="text-stone-500">(optional)</span>
        <input name="trackingUrl" type="url" placeholder="https://" className="admin-input mt-1" />
      </label>
      <button disabled={pending} className="admin-btn-primary w-full">
        Mark as shipped
      </button>
      <ActionMessage result={result} />
    </form>
  );
}

export function DeliverButton({ orderId, label }: { orderId: number; label: string }) {
  const [result, setResult] = useState<ActionResult | null>(null);
  const [pending, start] = useTransition();
  return (
    <div className="space-y-2">
      <button
        type="button"
        disabled={pending}
        onClick={() => start(async () => setResult(await deliverOrder(orderId)))}
        className="admin-btn-primary w-full"
      >
        {label}
      </button>
      <ActionMessage result={result} />
    </div>
  );
}

export function RefundForm({ detail }: { detail: AdminOrderDetail }) {
  const [result, action, pending] = useActionState<ActionResult | null, FormData>(refundOrder.bind(null, detail.id), null);
  const o = detail.order;
  return (
    <form action={action} className="space-y-3 text-sm">
      <p className="text-stone-600">Up to {formatPrice(detail.refundable, o.currency)} can be refunded.</p>
      <label className="block">
        Amount (KSh)
        <input name="amount" type="number" min={1} max={detail.refundable} step="1" required className="admin-input mt-1" />
      </label>
      <label className="block">
        Reason
        <input name="reason" required maxLength={300} placeholder="e.g. Item returned — wrong size" className="admin-input mt-1" />
      </label>
      <fieldset>
        <legend className="text-stone-600">Put items back in stock</legend>
        {o.lines.map((l) => {
          const max = l.quantity - l.returnedQuantity;
          return (
            <label key={l.id} className="mt-1 flex items-center justify-between gap-2">
              <span>
                {l.title} {l.size && `(${l.size})`}
              </span>
              <input name={`restock-${l.id}`} type="number" min={0} max={max} defaultValue={0} disabled={max === 0} className="admin-input w-16" />
            </label>
          );
        })}
      </fieldset>
      <button
        disabled={pending}
        className="admin-btn-primary w-full"
        onClick={(e) => {
          if (!confirm("Send this refund now? Money goes back the way the customer paid.")) e.preventDefault();
        }}
      >
        Issue refund
      </button>
      <ActionMessage result={result} />
    </form>
  );
}

export function CancelForm({ orderId, paid }: { orderId: number; paid: boolean }) {
  const [result, action, pending] = useActionState<ActionResult | null, FormData>(cancelOrder.bind(null, orderId), null);
  return (
    <form action={action} className="space-y-3 text-sm">
      <p className="text-stone-600">
        {paid ? "Refunds the full amount and puts everything back in stock." : "Releases the reserved stock."}
      </p>
      <label className="block">
        Reason
        <input name="reason" required maxLength={300} className="admin-input mt-1" />
      </label>
      <button
        disabled={pending}
        className="admin-btn w-full border-red-300 text-red-700"
        onClick={(e) => {
          if (!confirm("Cancel this order? The customer will be emailed.")) e.preventDefault();
        }}
      >
        Cancel order
      </button>
      <ActionMessage result={result} />
    </form>
  );
}

export function NoteForm({ id, kind }: { id: number; kind: "order" | "customer" }) {
  const [result, action, pending] = useActionState<ActionResult | null, FormData>(
    (kind === "order" ? addOrderNote : addCustomerNote).bind(null, id),
    null,
  );
  return (
    <form action={action} className="space-y-2 text-sm">
      <label className="block">
        Add an internal note <span className="text-stone-500">(customers never see these)</span>
        <textarea name="note" required maxLength={500} rows={2} className="admin-input mt-1" />
      </label>
      <button disabled={pending} className="admin-btn">
        Add note
      </button>
      <ActionMessage result={result && !result.ok ? result : null} />
    </form>
  );
}
