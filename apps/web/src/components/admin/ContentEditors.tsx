"use client";

import { useActionState, useState, useTransition } from "react";
import { createPage, publishPage, publishSite, savePageDraft, saveSiteDraft } from "@/app/admin/(panel)/content/actions";
import { Hero } from "@/components/Hero";
import { Markdown } from "@/components/Markdown";
import type { ActionResult } from "@/lib/admin/types";
import type { HomeContent } from "@/lib/api";
import { ActionMessage } from "./ui";

function PublishButton({ onPublish, label }: { onPublish: () => Promise<ActionResult>; label: string }) {
  const [result, setResult] = useState<ActionResult | null>(null);
  const [pending, start] = useTransition();
  return (
    <span className="inline-flex items-center gap-2">
      <button type="button" disabled={pending} onClick={() => start(async () => setResult(await onPublish()))} className="admin-btn-primary">
        {label}
      </button>
      <ActionMessage result={result} />
    </span>
  );
}

/** Edit a page's draft with a live preview; publish when happy (FR-AD-10). */
export function PageEditor({ slug, title, body, published }: { slug: string; title: string; body: string; published: boolean }) {
  const [result, action, pending] = useActionState<ActionResult | null, FormData>(savePageDraft.bind(null, slug), null);
  const [draftTitle, setTitle] = useState(title);
  const [draftBody, setBody] = useState(body);
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <form action={action} className="admin-card space-y-3 text-sm">
        <label className="block">
          Title
          <input name="title" value={draftTitle} onChange={(e) => setTitle(e.target.value)} required className="admin-input mt-1" />
        </label>
        <label className="block">
          Text
          <textarea name="body" value={draftBody} onChange={(e) => setBody(e.target.value)} rows={22} className="admin-input mt-1 font-mono text-xs" />
        </label>
        <p className="text-xs text-stone-500">
          Formatting: start a line with <code>## </code> for a heading, <code>- </code> for a bullet, wrap words in{" "}
          <code>**bold**</code>, links as <code>[text](/pages/returns)</code>. Leave a blank line between paragraphs.
        </p>
        <div className="flex flex-wrap items-center gap-2">
          <button disabled={pending} className="admin-btn">Save draft</button>
          <PublishButton label="Publish" onPublish={async () => {
            const fd = new FormData();
            fd.set("title", draftTitle);
            fd.set("body", draftBody);
            const saved = await savePageDraft(slug, null, fd);
            return saved.ok ? publishPage(slug, true) : saved;
          }} />
          {published && <PublishButton label="Unpublish" onPublish={() => publishPage(slug, false)} />}
        </div>
        <ActionMessage result={result} />
      </form>
      <div className="admin-card">
        <p className="mb-4 text-xs uppercase tracking-widest text-stone-500">Preview</p>
        <h1 className="mb-6 font-serif text-4xl">{draftTitle}</h1>
        <Markdown source={draftBody} />
      </div>
    </div>
  );
}

export function NewPageForm() {
  const [result, action, pending] = useActionState<ActionResult | null, FormData>(createPage, null);
  return (
    <form action={action} className="flex flex-wrap items-end gap-2 text-sm">
      <label>
        Title
        <input name="title" required className="admin-input mt-1" />
      </label>
      <label>
        Address <span className="text-stone-500">(/pages/…)</span>
        <input name="slug" required pattern="[a-z0-9]+(-[a-z0-9]+)*" placeholder="about-us" className="admin-input mt-1" />
      </label>
      <button disabled={pending} className="admin-btn">Create page</button>
      <ActionMessage result={result} />
    </form>
  );
}

export function HomeEditor({ draft, categories }: { draft: HomeContent; categories: { slug: string; name: string }[] }) {
  const [result, action, pending] = useActionState<ActionResult | null, FormData>(saveSiteDraft.bind(null, "home"), null);
  const [preview, setPreview] = useState<HomeContent>(draft);
  const field = (name: keyof HomeContent, label: string, hint?: string) => (
    <label className="block">
      {label} {hint && <span className="text-stone-500">({hint})</span>}
      <input
        name={name}
        defaultValue={(draft[name] as string | null | undefined) ?? ""}
        onChange={(e) => setPreview((p) => ({ ...p, [name]: e.target.value }))}
        className="admin-input mt-1"
      />
    </label>
  );
  return (
    <div className="space-y-4">
      <form action={action} className="grid gap-3 text-sm sm:grid-cols-2">
        {field("heroEyebrow", "Small heading")}
        {field("heroTitle", "Headline")}
        {field("heroText", "Text")}
        {field("heroCta", "Button label")}
        {field("heroLink", "Button link", "e.g. /shop?category=dresses")}
        {field("heroImage", "Background image URL", "optional — upload under Products first")}
        <label className="block">
          Featured products
          <select name="featuredCategory" defaultValue={draft.featuredCategory ?? ""} className="admin-input mt-1">
            <option value="">Newest from all categories</option>
            {categories.map((c) => (
              <option key={c.slug} value={c.slug}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
        {field("featuredTitle", "Featured section title")}
        <div className="flex flex-wrap items-center gap-2 sm:col-span-2">
          <button disabled={pending} className="admin-btn">Save draft</button>
          <PublishButton label="Publish home page" onPublish={() => publishSite("home")} />
          <ActionMessage result={result} />
        </div>
      </form>
      <div className="overflow-hidden rounded border border-stone-200">
        <p className="bg-stone-100 px-3 py-1 text-xs uppercase tracking-widest text-stone-500">Preview</p>
        <Hero home={preview} preview />
      </div>
    </div>
  );
}

export function AnnouncementEditor({ draft }: { draft: { text?: string; link?: string | null; enabled?: boolean } }) {
  const [result, action, pending] = useActionState<ActionResult | null, FormData>(saveSiteDraft.bind(null, "announcement"), null);
  return (
    <form action={action} className="grid gap-3 text-sm sm:grid-cols-2">
      <label className="block">
        Message
        <input name="text" defaultValue={draft.text ?? ""} maxLength={120} className="admin-input mt-1" />
      </label>
      <label className="block">
        Link <span className="text-stone-500">(optional)</span>
        <input name="link" defaultValue={draft.link ?? ""} className="admin-input mt-1" />
      </label>
      <label className="flex items-center gap-2">
        <input type="checkbox" name="enabled" defaultChecked={draft.enabled} /> Show the bar
      </label>
      <div className="flex flex-wrap items-center gap-2 sm:col-span-2">
        <button disabled={pending} className="admin-btn">Save draft</button>
        <PublishButton label="Publish bar" onPublish={() => publishSite("announcement")} />
        <ActionMessage result={result} />
      </div>
    </form>
  );
}
