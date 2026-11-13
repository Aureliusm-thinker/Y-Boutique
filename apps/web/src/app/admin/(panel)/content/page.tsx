import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { AnnouncementEditor, HomeEditor, NewPageForm } from "@/components/admin/ContentEditors";
import { Loading, PageTitle } from "@/components/admin/ui";
import { adminFetch, getCurrentStaff } from "@/lib/admin/api";
import { can, type AdminCategory } from "@/lib/admin/types";
import type { HomeContent } from "@/lib/api";

export const metadata: Metadata = { title: "Content" };

type AdminPage = { slug: string; title: string; published: boolean; hasUnpublishedChanges: boolean; updatedAt: string };
type SiteBlock = { key: string; draft: Record<string, unknown>; hasUnpublishedChanges: boolean };

async function Content() {
  const me = await getCurrentStaff();
  if (!can.marketing(me.role)) return <p className="text-stone-600">You don&apos;t have access to content.</p>;
  const [pages, site, categories] = await Promise.all([
    adminFetch<AdminPage[]>("/api/v1/admin/content/pages"),
    adminFetch<SiteBlock[]>("/api/v1/admin/content/site"),
    adminFetch<AdminCategory[]>("/api/v1/admin/categories"),
  ]);
  const home = site.find((b) => b.key === "home");
  const bar = site.find((b) => b.key === "announcement");
  const unpublished = <span className="ml-2 rounded-full bg-amber-100 px-2 py-0.5 text-xs text-amber-900">unpublished changes</span>;
  return (
    <div className="space-y-6">
      <section className="admin-card">
        <h2 className="mb-4 font-medium">Home page {home?.hasUnpublishedChanges && unpublished}</h2>
        <HomeEditor draft={(home?.draft ?? {}) as HomeContent} categories={categories} />
      </section>
      <section className="admin-card">
        <h2 className="mb-4 font-medium">Announcement bar {bar?.hasUnpublishedChanges && unpublished}</h2>
        <AnnouncementEditor draft={(bar?.draft ?? {}) as { text?: string; link?: string; enabled?: boolean }} />
      </section>
      <section className="admin-card">
        <h2 className="mb-3 font-medium">Pages</h2>
        <ul className="mb-6 divide-y divide-stone-100 text-sm">
          {pages.map((p) => (
            <li key={p.slug} className="flex items-center justify-between py-2">
              <Link href={`/admin/content/pages/${p.slug}`} className="hover:underline">
                {p.title} <span className="text-stone-500">/pages/{p.slug}</span>
              </Link>
              <span>
                {!p.published && <span className="text-xs text-stone-500">draft</span>}
                {p.published && p.hasUnpublishedChanges && unpublished}
              </span>
            </li>
          ))}
        </ul>
        <NewPageForm />
      </section>
    </div>
  );
}

export default function ContentPage() {
  return (
    <>
      <PageTitle>Content</PageTitle>
      <Suspense fallback={<Loading />}>
        <Content />
      </Suspense>
    </>
  );
}
