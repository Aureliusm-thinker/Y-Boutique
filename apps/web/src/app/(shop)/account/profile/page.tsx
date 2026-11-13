import type { Metadata } from "next";
import { Suspense } from "react";
import { PasswordForm, PreferencesForm, ProfileForm } from "@/components/account/AccountForms";
import { AccountShell } from "@/components/account/AccountShell";
import { requireCustomer } from "@/lib/account";

export const metadata: Metadata = { title: "Profile", robots: { index: false } };

async function Content() {
  const profile = await requireCustomer("/account/profile");
  return (
    <div className="space-y-12">
      <section className="max-w-md">
        <h2 className="mb-1 font-serif text-2xl">Your details</h2>
        <p className="mb-4 text-sm text-stone-500">{profile.email}</p>
        <ProfileForm profile={profile} />
      </section>
      <section className="max-w-xl">
        <h2 className="mb-1 font-serif text-2xl">Your style</h2>
        <p className="mb-4 text-sm text-stone-500">Optional. Helps us suggest pieces in your size and taste.</p>
        <PreferencesForm profile={profile} />
      </section>
      <section className="max-w-md">
        <h2 className="mb-4 font-serif text-2xl">Password</h2>
        <PasswordForm />
      </section>
    </div>
  );
}

export default function ProfilePage() {
  return (
    <AccountShell title="Profile & style" active="/account/profile">
      <Suspense fallback={<div className="h-64 animate-pulse bg-stone-100" />}>
        <Content />
      </Suspense>
    </AccountShell>
  );
}
