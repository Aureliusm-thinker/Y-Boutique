import type { Metadata } from "next";
import { Suspense } from "react";
import { CategoryForm } from "@/components/admin/CategoryForm";
import { Loading, PageTitle } from "@/components/admin/ui";
import { adminFetch, getCurrentStaff } from "@/lib/admin/api";
import { can, type AdminCategory } from "@/lib/admin/types";

export const metadata: Metadata = { title: "Categories" };

async function Categories() {
  const [me, categories] = await Promise.all([
    getCurrentStaff(),
    adminFetch<AdminCategory[]>("/api/v1/admin/categories"),
  ]);
  const canEdit = can.editCatalog(me.role);
  return (
    <div className="space-y-6">
      {canEdit && (
        <section className="admin-card">
          <h2 className="mb-3 font-medium">Add a category</h2>
          <CategoryForm />
        </section>
      )}
      <section className="admin-card space-y-4">
        <h2 className="font-medium">Categories ({categories.length})</h2>
        <p className="text-sm text-stone-500">Shown in the shop navigation in this order.</p>
        <ul className="divide-y divide-stone-100">
          {categories.map((c) => (
            <li key={c.id} className="py-3">
              <CategoryForm category={c} disabled={!canEdit} />
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

export default function CategoriesPage() {
  return (
    <>
      <PageTitle>Categories</PageTitle>
      <Suspense fallback={<Loading />}>
        <Categories />
      </Suspense>
    </>
  );
}
