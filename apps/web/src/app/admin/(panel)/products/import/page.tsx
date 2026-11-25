import type { Metadata } from "next";
import Link from "next/link";
import { PageTitle } from "@/components/admin/ui";
import { ProductImportForm } from "@/components/admin/ProductImportForm";

export const metadata: Metadata = { title: "Import products" };

const COLUMNS: [string, string][] = [
  ["slug", "The product's web address, e.g. linen-wrap-dress. An existing slug updates that product; leave empty for a new one."],
  ["title", "Product name (needed for new products)."],
  ["category", "Category name or slug, e.g. Dresses."],
  ["description, material, fit, occasion, season", "Product details."],
  ["style_tags", "Separated by |, e.g. boho | minimal."],
  ["seo_title, seo_description", "Search engine text."],
  ["image_urls", "Image links separated by |. Upload photos on the product page to get links."],
  ["sku", "Required. One row per SKU (size/colour combination)."],
  ["size, colour", "The variant's size and colour."],
  ["price, compare_at_price", "In KSh, e.g. 3500 or KSh 3,500. compare_at_price shows a sale and must be higher than price."],
  ["low_stock_threshold", "Warn when stock falls to this number."],
  ["stock_on_hand", "Opening stock for new SKUs only. Change stock for existing SKUs in Inventory."],
];

export default function ImportProductsPage() {
  return (
    <div className="max-w-4xl">
      <PageTitle
        actions={
          <Link href="/admin/products" className="text-sm underline">
            Back to products
          </Link>
        }
      >
        Import products
      </PageTitle>

      <div className="admin-card mb-6 space-y-3 text-sm text-stone-700">
        <p>
          Easiest start:{" "}
          {/* A file download from a route handler, not a page. */}
          <a href="/admin/products/export" download className="underline">
            export your products
          </a>
          , edit the file
          in Excel or Google Sheets, and import it again. Save it as CSV.
        </p>
        <ul className="list-disc space-y-1 pl-5">
          <li>You&apos;ll see a preview first. Nothing is saved until you confirm.</li>
          <li>If any row has a problem, nothing is saved. Fix the lines listed and try again.</li>
          <li>New products are saved as drafts. Products and SKUs missing from the file are never deleted.</li>
          <li>A column left out of the file keeps its current value. A column that&apos;s there but empty clears it.</li>
        </ul>
        <details>
          <summary className="cursor-pointer font-medium">Columns</summary>
          <dl className="mt-2 grid gap-x-4 gap-y-1 sm:grid-cols-[14rem_1fr]">
            {COLUMNS.map(([name, help]) => (
              <div key={name} className="contents">
                <dt className="font-mono text-xs leading-5">{name}</dt>
                <dd>{help}</dd>
              </div>
            ))}
          </dl>
        </details>
      </div>

      <ProductImportForm />
    </div>
  );
}
