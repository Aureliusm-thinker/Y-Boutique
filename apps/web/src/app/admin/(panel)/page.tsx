import Link from "next/link";
import { Suspense } from "react";
import { Loading, PageTitle } from "@/components/admin/ui";
import { adminFetch, getCurrentStaff } from "@/lib/admin/api";
import { can, type LowStockRow, type Me } from "@/lib/admin/types";
import { formatPrice } from "@/lib/format";

type Summary = {
  days: number;
  sales: number;
  refunds: number;
  orders: number;
  averageOrderValue: number;
  itemsSold: number;
  newCustomers: number;
  daily: { day: string; sales: number; orders: number }[];
  topProducts: { productId: number; title: string; unitsSold: number; revenue: number }[];
  funnel: { viewedProduct: number; addedToBag: number; startedCheckout: number; purchased: number };
  lowStockVariants: number;
  toShip: number;
  openReturns: number;
  pendingReviews: number;
  returnReasons: { reason: string; count: number }[];
};

const REASON: Record<string, string> = {
  TOO_SMALL: "Too small",
  TOO_LARGE: "Too large",
  NOT_AS_DESCRIBED: "Not as described",
  DAMAGED: "Damaged",
  CHANGED_MIND: "Changed mind",
  OTHER: "Other",
};

function Tile({ label, value, href, tone }: { label: string; value: string | number; href?: string; tone?: "alert" }) {
  const body = (
    <>
      <p className="text-sm text-stone-500">{label}</p>
      <p className={`mt-1 text-3xl tabular-nums ${tone === "alert" ? "text-amber-700" : ""}`}>{value}</p>
    </>
  );
  return href ? (
    <Link href={href} className="admin-card hover:border-stone-400">
      {body}
    </Link>
  ) : (
    <div className="admin-card">{body}</div>
  );
}

/** Daily sales as simple bars (no chart library needed). */
function SalesBars({ daily }: { daily: Summary["daily"] }) {
  const max = Math.max(1, ...daily.map((d) => d.sales));
  return (
    <figure>
      <div className="flex h-32 items-end gap-0.5" role="img" aria-label="Sales per day">
        {daily.map((d) => (
          <div
            key={d.day}
            title={`${d.day}: ${formatPrice(d.sales, "KES")} (${d.orders} orders)`}
            className="flex-1 bg-stone-800"
            style={{ height: `${Math.max(2, (d.sales / max) * 100)}%`, opacity: d.sales === 0 ? 0.15 : 1 }}
          />
        ))}
      </div>
      <figcaption className="mt-1 flex justify-between text-xs text-stone-500">
        <span>{daily[0]?.day}</span>
        <span>{daily.at(-1)?.day}</span>
      </figcaption>
    </figure>
  );
}

function Funnel({ f }: { f: Summary["funnel"] }) {
  const steps = [
    ["Viewed a product", f.viewedProduct],
    ["Added to bag", f.addedToBag],
    ["Started checkout", f.startedCheckout],
    ["Bought", f.purchased],
  ] as const;
  const top = Math.max(1, steps[0][1]);
  return (
    <dl className="space-y-2 text-sm">
      {steps.map(([label, n]) => (
        <div key={label} className="flex items-center gap-3">
          <dt className="w-32 shrink-0 text-stone-600">{label}</dt>
          <dd className="h-5 flex-1 bg-stone-100">
            <div className="h-full bg-stone-800" style={{ width: `${Math.min(100, (n / top) * 100)}%` }} />
          </dd>
          <dd className="w-12 text-right tabular-nums">{n}</dd>
        </div>
      ))}
      <p className="pt-1 text-xs text-stone-500">Visitors who accepted analytics. &ldquo;Bought&rdquo; counts all paid orders.</p>
    </dl>
  );
}

