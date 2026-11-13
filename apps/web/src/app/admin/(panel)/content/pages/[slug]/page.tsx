import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { PageEditor } from "@/components/admin/ContentEditors";
import { Loading } from "@/components/admin/ui";
import { adminFetch, ApiError } from "@/lib/admin/api";

export const metadata: Metadata = { title: "Edit page" };

type AdminPage = {
  slug: string;
  title: string;
  body: string;
  draftTitle: string | null;
  draftBody: string | null;
  published: boolean;
  hasUnpublishedChanges: boolean;
};

async function Editor({ params }: Pick<PageProps<"/admin/content/pages/[slug]">, "params">) {
  const { slug } = await params;
  let page: AdminPage;
  try {
    page = await adminFetch<AdminPage>(`/api/v1/admin/content/pages/${encodeURIComponent(slug)}`);
  } catch (e) {
    if (e instanceof ApiError && (e.status === 404 || e.status === 403)) notFound();
    throw e;
  }
  return (
    <>
      <Link href="/admin/content" className="text-sm text-stone-500 hover:underline">
        ← Content
      </Link>
      <h1 className="mb-1 mt-1 font-serif text-3xl">{page.title}</h1>
      <p className="mb-6 text-sm text-stone-500">
        {page.published ? (
          <>
            Live at{" "}
            <a href={`/pages/${page.slug}`} target="_blank" className="underline">
              /pages/{page.slug}
            </a>
            {page.hasUnpublishedChanges && " · you have unpublished changes"}
          </>
        ) : (
          "Not published yet"
        )}
      </p>
      <PageEditor slug={page.slug} title={page.draftTitle ?? page.title} body={page.draftBody ?? page.body} published={page.published} />
    </>
  );
}

export default function EditContentPage({ params }: PageProps<"/admin/content/pages/[slug]">) {
  return (
    <Suspense fallback={<Loading />}>
      <Editor params={params} />
    </Suspense>
  );
}
