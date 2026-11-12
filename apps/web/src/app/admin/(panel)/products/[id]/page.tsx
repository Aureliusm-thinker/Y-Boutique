import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { ProductForm } from "@/components/admin/ProductForm";
import { StatusPanel } from "@/components/admin/StatusPanel";
import { StockAdjuster } from "@/components/admin/StockAdjuster";
import { Loading } from "@/components/admin/ui";
import { adminFetch, ApiError, getCurrentStaff } from "@/lib/admin/api";
import { can, type AdminCategory, type AdminProductDetail } from "@/lib/admin/types";

async function EditProduct({ params, searchParams }: PageProps<"/admin/products/[id]">) {
  const [{ id }, sp] = await Promise.all([params, searchParams]);
  if (!/^\d+$/.test(id)) notFound();

  let product: AdminProductDetail;
  try {
    product = await adminFetch<AdminProductDetail>(`/api/v1/admin/products/${id}`);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }
  const [me, categories] = await Promise.all([
    getCurrentStaff(),
    adminFetch<AdminCategory[]>("/api/v1/admin/categories"),
  ]);
  const canEdit = can.editCatalog(me.role);

  return (
    <>
      <div className="mb-6">
        <Link href="/admin/products" className="text-sm text-stone-500 hover:underline">
          ← Products
        </Link>
        <h1 className="mt-1 font-serif text-3xl">{product.title}</h1>
        {sp.created && <p className="mt-2 text-sm text-green-800">Draft created. Add the remaining details, then publish.</p>}
      </div>
      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <ProductForm product={product} categories={categories} readOnly={!canEdit} />
        <aside className="space-y-6 lg:sticky lg:top-6 lg:self-start">
          <StatusPanel product={product} canEdit={canEdit} />
          <section className="admin-card">
            <h2 className="mb-3 font-medium">Stock</h2>
            {canEdit ? (
              <StockAdjuster
                variants={product.variants.map((v) => ({
                  id: v.id,
                  label: [v.size, v.colour].filter(Boolean).join(" / ") || v.sku,
                  onHand: v.onHand,
                  available: v.available,
                  low: v.low,
                }))}
              />
            ) : (
              <ul className="text-sm">
                {product.variants.map((v) => (
                  <li key={v.id}>
                    {v.sku}: {v.onHand} on hand
                  </li>
                ))}
              </ul>
            )}
          </section>
        </aside>
      </div>
    </>
  );
}

export default function EditProductPage(props: PageProps<"/admin/products/[id]">) {
  return (
    <Suspense fallback={<Loading />}>
      <EditProduct {...props} />
    </Suspense>
  );
}