async function SalesReport({ days }: { days: number }) {
  const s = await adminFetch<Summary>(`/api/v1/admin/reports/summary?days=${days}`);
  return (
    <>
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <Tile label={`Sales (last ${s.days} days)`} value={formatPrice(s.sales, "KES")} />
        <Tile label="Orders" value={s.orders} href="/admin/orders?status=" />
        <Tile label="Average order" value={formatPrice(s.averageOrderValue, "KES")} />
        <Tile label="New customers" value={s.newCustomers} href="/admin/customers" />
      </div>
      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <section className="admin-card">
          <h2 className="mb-3 font-medium">Sales per day</h2>
          <SalesBars daily={s.daily} />
          {s.refunds > 0 && <p className="mt-2 text-xs text-stone-500">Sales are after {formatPrice(s.refunds, "KES")} of refunds.</p>}
        </section>
        <section className="admin-card">
          <h2 className="mb-3 font-medium">Shopping funnel</h2>
          <Funnel f={s.funnel} />
        </section>
        <section className="admin-card">
          <h2 className="mb-3 font-medium">Top products</h2>
          {s.topProducts.length === 0 ? (
            <p className="text-sm text-stone-500">No sales in this period.</p>
          ) : (
            <ol className="space-y-1 text-sm">
              {s.topProducts.map((p) => (
                <li key={p.productId} className="flex justify-between gap-2">
                  <Link href={`/admin/products/${p.productId}`} className="hover:underline">{p.title}</Link>
                  <span className="text-stone-600">
                    {p.unitsSold} sold · {formatPrice(p.revenue, "KES")}
                  </span>
                </li>
              ))}
            </ol>
          )}
        </section>
        <section className="admin-card">
          <h2 className="mb-3 font-medium">Why items come back</h2>
          {s.returnReasons.length === 0 ? (
            <p className="text-sm text-stone-500">No returns in this period.</p>
          ) : (
            <ul className="space-y-1 text-sm">
              {s.returnReasons.map((r) => (
                <li key={r.reason} className="flex justify-between">
                  <span>{REASON[r.reason] ?? r.reason}</span>
                  <span>{r.count}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </>
  );
}

async function WorkQueue({ me }: { me: Me }) {
  const [counts, lowStock, returns, reviews] = await Promise.all([
    can.viewOrders(me.role) ? adminFetch<Record<string, number>>("/api/v1/admin/orders/counts") : null,
    can.viewCatalog(me.role) ? adminFetch<LowStockRow[]>("/api/v1/admin/inventory/low-stock") : null,
    can.receiveReturns(me.role) ? adminFetch<unknown[]>("/api/v1/admin/returns?status=REQUESTED") : null,
    can.moderateReviews(me.role) ? adminFetch<unknown[]>("/api/v1/admin/reviews?status=PENDING") : null,
  ]);
  return (
    <>
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        {counts && <Tile label="Orders to ship" value={counts.toShip} href="/admin/orders?status=PAID" tone={counts.toShip > 0 ? "alert" : undefined} />}
        {counts && <Tile label="Awaiting payment" value={counts.awaitingPayment} href="/admin/orders?status=PENDING_PAYMENT" />}
        {returns && <Tile label="New returns" value={returns.length} href="/admin/returns" tone={returns.length > 0 ? "alert" : undefined} />}
        {reviews && <Tile label="Reviews to check" value={reviews.length} href="/admin/reviews" />}
        {lowStock && <Tile label="Low-stock variants" value={lowStock.length} href="/admin/inventory" tone={lowStock.length > 0 ? "alert" : undefined} />}
      </div>
      {lowStock && lowStock.length > 0 && (
        <section className="admin-card mt-6">
          <h2 className="mb-3 font-medium">Running low</h2>
          <ul className="divide-y divide-stone-100 text-sm">
            {lowStock.slice(0, 8).map((r) => (
              <li key={r.variantId} className="flex justify-between py-2">
                <Link href={`/admin/products/${r.productId}`} className="hover:underline">
                  {r.productTitle} <span className="text-stone-500">· {[r.size, r.colour].filter(Boolean).join(" / ") || r.sku}</span>
                </Link>
                <span className={r.onHand - r.reserved <= 0 ? "text-red-700" : "text-amber-700"}>{r.onHand - r.reserved} left</span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </>
  );
}

async function Dashboard({ searchParams }: Pick<PageProps<"/admin">, "searchParams">) {
  const [me, sp] = await Promise.all([getCurrentStaff(), searchParams]);
  const days = [7, 30, 90].includes(Number(sp.days)) ? Number(sp.days) : 30;
  return (
    <>
      <p className="mb-6 text-stone-600">Welcome back, {me.fullName.split(" ")[0]}.</p>
      <WorkQueue me={me} />
      {can.reports(me.role) && (
        <section className="mt-10">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
            <h2 className="font-serif text-2xl">Sales</h2>
            <nav className="flex gap-1 text-sm" aria-label="Period">
              {[7, 30, 90].map((d) => (
                <Link key={d} href={`/admin?days=${d}`} aria-current={d === days ? "page" : undefined}
                  className="rounded px-3 py-1 hover:bg-stone-200 aria-[current=page]:bg-stone-900 aria-[current=page]:text-white">
                  {d} days
                </Link>
              ))}
            </nav>
          </div>
          <SalesReport days={days} />
        </section>
      )}
    </>
  );
}

export default function AdminHome({ searchParams }: PageProps<"/admin">) {
  return (
    <>
      <PageTitle>Dashboard</PageTitle>
      <Suspense fallback={<Loading />}>
        <Dashboard searchParams={searchParams} />
      </Suspense>
    </>
  );
}
