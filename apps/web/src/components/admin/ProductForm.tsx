"use client";

import { useRef, useState, useTransition } from "react";
import { saveProduct, uploadImage } from "@/app/admin/(panel)/products/actions";
import type { ActionResult, AdminCategory, AdminProductDetail, ProductUpsert } from "@/lib/admin/types";
import { ActionMessage } from "./ui";

type VariantRow = {
  key: string;
  id?: number;
  sku: string;
  size: string;
  colour: string;
  price: string;
  compareAtPrice: string;
  lowStockThreshold: string;
  openingStock: string;
  onHand?: number;
};

type ImageRow = { url: string; altText: string };

let rowCounter = 0;
const newKey = () => `row-${++rowCounter}`;

const emptyVariant = (): VariantRow => ({
  key: newKey(),
  sku: "",
  size: "",
  colour: "",
  price: "",
  compareAtPrice: "",
  lowStockThreshold: "3",
  openingStock: "0",
});

function toVariantRows(product: AdminProductDetail): VariantRow[] {
  return product.variants.map((v) => ({
    key: newKey(),
    id: v.id,
    sku: v.sku,
    size: v.size ?? "",
    colour: v.colour ?? "",
    price: String(v.price),
    compareAtPrice: v.compareAtPrice == null ? "" : String(v.compareAtPrice),
    lowStockThreshold: String(v.lowStockThreshold),
    openingStock: "0",
    onHand: v.onHand,
  }));
}

