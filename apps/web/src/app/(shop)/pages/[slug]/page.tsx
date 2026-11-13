import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { Markdown } from "@/components/Markdown";
import { getContentPage } from "@/lib/api";

export async function generateMetadata({ params }: PageProps<"/pages/[slug]">): Promise<Metadata> {
  const { slug } = await params;
  const page = await getContentPage(slug);
  return page ? { title: page.title, alternates: { canonical: `/pages/${slug}` } } : { title: "Not found" };
}

async function Content({ params }: Pick<PageProps<"/pages/[slug]">, "params">) {
  const { slug } = await params;
  const page = await getContentPage(slug);
  if (!page) notFound();
  return (
    <article>
      <h1 className="mb-6 font-serif text-4xl">{page.title}</h1>
      <Markdown source={page.body} />
      {page.publishedAt && (
        <p className="mt-10 text-xs text-stone-500">
          Last updated {new Date(page.publishedAt).toLocaleDateString("en-KE", { dateStyle: "long" })}
        </p>
      )}
    </article>
  );
}

export default function ContentPageRoute({ params }: PageProps<"/pages/[slug]">) {
  return (
    <div className="mx-auto max-w-2xl px-4 py-12">
      <Suspense fallback={<div className="h-64 animate-pulse bg-stone-100" />}>
        <Content params={params} />
      </Suspense>
    </div>
  );
}
