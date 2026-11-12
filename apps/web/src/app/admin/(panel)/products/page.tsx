import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { Loading, PageTitle, StatusBadge } from "@/components/admin/ui";
import { adminFetch, getCurrentStaff } from "@/lib/admin/api";
import { can, type AdminProductSummary } from "@/lib/admin/types";
import { formatPrice } from "@/lib/format";
import type { Page } from "@/lib/types";

export const metadata: Metadata = { title: "Products" };

const STATUSES = [
  { value: "", label: "All" },
  { value: "ACTIVE", label: "Live" },
  { value: "DRAFT", label: "Drafts" },
  { value: "ARCHIVED", label: "Archived" },
];

async function ProductTable({ searchParams }: Pick<PageProps<"/admin/products">, "searchParams">) {
  const sp = await searchParams;
  const q = typeof sp.q === "string" ? sp.q : "";
  const status = typeof sp.status === "string" ? sp.status : "";
  const page = Math.max(0, Number(sp.page ?? 0) || 0);

  const qs = new URLSearchParams({ page: String(page), size: "25" });
  if (q) qs.set("q", q);
  if (status) qs.set("status", status);
  const [result, me] = await Promise.all([
    adminFetch<Page<AdminProductSummary>>(`/api/v1/admin/products?${qs}`),
    getCurrentStaff(),
  ]);

  const pageLink = (p: number) => {
    const next = new URLSearchParams(qs);
    next.set("page", String(p));
    next.delete("size");
    return `/admin/products?${next}`;
  };

  return (
    <>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <form className="flex gap-2" action="/admin/products">
          <input name="q" defaultValue={q} placeholder="Search title or SKU" className="admin-input w-64" />
          {status && <input type="hidden" name="status" value={status} />}
          <button className="admin-btn">Search</button>
        </form>
        <div className="flex gap-1">
          {STATUSES.map((s) => (
            <Link
              key={s.value}
              href={`/admin/products?${new URLSearchParams({ ...(q && { q }), ...(s.value && { status: s.value }) })}`}
              className={`rounded px-3 py-1.5 text-sm ${status === s.value ? "bg-stone-900 text-white" : "hover:bg-stone-200"}`}
            >
              {s.label}
            </Link>
          ))}
        </div>
        {can.editCatalog(me.role) && (
          <Link href="/admin/products/new" className="admin-btn-primary">
            Add product
          </Link>
        )}
      </div>

      <div className="admin-card overflow-x-auto p-0">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-stone-200 text-stone-500">
            <tr>
              <th className="p-3 font-normal">Product</th>
              <th className="p-3 font-normal">Status</th>
              <th className="p-3 font-normal">Category</th>
              <th className="p-3 text-right font-normal">Variants</th>
              <th className="p-3 text-right font-normal">In stock</th>
              <th className="p-3 text-right font-normal">Price</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-stone-100">
            {result.items.map((p) => (
              <tr key={p.id} className="hover:bg-stone-50">
                <td className="p-3">
                  <Link href={`/admin/products/${p.id}`} className="flex items-center gap-3 hover:underline">
                    {p.imageUrl ? (
                      // eslint-disable-next-line @next/next/no-img-element -- small admin thumbnail
                      <img src={p.imageUrl} alt="" className="h-12 w-10 rounded object-cover" />
                    ) : (
                      <span className="h-12 w-10 rounded bg-stone-100" />
                    )}
                    {p.title}
                  </Link>
                </td>
                <td className="p-3">
                  <StatusBadge status={p.status} />
                </td>
                <td className="p-3 text-stone-600">{p.categoryName ?? "—"}</td>
                <td className="p-3 text-right tabular-nums">{p.variantCount}</td>
                <td className="p-3 text-right tabular-nums">{p.totalOnHand}</td>
                <td className="p-3 text-right tabular-nums">
                  {p.priceMin == null
                    ? "—"
                    : p.priceMin === p.priceMax
                      ? formatPrice(p.priceMin, p.currency)
                      : `${formatPrice(p.priceMin, p.currency)} – ${formatPrice(p.priceMax, p.currency)}`}
                </td>
              </tr>
            ))}
            {result.items.length === 0 && (
              <tr>
                <td colSpan={6} className="p-8 text-center text-stone-500">
                  No products found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {result.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-end gap-3 text-sm">
          {page > 0 && <Link href={pageLink(page - 1)} className="admin-btn">Previous</Link>}
          <span className="text-stone-500">
            Page {page + 1} of {result.totalPages}
          </span>
          {page + 1 < result.totalPages && <Link href={pageLink(page + 1)} className="admin-btn">Next</Link>}
        </div>
      )}
    </>
  );
}

export default function ProductsPage({ searchParams }: PageProps<"/admin/products">) {
  return (
    <>
      <PageTitle>Products</PageTitle>
      <Suspense fallback={<Loading />}>
        <ProductTable searchParams={searchParams} />
      </Suspense>
    </>
  );
}
