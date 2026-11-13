import type { ActionResult, ProductStatus } from "@/lib/admin/types";

export function PageTitle({ children, actions }: { children: React.ReactNode; actions?: React.ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
      <h1 className="font-serif text-3xl">{children}</h1>
      {actions}
    </div>
  );
}

export function StatusBadge({ status }: { status: ProductStatus }) {
  const styles: Record<ProductStatus, string> = {
    ACTIVE: "bg-green-100 text-green-800",
    DRAFT: "bg-amber-100 text-amber-800",
    ARCHIVED: "bg-stone-200 text-stone-600",
  };
  const label: Record<ProductStatus, string> = { ACTIVE: "Live", DRAFT: "Draft", ARCHIVED: "Archived" };
  return <span className={`rounded-full px-2 py-0.5 text-xs ${styles[status]}`}>{label[status]}</span>;
}

/** Shows the outcome of a server action: success message or error + list of problems. */
export function ActionMessage({ result }: { result: ActionResult | null }) {
  if (!result || (result.ok && !result.message)) return null;
  return (
    <div
      role={result.ok ? "status" : "alert"}
      className={`rounded border px-3 py-2 text-sm ${result.ok ? "border-green-200 bg-green-50 text-green-800" : "border-red-200 bg-red-50 text-red-800"}`}
    >
      {result.message}
      {result.errors && result.errors.length > 0 && (
        <ul className="mt-1 list-disc pl-5">
          {result.errors.map((e) => (
            <li key={e}>{e}</li>
          ))}
        </ul>
      )}
    </div>
  );
}

export function Loading() {
  return <p className="text-sm text-stone-500">Loading…</p>;
}

export const orderStatusStyle: Record<string, string> = {
  PENDING_PAYMENT: "bg-stone-100 text-stone-700",
  PAID: "bg-amber-100 text-amber-900",
  SHIPPED: "bg-blue-100 text-blue-900",
  DELIVERED: "bg-green-100 text-green-800",
  CANCELLED: "bg-stone-200 text-stone-500",
};

export function OrderStatusBadge({ status }: { status: string }) {
  return (
    <span className={`rounded-full px-2 py-0.5 text-xs ${orderStatusStyle[status] ?? "bg-stone-100"}`}>
      {status.replace("_", " ").toLowerCase()}
    </span>
  );
}
