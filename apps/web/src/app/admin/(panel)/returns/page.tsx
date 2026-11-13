import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { ReturnActions, type AdminReturn } from "@/components/admin/ReturnActions";
import { Loading, PageTitle } from "@/components/admin/ui";
import { adminFetch, getCurrentStaff } from "@/lib/admin/api";
import { can } from "@/lib/admin/types";
import { formatPrice } from "@/lib/format";

export const metadata: Metadata = { title: "Returns" };

const TABS = [
  ["REQUESTED", "New"],
  ["APPROVED", "Awaiting items"],
  ["RECEIVED", "To refund"],
  ["REFUNDED", "Refunded"],
  ["REJECTED", "Rejected"],
];
const REASON: Record<string, string> = {
  TOO_SMALL: "Too small",
  TOO_LARGE: "Too large",
  NOT_AS_DESCRIBED: "Not as described",
  DAMAGED: "Damaged",
  CHANGED_MIND: "Changed mind",
  OTHER: "Other",
};

async function Returns({ searchParams }: Pick<PageProps<"/admin/returns">, "searchParams">) {
  const [me, sp] = await Promise.all([getCurrentStaff(), searchParams]);
  if (!can.receiveReturns(me.role)) return <p className="text-stone-600">You don&apos;t have access to returns.</p>;
  const status = typeof sp.status === "string" ? sp.status : "REQUESTED";
  const returns = await adminFetch<AdminReturn[]>(`/api/v1/admin/returns?status=${status}`);
  return (
    <>
      <nav className="mb-4 flex flex-wrap gap-1 text-sm" aria-label="Return status">
        {TABS.map(([s, label]) => (
          <Link key={s} href={`/admin/returns?status=${s}`} aria-current={status === s ? "page" : undefined}
            className="rounded px-3 py-1.5 hover:bg-stone-200 aria-[current=page]:bg-stone-900 aria-[current=page]:text-white">
            {label}
          </Link>
        ))}
      </nav>
      {returns.length === 0 && <p className="admin-card text-sm text-stone-500">Nothing here.</p>}
      <ul className="space-y-4">
        {returns.map((r) => (
          <li key={r.id} className="admin-card space-y-3">
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <p className="font-medium">
                Return #{r.id} ·{" "}
                <Link href={`/admin/orders/${r.orderId}`} className="underline">{r.orderNumber}</Link>
              </p>
              <p className="text-sm text-stone-500">
                {r.customerName} · {new Date(r.createdAt).toLocaleDateString("en-KE", { dateStyle: "medium" })}
              </p>
            </div>
            <ul className="text-sm">
              {r.lines.map((l) => (
                <li key={l.id}>
                  {l.quantity} × {l.title} {[l.size, l.colour].filter(Boolean).length > 0 && `(${[l.size, l.colour].filter(Boolean).join(" / ")})`} —{" "}
                  <strong>{REASON[l.reason]}</strong> {l.restocked && <span className="text-green-800">· restocked</span>}
                </li>
              ))}
            </ul>
            {r.comment && <p className="text-sm italic text-stone-600">“{r.comment}”</p>}
            <p className="text-sm text-stone-500">
              Items value {formatPrice(r.itemsValue, "KES")}
              {r.refundAmount != null && ` · refunded ${formatPrice(r.refundAmount, "KES")}`}
            </p>
            <ReturnActions ret={r} canApprove={can.approveReturns(me.role)} canRefund={can.refund(me.role)} />
          </li>
        ))}
      </ul>
    </>
  );
}

export default function ReturnsPage({ searchParams }: PageProps<"/admin/returns">) {
  return (
    <>
      <PageTitle>Returns</PageTitle>
      <Suspense fallback={<Loading />}>
        <Returns searchParams={searchParams} />
      </Suspense>
    </>
  );
}