export function ProductForm({
  product,
  categories,
  readOnly = false,
}: {
  product?: AdminProductDetail;
  categories: AdminCategory[];
  readOnly?: boolean;
}) {
  const [title, setTitle] = useState(product?.title ?? "");
  const [slug, setSlug] = useState(product?.slug ?? "");
  const [description, setDescription] = useState(product?.description ?? "");
  const [categoryId, setCategoryId] = useState(product?.categoryId?.toString() ?? "");
  const [material, setMaterial] = useState(product?.material ?? "");
  const [fit, setFit] = useState(product?.fit ?? "");
  const [occasion, setOccasion] = useState(product?.occasion ?? "");
  const [season, setSeason] = useState(product?.season ?? "");
  const [styleTags, setStyleTags] = useState(product?.styleTags.join(", ") ?? "");
  const [seoTitle, setSeoTitle] = useState(product?.seoTitle ?? "");
  const [seoDescription, setSeoDescription] = useState(product?.seoDescription ?? "");
  const [variants, setVariants] = useState<VariantRow[]>(() =>
    product ? toVariantRows(product) : [emptyVariant()],
  );
  const [images, setImages] = useState<ImageRow[]>(
    product?.images.map((i) => ({ url: i.url, altText: i.altText ?? "" })) ?? [],
  );
  const [result, setResult] = useState<ActionResult | null>(null);
  const [uploadResult, setUploadResult] = useState<ActionResult | null>(null);
  const [saving, startSaving] = useTransition();
  const [uploading, startUploading] = useTransition();
  const fileInput = useRef<HTMLInputElement>(null);

  const updateVariant = (key: string, patch: Partial<VariantRow>) =>
    setVariants((rows) => rows.map((r) => (r.key === key ? { ...r, ...patch } : r)));

  const toNumber = (s: string) => (s.trim() === "" ? undefined : Number(s));

  function buildPayload(): ProductUpsert {
    return {
      title,
      slug: slug.trim() || undefined,
      description,
      categoryId: categoryId ? Number(categoryId) : null,
      material,
      fit,
      occasion,
      season,
      styleTags: styleTags.split(",").map((t) => t.trim()).filter(Boolean),
      seoTitle,
      seoDescription,
      variants: variants.map((v) => ({
        id: v.id,
        sku: v.sku,
        size: v.size,
        colour: v.colour,
        price: Number(v.price || 0),
        compareAtPrice: toNumber(v.compareAtPrice) ?? null,
        lowStockThreshold: toNumber(v.lowStockThreshold),
        openingStock: v.id ? undefined : toNumber(v.openingStock),
      })),
      images: images.map((i) => ({ url: i.url, altText: i.altText })),
    };
  }

  function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    startSaving(async () => {
      const res = await saveProduct(product?.id ?? null, buildPayload());
      // Reload rows from what the server saved, so new variants get their ids (no duplicates on re-save).
      if (res.product) {
        setVariants(toVariantRows(res.product));
        setSlug(res.product.slug);
      }
      setResult(res);
    });
  }

  function onUpload() {
    const file = fileInput.current?.files?.[0];
    if (!file) return;
    const data = new FormData();
    data.append("file", file);
    startUploading(async () => {
      const res = await uploadImage(data);
      setUploadResult(res.ok ? null : res);
      if (res.ok && res.url) setImages((imgs) => [...imgs, { url: res.url!, altText: title }]);
      if (fileInput.current) fileInput.current.value = "";
    });
  }

  const moveImage = (index: number, dir: -1 | 1) =>
    setImages((imgs) => {
      const next = [...imgs];
      const target = index + dir;
      if (target < 0 || target >= next.length) return imgs;
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });

  return (
    <form onSubmit={onSubmit} className="space-y-6">
      <fieldset disabled={readOnly || saving} className="space-y-6">
        <section className="admin-card space-y-4">
          <h2 className="font-medium">Details</h2>
          <label className="block text-sm">
            Title *
            <input value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={200} className="admin-input mt-1" />
          </label>
          <label className="block text-sm">
            URL slug <span className="text-stone-500">(leave empty to generate from the title)</span>
            <input
              value={slug}
              onChange={(e) => setSlug(e.target.value)}
              pattern="[a-z0-9]+(-[a-z0-9]+)*"
              title="lower-case letters, numbers and dashes"
              className="admin-input mt-1"
            />
          </label>
          <label className="block text-sm">
            Description
            <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={5} maxLength={5000} className="admin-input mt-1" />
          </label>
        </section>

        <section className="admin-card space-y-4">
          <div>
            <h2 className="font-medium">Attributes</h2>
            <p className="text-sm text-stone-500">
              Used for filters now and personalised recommendations later. Category and material are required to publish.
            </p>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block text-sm">
              Category
              <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)} className="admin-input mt-1">
                <option value="">— Choose —</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="block text-sm">
              Material
              <input value={material} onChange={(e) => setMaterial(e.target.value)} placeholder="e.g. Linen" maxLength={80} className="admin-input mt-1" />
            </label>
            <label className="block text-sm">
              Fit
              <input value={fit} onChange={(e) => setFit(e.target.value)} placeholder="Slim / Regular / Relaxed" maxLength={40} className="admin-input mt-1" />
            </label>
            <label className="block text-sm">
              Occasion
              <input value={occasion} onChange={(e) => setOccasion(e.target.value)} placeholder="Work, Evening, Wedding…" maxLength={80} className="admin-input mt-1" />
            </label>
            <label className="block text-sm">
              Season
              <input value={season} onChange={(e) => setSeason(e.target.value)} placeholder="Summer / All season" maxLength={40} className="admin-input mt-1" />
            </label>
            <label className="block text-sm">
              Style tags <span className="text-stone-500">(comma separated)</span>
              <input value={styleTags} onChange={(e) => setStyleTags(e.target.value)} placeholder="minimal, boho" className="admin-input mt-1" />
            </label>
          </div>
        </section>

        <section className="admin-card space-y-4">
          <h2 className="font-medium">Images</h2>
          {images.length === 0 && <p className="text-sm text-stone-500">No images yet. At least one is required to publish.</p>}
          <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {images.map((img, i) => (
              <li key={img.url + i} className="rounded border border-stone-200 p-2">
                {/* eslint-disable-next-line @next/next/no-img-element -- admin preview of any uploaded URL */}
                <img src={img.url} alt={img.altText} className="aspect-[4/5] w-full rounded object-cover" />
                <input
                  value={img.altText}
                  onChange={(e) => setImages((imgs) => imgs.map((x, j) => (j === i ? { ...x, altText: e.target.value } : x)))}
                  placeholder="Describe the image (for screen readers)"
                  maxLength={200}
                  className="admin-input mt-2"
                />
                <div className="mt-2 flex gap-1">
                  <button type="button" onClick={() => moveImage(i, -1)} className="admin-btn" aria-label="Move earlier">←</button>
                  <button type="button" onClick={() => moveImage(i, 1)} className="admin-btn" aria-label="Move later">→</button>
                  <button type="button" onClick={() => setImages((imgs) => imgs.filter((_, j) => j !== i))} className="admin-btn ml-auto text-red-700">
                    Remove
                  </button>
                </div>
              </li>
            ))}
          </ul>
          {!readOnly && (
            <div className="flex flex-wrap items-center gap-2">
              <input ref={fileInput} type="file" accept="image/jpeg,image/png,image/webp" className="text-sm" />
              <button type="button" onClick={onUpload} disabled={uploading} className="admin-btn">
                {uploading ? "Uploading…" : "Upload image"}
              </button>
              <span className="text-xs text-stone-500">JPEG, PNG or WebP, up to 10 MB. The first image is the main one.</span>
            </div>
          )}
          <ActionMessage result={uploadResult} />
        </section>

        <section className="admin-card space-y-4">
          <div>
            <h2 className="font-medium">Variants (sizes &amp; colours)</h2>
            <p className="text-sm text-stone-500">
              Each size/colour combination has its own SKU, price and stock. Change stock for existing variants
              in the Stock panel, so every change is recorded.
            </p>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-sm">
              <thead className="text-left text-stone-500">
                <tr>
                  <th className="pb-2 font-normal">SKU *</th>
                  <th className="pb-2 font-normal">Size</th>
                  <th className="pb-2 font-normal">Colour</th>
                  <th className="pb-2 font-normal">Price *</th>
                  <th className="pb-2 font-normal">Was price</th>
                  <th className="pb-2 font-normal">Low at</th>
                  <th className="pb-2 font-normal">Stock</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {variants.map((v) => (
                  <tr key={v.key}>
                    <td className="py-1 pr-2">
                      <input value={v.sku} onChange={(e) => updateVariant(v.key, { sku: e.target.value })} required pattern="[A-Za-z0-9_\-]+" maxLength={64} className="admin-input" />
                    </td>
                    <td className="py-1 pr-2">
                      <input value={v.size} onChange={(e) => updateVariant(v.key, { size: e.target.value })} maxLength={20} className="admin-input w-20" />
                    </td>
                    <td className="py-1 pr-2">
                      <input value={v.colour} onChange={(e) => updateVariant(v.key, { colour: e.target.value })} maxLength={40} className="admin-input w-28" />
                    </td>
                    <td className="py-1 pr-2">
                      <input value={v.price} onChange={(e) => updateVariant(v.key, { price: e.target.value })} required type="number" min="0" step="0.01" className="admin-input w-24" />
                    </td>
                    <td className="py-1 pr-2">
                      <input value={v.compareAtPrice} onChange={(e) => updateVariant(v.key, { compareAtPrice: e.target.value })} type="number" min="0" step="0.01" className="admin-input w-24" />
                    </td>
                    <td className="py-1 pr-2">
                      <input value={v.lowStockThreshold} onChange={(e) => updateVariant(v.key, { lowStockThreshold: e.target.value })} type="number" min="0" className="admin-input w-16" />
                    </td>
                    <td className="py-1 pr-2">
                      {v.id ? (
                        <span className="tabular-nums">{v.onHand}</span>
                      ) : (
                        <input
                          value={v.openingStock}
                          onChange={(e) => updateVariant(v.key, { openingStock: e.target.value })}
                          type="number"
                          min="0"
                          aria-label="Opening stock"
                          className="admin-input w-20"
                        />
                      )}
                    </td>
                    <td className="py-1 text-right">
                      <button
                        type="button"
                        onClick={() => setVariants((rows) => rows.filter((r) => r.key !== v.key))}
                        className="admin-btn text-red-700"
                        aria-label={`Remove variant ${v.sku}`}
                      >
                        ✕
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!readOnly && (
            <button type="button" onClick={() => setVariants((rows) => [...rows, emptyVariant()])} className="admin-btn">
              Add variant
            </button>
          )}
        </section>

        <section className="admin-card space-y-4">
          <h2 className="font-medium">Search engine listing</h2>
          <label className="block text-sm">
            SEO title
            <input value={seoTitle} onChange={(e) => setSeoTitle(e.target.value)} maxLength={200} className="admin-input mt-1" />
          </label>
          <label className="block text-sm">
            SEO description
            <textarea value={seoDescription} onChange={(e) => setSeoDescription(e.target.value)} maxLength={320} rows={2} className="admin-input mt-1" />
          </label>
        </section>
      </fieldset>

      {!readOnly && (
        <div className="sticky bottom-0 -mx-4 flex items-center gap-3 border-t border-stone-200 bg-stone-50/95 px-4 py-3">
          <button type="submit" disabled={saving} className="admin-btn-primary">
            {saving ? "Saving…" : product ? "Save changes" : "Create draft"}
          </button>
          <div className="flex-1">
            <ActionMessage result={result} />
          </div>
        </div>
      )}
    </form>
  );
}
