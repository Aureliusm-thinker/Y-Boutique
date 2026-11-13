// Store currency is Kenyan shillings (KES); amounts are shown without cents, e.g. "Ksh 12,900".
export function formatPrice(amount: number | null, currency: string | null): string {
  if (amount == null) return "";
  const code = currency ?? "KES";
  return new Intl.NumberFormat("en-KE", {
    style: "currency",
    currency: code,
    maximumFractionDigits: code === "KES" ? 0 : 2,
    minimumFractionDigits: 0,
  }).format(amount);
}
