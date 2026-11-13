import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { Loading, PageTitle } from "@/components/admin/ui";
import { adminFetch, getCurrentStaff } from "@/lib/admin/api";
import { can, type AdminCustomerRow } from "@/lib/admin/types";
import { formatPrice } from "@/lib/format";
import type { Page } from "@/lib/types";

export const metadata: Metadata = { title: "Customers" };

async function CustomerList({ searchParams }: Pick<PageProps<"/admin/customers">, "searchParams">) {
  const [me, sp] = await Promise.all([getCurrentStaff(), searchParams]);
  if (!can.viewCustomers(me.role)) return <p className="text-stone-600">You don&apos;t have access to customers.</p>;
  const q = typeof sp.q === "string" ? sp.q : "";
  const result = await adminFetch<Page<AdminCustomerRow>>(`/api/v1/admin/customers?q=${encodeURIComponent(q)}`);
  return (
    <>
      <form className="mb-4 flex gap-2">
        <input name="q" defaultValue={q} placeholder="Email, name, phone or order number" aria-label="Search customers" className="admin-input max-w-sm" />
        <button className="admin-btn">Search</button>
      </form>
      {result.items.length === 0 ? (
        <p className="admin-card text-sm text-stone-500">No customers found. Guest orders are on the Orders page.</p>
      ) : (
        <div className="admin-card overflow-x-auto p-0">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-stone-200 text-xs uppercase tracking-wide text-stone-500">
              <tr>
                <th className="px-3 py-2">Name</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Orders</th>
                <th className="pr-3 text-right">Spent</th>
              </tr>
            </thead>
            <tbody>
              {result.items.map((c) => (
                <tr key={c.id} className="border-b border-stone-100 hover:bg-stone-50">
                  <td className="px-3 py-2">
                    <Link href={`/admin/customers/${c.id}`} className="font-medium hover:underline">
                      {c.fullName}
                    </Link>
                  </td>
                  <td>
                    {c.email} {!c.emailVerified && <span className="text-xs text-stone-400">(unverified)</span>}
                  </td>
                  <td>{c.phone ? `0${c.phone.slice(3)}` : "—"}</td>
                  <td>{c.orderCount}</td>
                  <td className="pr-3 text-right">{formatPrice(c.totalSpent, "KES")}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

export default function CustomersPage({ searchParams }: PageProps<"/admin/customers">) {
  return (
    <>
      <PageTitle>Customers</PageTitle>
      <Suspense fallback={<Loading />}>
        <CustomerList searchParams={searchParams} />
      </Suspense>
    </>
  );
}
