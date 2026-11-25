"use client";

import Link from "next/link";
import { useRef, useState, useTransition } from "react";
import { importProducts, type ImportResult } from "@/app/admin/(panel)/products/actions";
import { ActionMessage } from "@/components/admin/ui";
import type { ActionResult } from "@/lib/admin/types";

const plural = (n: number, word: string) => `${n} ${word}${n === 1 ? "" : "s"}`;

/** Pick a CSV → preview what would change → confirm to save. */
export function ProductImportForm() {
  const fileRef = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<ImportResult | null>(null);
  const [problem, setProblem] = useState<ActionResult | null>(null);
  const [pending, startTransition] = useTransition();

  function run(apply: boolean) {
    const file = fileRef.current?.files?.[0];
    if (!file) {
      setProblem({ ok: false, message: "Choose a CSV file first." });
      return;
    }
    const data = new FormData();
    data.set("file", file);
    data.set("apply", String(apply));
    startTransition(async () => {
      const res = await importProducts(data);
      setProblem(res.result ? null : res);
      setPreview(res.result ?? null);
    });
  }

  const result = preview;
  const nothingToDo =
    result && result.errors.length === 0 && result.productsCreated + result.productsUpdated === 0;

  return (
    <div className="space-y-6">
      <div className="admin-card flex flex-wrap items-center gap-3">
        <input
          ref={fileRef}
          type="file"
          accept=".csv,text/csv"
          aria-label="Product CSV file"
          className="text-sm"
          onChange={() => {
            setPreview(null);
            setProblem(null);
          }}
        />
        <button type="button" className="admin-btn" disabled={pending} onClick={() => run(false)}>
          {pending ? "Checking…" : "Check file"}
        </button>
      </div>

      <ActionMessage result={problem} />

      {result && result.errors.length > 0 && (
        <div role="alert" className="admin-card border-red-200 bg-red-50 text-sm text-red-800">
          <p className="font-medium">
            Nothing was saved. Fix {result.errors.length === 1 ? "this problem" : `these ${result.errors.length} problems`} and
            check the file again:
          </p>
          <ul className="mt-2 space-y-1">
            {result.errors.map((e, i) => (
              <li key={i}>
                {e.line > 0 && <span className="mr-2 font-mono text-xs">Line {e.line}</span>}
                {e.message}
              </li>
            ))}
          </ul>
        </div>
      )}

      {result && result.errors.length === 0 && (
        <div className="admin-card space-y-4 text-sm">
          <p role="status" className={result.applied ? "font-medium text-green-800" : "font-medium"}>
            {result.applied ? "Saved. " : "Preview: "}
            {plural(result.productsCreated, "new product")}, {result.productsUpdated} updated ·{" "}
            {plural(result.variantsCreated, "new SKU")}, {plural(result.variantsUpdated, "SKU")} changed
            <span className="text-stone-500"> ({plural(result.rows, "row")} read)</span>
          </p>

          {result.products.length > 0 && (
            <table className="w-full text-left">
              <thead className="border-b border-stone-200 text-stone-500">
                <tr>
                  <th className="py-2 font-normal">Product</th>
                  <th className="py-2 font-normal">What changes</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-100">
                {result.products.map((p, i) => (
                  <tr key={i}>
                    <td className="py-2 pr-4">{p.title}</td>
                    <td className="py-2 text-stone-600">
                      {[
                        p.created ? "New draft product" : null,
                        p.changedFields.length > 0 ? `Updates ${p.changedFields.join(", ")}` : null,
                        p.newVariants > 0 ? plural(p.newVariants, "new SKU") : null,
                        p.updatedVariants > 0 ? `${plural(p.updatedVariants, "SKU")} changed` : null,
                      ]
                        .filter(Boolean)
                        .join(" · ")}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {result.notes.length > 0 && (
            <ul className="list-disc pl-5 text-stone-600">
              {result.notes.map((n) => (
                <li key={n}>{n}</li>
              ))}
            </ul>
          )}

          {nothingToDo && !result.applied && <p className="text-stone-600">Everything in this file already matches the shop.</p>}

          {!result.applied && !nothingToDo && (
            <button type="button" className="admin-btn-primary" disabled={pending} onClick={() => run(true)}>
              {pending ? "Saving…" : "Save these changes"}
            </button>
          )}
          {result.applied && (
            <Link href="/admin/products?status=DRAFT" className="underline">
              See draft products
            </Link>
          )}
        </div>
      )}
    </div>
  );
}
