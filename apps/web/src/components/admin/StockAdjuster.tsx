"use client";

import { useState, useTransition } from "react";
import { adjustStock } from "@/app/admin/(panel)/products/actions";
import { ADJUSTMENT_REASONS, type ActionResult, type AdjustmentReason } from "@/lib/admin/types";
import { ActionMessage } from "./ui";

export type AdjustableVariant = {
  id: number;
  label: string;
  onHand: number;
  available: number;
  low: boolean;
};

const REASON_LABELS: Record<AdjustmentReason, string> = {
  RECEIVED: "Received delivery",
  RETURNED: "Customer return",
  DAMAGED: "Damaged",
  LOST: "Lost / stolen",
  CORRECTION: "Stock-take correction",
  OTHER: "Other",
};

/** Records a stock change for one variant with a reason (FR-AD-04). */
export function StockAdjuster({ variants }: { variants: AdjustableVariant[] }) {
  const [variantId, setVariantId] = useState(variants[0]?.id ?? 0);
  const [delta, setDelta] = useState("");
  const [reason, setReason] = useState<AdjustmentReason>("RECEIVED");
  const [note, setNote] = useState("");
  const [result, setResult] = useState<ActionResult | null>(null);
  const [pending, start] = useTransition();

  if (variants.length === 0) return <p className="text-sm text-stone-500">Save a variant first.</p>;

  function submit(e: React.FormEvent) {
    e.preventDefault();
    start(async () => {
      const res = await adjustStock(variantId, Number(delta), reason, note);
      setResult(res);
      if (res.ok) {
        setDelta("");
        setNote("");
      }
    });
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <ul className="divide-y divide-stone-100 text-sm">
        {variants.map((v) => (
          <li key={v.id} className="flex justify-between py-1.5">
            <span>{v.label}</span>
            <span className={`tabular-nums ${v.available <= 0 ? "text-red-700" : v.low ? "text-amber-700" : ""}`}>
              {v.onHand} on hand{v.low && " · low"}
            </span>
          </li>
        ))}
      </ul>
      <label className="block text-sm">
        Variant
        <select value={variantId} onChange={(e) => setVariantId(Number(e.target.value))} className="admin-input mt-1">
          {variants.map((v) => (
            <option key={v.id} value={v.id}>
              {v.label}
            </option>
          ))}
        </select>
      </label>
      <div className="grid grid-cols-2 gap-2">
        <label className="block text-sm">
          Change (+/−)
          <input
            value={delta}
            onChange={(e) => setDelta(e.target.value)}
            type="number"
            step="1"
            required
            placeholder="e.g. 5 or -1"
            className="admin-input mt-1"
          />
        </label>
        <label className="block text-sm">
          Reason
          <select value={reason} onChange={(e) => setReason(e.target.value as AdjustmentReason)} className="admin-input mt-1">
            {ADJUSTMENT_REASONS.map((r) => (
              <option key={r} value={r}>
                {REASON_LABELS[r]}
              </option>
            ))}
          </select>
        </label>
      </div>
      <label className="block text-sm">
        Note <span className="text-stone-500">(optional)</span>
        <input value={note} onChange={(e) => setNote(e.target.value)} maxLength={300} placeholder="e.g. PO-1001" className="admin-input mt-1" />
      </label>
      <button disabled={pending} className="admin-btn-primary">
        {pending ? "Saving…" : "Adjust stock"}
      </button>
      <ActionMessage result={result} />
    </form>
  );
}
