import type { Metadata } from "next";
import { Suspense } from "react";
import { ProductForm } from "@/components/admin/ProductForm";
import { Loading, PageTitle } from "@/components/admin/ui";
import { adminFetch, getCurrentStaff } from "@/lib/admin/api";
import { can, type AdminCategory } from "@/lib/admin/types";

export const metadata: Metadata = { title: "New product" };

async function NewProduct() {
  const [me, categories] = await Promise.all([
    getCurrentStaff(),
    adminFetch<AdminCategory[]>("/api/v1/admin/categories"),
  ]);
  if (!can.editCatalog(me.role)) return <p className="text-stone-600">You don&apos;t have permission to add products.</p>;
  return <ProductForm categories={categories} />;
}

export default function NewProductPage() {
  return (
    <>
      <PageTitle>New product</PageTitle>
      <p className="-mt-4 mb-6 text-sm text-stone-500">
        New products start as drafts. Shoppers only see them after you publish.
      </p>
      <Suspense fallback={<Loading />}>
        <NewProduct />
      </Suspense>
    </>
  );
}
