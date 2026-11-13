import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { ResetForm } from "@/components/account/AccountForms";

export const metadata: Metadata = { title: "Choose a new password", robots: { index: false } };

async function Form({ searchParams }: Pick<PageProps<"/account/reset">, "searchParams">) {
  const { token } = await searchParams;
  if (typeof token !== "string") {
    return (
      <p className="text-sm">
        This link is incomplete. <Link href="/account/forgot" className="underline">Request a new one</Link>.
      </p>
    );
  }
  return <ResetForm token={token} />;
}

export default function ResetPage({ searchParams }: PageProps<"/account/reset">) {
  return (
    <div className="mx-auto max-w-sm px-4 py-16">
      <h1 className="mb-8 font-serif text-4xl">Choose a new password</h1>
      <Suspense fallback={<div className="h-48" />}>
        <Form searchParams={searchParams} />
      </Suspense>
    </div>
  );
}
