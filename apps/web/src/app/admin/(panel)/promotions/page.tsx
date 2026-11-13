import type { Metadata } from "next";
import { Suspense } from "react";
import { PromotionForm, type Promotion } from "@/components/admin/PromotionForm";
import { Loading, PageTitle } from "@/components/admin/ui";
import { adminFetch, getCurrentStaff } from "@/lib/admin/api";
import { can } from "@/lib/admin/types";
import { formatPrice } from "@/lib/format";

export const metadata: Metadata = { title: "Discounts" };

const STATE_STYLE: Record<string, string> = {
  Live: "bg-green-100 text-green-800",
  Scheduled: "bg-blue-100 text-blue-900",
  Ended: "bg-stone-200 text-stone-600",
  "Used up": "bg-stone-200 text-stone-600",
  Off: "bg-stone-100 text-stone-500",
};

const describe = (p: Promotion) =>
  p.type === "PERCENT" ? `${p.value}% off` : p.type === "FIXED" ? `${formatPrice(p.value, "KES")} off` : "Free delivery";

async function Promotions() {
  const me = await getCurrentStaff();
  if (!can.marketing(me.role)) return <p className="text-stone-600">You don&apos;t have access to discounts.</p>;
  const promotions = await adminFetch<Promotion[]>("/api/v1/admin/promotions");
  return (
    <div className="space-y-6">
      <section className="admin-card">
        <h2 className="mb-1 font-medium">New discount or sale</h2>
        <p className="mb-4 text-sm text-stone-500">
          With a code, shoppers enter it in their bag. Without a code it&apos;s an automatic sale for everyone (the best one applies).
        </p>
        <PromotionForm />
      </section>
      <ul className="space-y-3">
        {promotions.map((p) => (
          <li key={p.id} className="admin-card">
            <details>
              <summary className="flex cursor-pointer flex-wrap items-center gap-3 text-sm">
                <span className={`rounded-full px-2 py-0.5 text-xs ${STATE_STYLE[p.state]}`}>{p.state}</span>
                <span className="font-medium">{p.code ?? "Automatic"}</span>
                <span>{p.name}</span>
                <span className="text-stone-500">
                  {describe(p)}
                  {p.minSubtotal ? ` over ${formatPrice(p.minSubtotal, "KES")}` : ""} · used {p.timesUsed}
                  {p.usageLimit ? ` of ${p.usageLimit}` : ""}
                </span>
              </summary>
              <div className="mt-4">
                <PromotionForm promotion={p} />
              </div>
            </details>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default function PromotionsPage() {
  return (
    <>
      <PageTitle>Discounts &amp; sales</PageTitle>
      <Suspense fallback={<Loading />}>
        <Promotions />
      </Suspense>
    </>
  );
}
