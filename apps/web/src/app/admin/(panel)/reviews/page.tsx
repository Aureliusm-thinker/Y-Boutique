import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { ModerateButtons } from "@/components/admin/ModerateButtons";
import { Loading, PageTitle } from "@/components/admin/ui";
import { adminFetch, getCurrentStaff } from "@/lib/admin/api";
import { can } from "@/lib/admin/types";

export const metadata: Metadata = { title: "Reviews" };

type Review = {
  id: number;
  productTitle: string;
  productSlug: string;
  authorName: string;
  rating: number;
  title: string | null;
  body: string | null;
  fit: string | null;
  sizeBought: string | null;
  status: string;
  createdAt: string;
};

async function Queue({ searchParams }: Pick<PageProps<"/admin/reviews">, "searchParams">) {
  const [me, sp] = await Promise.all([getCurrentStaff(), searchParams]);
  if (!can.moderateReviews(me.role)) return <p className="text-stone-600">You don&apos;t have access to reviews.</p>;
  const status = typeof sp.status === "string" ? sp.status : "PENDING";
  const reviews = await adminFetch<Review[]>(`/api/v1/admin/reviews?status=${status}`);
  return (
    <>
      <nav className="mb-4 flex gap-1 text-sm" aria-label="Review status">
        {[["PENDING", "Waiting"], ["APPROVED", "Published"], ["REJECTED", "Rejected"]].map(([s, label]) => (
          <Link key={s} href={`/admin/reviews?status=${s}`} aria-current={status === s ? "page" : undefined}
            className="rounded px-3 py-1.5 hover:bg-stone-200 aria-[current=page]:bg-stone-900 aria-[current=page]:text-white">
            {label}
          </Link>
        ))}
      </nav>
      {reviews.length === 0 && <p className="admin-card text-sm text-stone-500">Nothing here.</p>}
      <ul className="space-y-4">
        {reviews.map((r) => (
          <li key={r.id} className="admin-card text-sm">
            <div className="flex flex-wrap justify-between gap-2">
              <p>
                <span aria-label={`${r.rating} stars`}>{"★".repeat(r.rating)}<span className="text-stone-300">{"★".repeat(5 - r.rating)}</span></span>{" "}
                on <Link href={`/products/${r.productSlug}`} className="underline">{r.productTitle}</Link>
              </p>
              <p className="text-stone-500">
                {r.authorName} · {new Date(r.createdAt).toLocaleDateString("en-KE", { dateStyle: "medium" })}
              </p>
            </div>
            {r.title && <p className="mt-2 font-medium">{r.title}</p>}
            {r.body && <p className="mt-1 text-stone-700">{r.body}</p>}
            <p className="mt-1 text-xs text-stone-500">
              {r.sizeBought && `Bought ${r.sizeBought}`} {r.fit && `· ${r.fit.replaceAll("_", " ").toLowerCase()}`}
            </p>
            {r.status === "PENDING" && <ModerateButtons id={r.id} />}
          </li>
        ))}
      </ul>
    </>
  );
}

export default function ReviewsPage({ searchParams }: PageProps<"/admin/reviews">) {
  return (
    <>
      <PageTitle>Reviews</PageTitle>
      <Suspense fallback={<Loading />}>
        <Queue searchParams={searchParams} />
      </Suspense>
    </>
  );
}
