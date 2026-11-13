import type { Metadata } from "next";
import { Suspense } from "react";
import { DeleteAccountForm } from "@/components/account/AccountForms";
import { AccountShell } from "@/components/account/AccountShell";
import { requireCustomer } from "@/lib/account";

export const metadata: Metadata = { title: "Privacy & data", robots: { index: false } };

async function Content() {
  await requireCustomer("/account/privacy");
  return (
    <div className="max-w-xl space-y-12 text-sm">
      <section>
        <h2 className="mb-2 font-serif text-2xl">Download your data</h2>
        <p className="mb-4 text-stone-600">
          A file with your profile, addresses, orders, wishlist, reviews and browsing history on Yvette (Kenya Data
          Protection Act, 2019 — right of access).
        </p>
        <a href="/account/export" className="inline-block border border-stone-900 px-6 py-3 uppercase tracking-widest">
          Download my data
        </a>
      </section>
      <section>
        <h2 className="mb-2 font-serif text-2xl">Delete your account</h2>
        <p className="mb-4 text-stone-600">
          This deletes your profile, addresses, wishlist and alerts straight away. We keep order records for tax
          purposes, but remove your name, email, phone and address from them. This can&apos;t be undone.
        </p>
        <DeleteAccountForm />
      </section>
    </div>
  );
}

export default function PrivacyPage() {
  return (
    <AccountShell title="Privacy & data" active="/account/privacy">
      <Suspense fallback={<div className="h-64 animate-pulse bg-stone-100" />}>
        <Content />
      </Suspense>
    </AccountShell>
  );
}
