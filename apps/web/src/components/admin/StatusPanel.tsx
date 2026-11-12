"use client";

import { useState, useTransition } from "react";
import { changeStatus } from "@/app/admin/(panel)/products/actions";
import type { ActionResult, AdminProductDetail } from "@/lib/admin/types";
import { ActionMessage, StatusBadge } from "./ui";

export function StatusPanel({ product, canEdit }: { product: AdminProductDetail; canEdit: boolean }) {
  const [result, setResult] = useState<ActionResult | null>(null);
  const [pending, start] = useTransition();
  const run = (action: "publish" | "unpublish" | "archive") =>
    start(async () => setResult(await changeStatus(product.id, action)));

  return (
    <section className="admin-card space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="font-medium">Status</h2>
        <StatusBadge status={product.status} />
      </div>

      {product.status !== "ACTIVE" && product.publishProblems.length > 0 && (
        <div className="text-sm">
          <p className="text-stone-600">Before publishing:</p>
          <ul className="mt-1 list-disc pl-5 text-amber-800">
            {product.publishProblems.map((p) => (
              <li key={p}>{p}</li>
            ))}
          </ul>
        </div>
      )}
      {product.status === "ACTIVE" && (
        <a href={`/products/${product.slug}`} target="_blank" className="block text-sm underline underline-offset-4">
          View in shop ↗
        </a>
      )}

      {canEdit && (
        <div className="flex flex-wrap gap-2">
          {product.status !== "ACTIVE" && (
            <button
              onClick={() => run("publish")}
              disabled={pending || product.publishProblems.length > 0}
              className="admin-btn-primary"
            >
              Publish
            </button>
          )}
          {product.status === "ACTIVE" && (
            <button onClick={() => run("unpublish")} disabled={pending} className="admin-btn">
              Unpublish
            </button>
          )}
          {product.status !== "ARCHIVED" && (
            <button
              onClick={() => confirm("Archive this product? It will be hidden from the shop.") && run("archive")}
              disabled={pending}
              className="admin-btn text-red-700"
            >
              Archive
            </button>
          )}
        </div>
      )}
      <ActionMessage result={result} />
    </section>
  );
}
