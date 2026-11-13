import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { NoteForm } from "@/components/admin/OrderPanels";
import { Loading, OrderStatusBadge } from "@/components/admin/ui";
import { adminFetch, ApiError } from "@/lib/admin/api";
import type { AdminCustomerDetail } from "@/lib/admin/types";
import { formatPrice } from "@/lib/format";

export const metadata: Metadata = { title: "Customer" };

async function CustomerView({ params }: Pick<PageProps<"/admin/customers/[id]">, "params">) {
  const { id } = await params;
  if (!/^\d+$/.test(id)) notFound();
  let c: AdminCustomerDetail;
  try {
    c = await adminFetch<AdminCustomerDetail>(`/api/v1/admin/customers/${id}`);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }
  const p = c.profile;
  return (
    <>
      <Link href="/admin/customers" className="text-sm text-stone-500 hover:underline">
        ← Customers
      </Link>
      <h1 className="mb-6 mt-1 font-serif text-3xl">{p.fullName}</h1>
      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="space-y-6">
          <section className="admin-card">
            <h2 className="mb-3 font-medium">Orders</h2>
            {c.orders.length === 0 ? (
              <p className="text-sm text-stone-500">No orders yet.</p>
            ) : (
              <table className="w-full text-left text-sm">
                <tbody>
                  {c.orders.map((o) => (
                    <tr key={o.number} className="border-b border-stone-100">
                      <td className="py-2">
                        <Link href={`/admin/orders?status=&q=${o.number}`} className="hover:underline">
                          {o.number}
                        </Link>
                      </td>
                      <td>{new Date(o.placedAt).toLocaleDateString("en-KE", { dateStyle: "medium" })}</td>
                      <td>
                        <OrderStatusBadge status={o.status} />
                      </td>
                      <td className="text-right">{formatPrice(o.total, o.currency)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>
          <section className="admin-card">
            <h2 className="mb-3 font-medium">Notes</h2>
            <ul className="mb-4 space-y-2 text-sm">
              {c.notes.map((n, i) => (
                <li key={i}>
                  {n.note}
                  <span className="block text-xs text-stone-500">
                    {n.staffName} · {new Date(n.at).toLocaleString("en-KE", { dateStyle: "medium", timeStyle: "short" })}
                  </span>
                </li>
              ))}
            </ul>
            <NoteForm id={p.id} kind="customer" />
          </section>
        </div>
        <aside className="admin-card h-fit space-y-3 text-sm">
          <p>
            <span className="text-stone-500">Email</span>
            <br />
            {p.email} {p.emailVerified ? "✓" : <span className="text-stone-400">(unverified)</span>}
          </p>
          <p>
            <span className="text-stone-500">Phone</span>
            <br />
            {p.phone ? `0${p.phone.slice(3)}` : "—"}
          </p>
          <p>
            <span className="text-stone-500">Total spent</span>
            <br />
            {formatPrice(c.totalSpent, "KES")}
          </p>
          <p>
            <span className="text-stone-500">Customer since</span>
            <br />
            {new Date(p.createdAt).toLocaleDateString("en-KE", { dateStyle: "medium" })}
          </p>
          {p.addresses[0] && (
            <p>
              <span className="text-stone-500">Default address</span>
              <br />
              {p.addresses[0].line1}, {p.addresses[0].city}, {p.addresses[0].county}
            </p>
          )}
          {(p.preferences.sizes.length > 0 || p.preferences.styles.length > 0) && (
            <p>
              <span className="text-stone-500">Style</span>
              <br />
              {[...p.preferences.sizes, ...p.preferences.styles].join(", ")}
            </p>
          )}
          <p>
            <span className="text-stone-500">Marketing emails</span>
            <br />
            {p.marketingOptIn ? "Yes" : "No"}
          </p>
        </aside>
      </div>
    </>
  );
}

export default function AdminCustomerPage({ params }: PageProps<"/admin/customers/[id]">) {
  return (
    <Suspense fallback={<Loading />}>
      <CustomerView params={params} />
    </Suspense>
  );
}
