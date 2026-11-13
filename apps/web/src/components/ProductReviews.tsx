import type { ProductReviews as Reviews } from "@/lib/api";

export function Stars({ rating, label }: { rating: number; label?: string }) {
  const rounded = Math.round(rating);
  return (
    <span aria-label={label ?? `${rating} out of 5 stars`} role="img" className="tracking-wider text-stone-900">
      {"★".repeat(rounded)}
      <span className="text-stone-300" aria-hidden>{"★".repeat(5 - rounded)}</span>
    </span>
  );
}

const FIT_LABEL: Record<string, string> = { RUNS_SMALL: "Runs small", TRUE_TO_SIZE: "True to size", RUNS_LARGE: "Runs large" };

/** Fit feedback summary: what most buyers said about the size (feeds size advice later, docs/04). */
export function FitSummary({ fit }: { fit: Reviews["fit"] }) {
  const total = fit.runsSmall + fit.trueToSize + fit.runsLarge;
  if (total === 0) return null;
  const rows = [
    ["Runs small", fit.runsSmall],
    ["True to size", fit.trueToSize],
    ["Runs large", fit.runsLarge],
  ] as const;
  const top = [...rows].sort((a, b) => b[1] - a[1])[0];
  return (
    <div className="text-sm">
      <p>
        <strong>Fit:</strong> most buyers say it {top[0] === "True to size" ? "is true to size" : top[0].toLowerCase()}.
      </p>
      <dl className="mt-2 space-y-1">
        {rows.map(([label, n]) => (
          <div key={label} className="flex items-center gap-3">
            <dt className="w-24 text-stone-500">{label}</dt>
            <dd className="h-1.5 flex-1 bg-stone-100">
              <div className="h-full bg-stone-900" style={{ width: `${(n / total) * 100}%` }} />
            </dd>
          </div>
        ))}
      </dl>
    </div>
  );
}

export function ProductReviewList({ data }: { data: Reviews }) {
  return (
    <section className="mt-20" aria-labelledby="reviews-heading">
      <h2 id="reviews-heading" className="mb-2 font-serif text-2xl">
        Reviews
      </h2>
      {data.count === 0 ? (
        <p className="text-sm text-stone-500">No reviews yet. Bought this? You can review it from your order page.</p>
      ) : (
        <div className="grid gap-10 md:grid-cols-[16rem_1fr]">
          <div className="space-y-4">
            <p className="text-3xl">
              {data.average?.toFixed(1)} <span className="text-base text-stone-500">/ 5</span>
            </p>
            <Stars rating={data.average ?? 0} />
            <p className="text-sm text-stone-500">
              {data.count} {data.count === 1 ? "review" : "reviews"}
            </p>
            <FitSummary fit={data.fit} />
          </div>
          <ul className="divide-y divide-stone-200">
            {data.reviews.map((r) => (
              <li key={r.id} className="py-5 text-sm">
                <div className="flex items-center justify-between">
                  <Stars rating={r.rating} />
                  <span className="text-stone-500">{new Date(r.createdAt).toLocaleDateString("en-KE", { dateStyle: "medium" })}</span>
                </div>
                {r.title && <p className="mt-2 font-medium">{r.title}</p>}
                {r.body && <p className="mt-1 text-stone-700">{r.body}</p>}
                <p className="mt-2 text-xs text-stone-500">
                  {r.authorName}
                  {r.verifiedBuyer && " · Verified buyer"}
                  {r.sizeBought && ` · Bought size ${r.sizeBought}`}
                  {r.fit && ` · ${FIT_LABEL[r.fit]}`}
                </p>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
