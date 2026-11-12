"use client";

import { useActionState } from "react";
import { saveCategory } from "@/app/admin/(panel)/categories/actions";
import type { ActionResult, AdminCategory } from "@/lib/admin/types";
import { ActionMessage } from "./ui";

/** Inline create (no category) or edit (with category) form. */
export function CategoryForm({ category, disabled }: { category?: AdminCategory; disabled?: boolean }) {
  const [result, action, pending] = useActionState<ActionResult | null, FormData>(saveCategory, null);
  return (
    <form action={action} className="flex flex-wrap items-end gap-2">
      {category && <input type="hidden" name="id" value={category.id} />}
      <label className="text-sm">
        Name
        <input name="name" defaultValue={category?.name} required maxLength={120} disabled={disabled} className="admin-input mt-1 w-48" />
      </label>
      <label className="text-sm">
        Slug
        <input
          name="slug"
          defaultValue={category?.slug}
          placeholder={category ? undefined : "auto"}
          pattern="[a-z0-9]+(-[a-z0-9]+)*"
          maxLength={120}
          disabled={disabled}
          className="admin-input mt-1 w-40"
        />
      </label>
      <label className="text-sm">
        Order
        <input name="position" type="number" min="0" defaultValue={category?.position ?? 0} disabled={disabled} className="admin-input mt-1 w-20" />
      </label>
      {!disabled && (
        <button disabled={pending} className={category ? "admin-btn" : "admin-btn-primary"}>
          {category ? "Save" : "Add category"}
        </button>
      )}
      {category && <span className="pb-2 text-sm text-stone-500">{category.productCount} products</span>}
      <div className="basis-full">
        <ActionMessage result={result} />
      </div>
    </form>
  );
}
