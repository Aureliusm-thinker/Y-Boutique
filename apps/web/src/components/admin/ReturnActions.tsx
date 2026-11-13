"use client";

import { useActionState } from "react";
import { approveReturn, receiveReturn, refundReturn, rejectReturn } from "@/app/admin/(panel)/returns/actions";
import type { ActionResult } from "@/lib/admin/types";
import { ActionMessage } from "./ui";

export type AdminReturn = {
  id: number;
  orderId: number;
  orderNumber: string;
  status: string;
  comment: string | null;
  staffComment: string | null;
  refundAmount: number | null;
  itemsValue: number;
  customerName: string;
  email: string;
  createdAt: string;
  lines: { id: number; title: string; size: string | null; colour: string | null; sku: string; quantity: number; reason: string; restocked: boolean; unitPrice: number }[];
};

export function ReturnActions({ ret, canApprove, canRefund }: { ret: AdminReturn; canApprove: boolean; canRefund: boolean }) {
  const [approveResult, approve, approving] = useActionState<ActionResult | null, FormData>(approveReturn.bind(null, ret.id), null);
  const [rejectResult, reject, rejecting] = useActionState<ActionResult | null, FormData>(rejectReturn.bind(null, ret.id), null);
  const [receiveResult, receive, receiving] = useActionState<ActionResult | null, FormData>(receiveReturn.bind(null, ret.id), null);
  const [refundResult, refund, refunding] = useActionState<ActionResult | null, FormData>(refundReturn.bind(null, ret.id), null);

  if (ret.status === "REQUESTED" && canApprove) {
    return (
      <div className="grid gap-4 sm:grid-cols-2">
        <form action={approve} className="space-y-2 text-sm">
          <input name="comment" placeholder="Note to the customer (optional)" aria-label="Note to the customer" className="admin-input" />
          <button disabled={approving} className="admin-btn-primary">Approve</button>
          <ActionMessage result={approveResult} />
        </form>
        <form action={reject} className="space-y-2 text-sm">
          <input name="comment" required placeholder="Why it can't be accepted" aria-label="Reason for rejecting" className="admin-input" />
          <button disabled={rejecting} className="admin-btn text-red-700">Reject</button>
          <ActionMessage result={rejectResult} />
        </form>
      </div>
    );
  }
  if (ret.status === "APPROVED") {
    return (
      <form action={receive} className="space-y-2 text-sm">
        <p>Items arrived? Tick the ones in good condition to put back in stock:</p>
        {ret.lines.map((l) => (
          <label key={l.id} className="flex items-center gap-2">
            <input type="checkbox" name="restock" value={l.id} defaultChecked={l.reason !== "DAMAGED"} />
            {l.quantity} × {l.title} {l.size && `(${l.size})`}
          </label>
        ))}
        <button disabled={receiving} className="admin-btn-primary">Mark as received</button>
        <ActionMessage result={receiveResult} />
      </form>
    );
  }
  if (ret.status === "RECEIVED" && canRefund) {
    return (
      <form action={refund} className="flex flex-wrap items-end gap-2 text-sm">
        <label>
          Refund amount (KSh)
          <input name="amount" type="number" min={1} defaultValue={ret.itemsValue} className="admin-input mt-1 w-36" />
        </label>
        <button disabled={refunding} className="admin-btn-primary">Refund</button>
        <ActionMessage result={refundResult} />
      </form>
    );
  }
  return null;
}
