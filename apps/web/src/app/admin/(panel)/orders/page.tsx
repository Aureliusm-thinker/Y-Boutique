import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { Loading, orderStatusStyle, PageTitle } from "@/components/admin/ui";
import { adminFetch, getCurrentStaff } from "@/lib/admin/api";
import { can, type AdminOrderRow } from "@/lib/admin/types";
import { formatPrice } from "@/lib/format";
import type { Page } from "@/lib/types";

export const metadata: Metadata = { title: "Orders" };

const TABS = [
  { status: "PAID", label: "To ship" },
  { status: "PENDING_PAYMENT", label: "Awaiting payment" },
  { status: "SHIPPED", label: "Shipped" },
  { status: "DELIVERED", label: "Delivered" },
  { status: "CANCELLED", label: "Cancelled" },
  { status: "", label: "All" },
];


async function OrderList({ searchParams }: Pick<PageProps<"/admin/orders">, "searchParams">) {
  const [me, sp] = await Promise.all([getCurrentStaff(), searchParams]);
  if (!can.viewOrders(me.role)) return <p className="text-stone-600">You don&apos;t have access to orders.</p>;
  const status = typeof sp.status === "string" ? sp.status : "PAID";
  const q = typeof sp.q === "string" ? sp.q : "";
  const page = typeof sp.page === "string" ? Number(sp.page) || 0 : 0;
  const qs = new URLSearchParams({ page: String(page) });
  if (status) qs.set("status", status);
  if (q) qs.set("q", q);
  const [orders, counts] = await Promise.all([
    adminFetch<Page<AdminOrderRow>>(`/api/v1/admin/orders?${qs}`),
    adminFetch<Record<string, number>>("/api/v1/admin/orders/counts"),
  ]);
  const canFulfil = can.fulfilOrders(me.role);

  return (
    <>
      <nav className="mb-4 flex flex-wrap gap-1 text-sm" aria-label="Order status">
        {TABS.map((t) => (
          <Link
            key={t.label}
            href={`/admin/orders?status=${t.status}${q ? `&q=${encodeURIComponent(q)}` : ""}`}
            aria-current={status === t.status ? "page" : undefined}
            className="rounded px-3 py-1.5 hover:bg-stone-200 aria-[current=page]:bg-stone-900 aria-[current=page]:text-white"
          >
            {t.label}
            {t.status === "PAID" && counts.toShip > 0 && <span className="ml-1 rounded-full bg-amber-500 px-1.5 text-xs text-white">{counts.toShip}</span>}
          </Link>
        ))}
      </nav>
      <form className="mb-4 flex gap-2">
        <input type="hidden" name="status" value={status} />
        <input name="q" defaultValue={q} placeholder="Order number, email, name or phone" aria-label="Search orders" className="admin-input max-w-sm" />
        <button className="admin-btn">Search</button>
      </form>

      {orders.items.length === 0 ? (
        <p className="admin-card text-sm text-stone-500">No orders here.</p>
      ) : (
        <form action="/admin/orders/packing-slips" className="admin-card overflow-x-auto p-0">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-stone-200 text-xs uppercase tracking-wide text-stone-500">
              <tr>
                {canFulfil && <th className="w-8 px-3 py-2" aria-label="Select" />}
                <th className="px-3 py-2">Order</th>
                <th>Customer</th>
                <th>Placed</th>
                <th>Items</th>
                <th>Delivery</th>
                <th>Status</th>
                <th className="pr-3 text-right">Total</th>
              </tr>
            </thead>
            <tbody>
              {orders.items.map((o) => (
                <tr key={o.id} className="border-b border-stone-100 hover:bg-stone-50">
                  {canFulfil && (
                    <td className="px-3">
                      <input type="checkbox" name="ids" value={o.id} aria-label={`Select ${o.number}`} />
                    </td>
                  )}
                  <td className="px-3 py-2">
                    <Link href={`/admin/orders/${o.id}`} className="font-medium hover:underline">
                      {o.number}
                    </Link>
                  </td>
                  <td>
                    {o.customerName}
                    <span className="block text-xs text-stone-500">{o.email}</span>
                  </td>
                  <td className="whitespace-nowrap">{new Date(o.placedAt).toLocaleString("en-KE", { dateStyle: "medium", timeStyle: "short" })}</td>
                  <td>{o.itemCount}</td>
                  <td>{o.shippingMethod}</td>
                  <td>
                    <span className={`rounded-full px-2 py-0.5 text-xs ${orderStatusStyle[o.status]}`}>{o.status.replace("_", " ").toLowerCase()}</span>
                  </td>
                  <td className="pr-3 text-right">{formatPrice(o.total, o.currency)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {canFulfil && (
            <div className="border-t border-stone-200 p-3">
              <button className="admin-btn">Print packing slips for selected</button>
            </div>
          )}
        </form>
      )}
      {orders.totalPages > 1 && (
        <p className="mt-4 flex gap-4 text-sm">
          {page > 0 && <Link href={`/admin/orders?status=${status}&q=${q}&page=${page - 1}`}>← Newer</Link>}
          <span>
            Page {page + 1} of {orders.totalPages}
          </span>
          {page + 1 < orders.totalPages && <Link href={`/admin/orders?status=${status}&q=${q}&page=${page + 1}`}>Older →</Link>}
        </p>
      )}
    </>
  );
}

export default function OrdersPage({ searchParams }: PageProps<"/admin/orders">) {
  return (
    <>
      <PageTitle>Orders</PageTitle>
      <Suspense fallback={<Loading />}>
        <OrderList searchParams={searchParams} />
      </Suspense>
    </>
  );
}
