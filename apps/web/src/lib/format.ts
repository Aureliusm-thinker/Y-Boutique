export function formatPrice(amount: number | null, currency: string | null): string {
  if (amount == null) return "";
  return new Intl.NumberFormat("en", {
    style: "currency",
    currency: currency ?? "USD",
  }).format(amount);
}
