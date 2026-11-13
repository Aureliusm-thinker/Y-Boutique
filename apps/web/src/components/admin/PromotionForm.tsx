"use client";

import { useActionState, useState } from "react";
import { savePromotion } from "@/app/admin/(panel)/promotions/actions";
import type { ActionResult } from "@/lib/admin/types";
import { ActionMessage } from "./ui";

export type Promotion = {
  id: number;
  code: string | null;
  name: string;
  type: "PERCENT" | "FIXED" | "FREE_SHIPPING";
  value: number;
  minSubtotal: number | null;
  startsAt: string | null;
  endsAt: string | null;
  usageLimit: number | null;
  timesUsed: number;
  active: boolean;
  state: string;
};

/** Nairobi local time for a datetime-local input. */
const local = (iso: string | null) =>
  iso ? new Date(new Date(iso).getTime() + 3 * 3600 * 1000).toISOString().slice(0, 16) : "";

export function PromotionForm({ promotion }: { promotion?: Promotion }) {
  const [result, action, pending] = useActionState<ActionResult | null, FormData>(
    savePromotion.bind(null, promotion?.id ?? null),
    null,
  );
  const [type, setType] = useState(promotion?.type ?? "PERCENT");
  return (
    <form action={action} className="grid gap-3 text-sm sm:grid-cols-3">
      <label className="block sm:col-span-2">
        Name <span className="text-stone-500">(shown to shoppers)</span>
        <input name="name" required defaultValue={promotion?.name} placeholder="e.g. Madaraka Day 15% off" className="admin-input mt-1" />
      </label>
      <label className="block">
        Code <span className="text-stone-500">(empty = automatic sale)</span>
        <input name="code" defaultValue={promotion?.code ?? ""} placeholder="MADARAKA15" className="admin-input mt-1 uppercase" />
      </label>
      <label className="block">
        Type
        <select name="type" value={type} onChange={(e) => setType(e.target.value as Promotion["type"])} className="admin-input mt-1">
          <option value="PERCENT">% off</option>
          <option value="FIXED">KSh off</option>
          <option value="FREE_SHIPPING">Free delivery</option>
        </select>
      </label>
      {type !== "FREE_SHIPPING" && (
        <label className="block">
          {type === "PERCENT" ? "Percent off" : "Amount off (KSh)"}
          <input name="value" type="number" min={1} max={type === "PERCENT" ? 100 : undefined} required defaultValue={promotion?.value} className="admin-input mt-1" />
        </label>
      )}
      <label className="block">
        Minimum spend (KSh) <span className="text-stone-500">(optional)</span>
        <input name="minSubtotal" type="number" min={0} defaultValue={promotion?.minSubtotal ?? ""} className="admin-input mt-1" />
      </label>
      <label className="block">
        Starts <span className="text-stone-500">(optional)</span>
        <input name="startsAt" type="datetime-local" defaultValue={local(promotion?.startsAt ?? null)} className="admin-input mt-1" />
      </label>
      <label className="block">
        Ends <span className="text-stone-500">(optional)</span>
        <input name="endsAt" type="datetime-local" defaultValue={local(promotion?.endsAt ?? null)} className="admin-input mt-1" />
      </label>
      <label className="block">
        Max uses <span className="text-stone-500">(optional)</span>
        <input name="usageLimit" type="number" min={1} defaultValue={promotion?.usageLimit ?? ""} className="admin-input mt-1" />
      </label>
      <label className="flex items-center gap-2 sm:col-span-3">
        <input type="checkbox" name="active" defaultChecked={promotion?.active ?? true} /> Active
      </label>
      <div className="flex items-center gap-3 sm:col-span-3">
        <button disabled={pending} className="admin-btn-primary">
          {promotion ? "Save" : "Create"}
        </button>
        <ActionMessage result={result} />
      </div>
    </form>
  );
}
