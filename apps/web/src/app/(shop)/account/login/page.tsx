import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { LoginForm } from "@/components/account/AccountForms";

export const metadata: Metadata = { title: "Sign in", robots: { index: false } };

async function Form({ searchParams }: Pick<PageProps<"/account/login">, "searchParams">) {
  const { next } = await searchParams;
  return <LoginForm next={typeof next === "string" ? next : undefined} />;
}

export default function LoginPage({ searchParams }: PageProps<"/account/login">) {
  return (
    <div className="mx-auto max-w-sm px-4 py-16">
      <h1 className="font-serif text-4xl">Sign in</h1>
      <p className="mb-8 mt-2 text-sm text-stone-600">
        New here?{" "}
        <Link href="/account/register" className="underline">
          Create an account
        </Link>
      </p>
      <Suspense fallback={<div className="h-64" />}>
        <Form searchParams={searchParams} />
      </Suspense>
      <p className="mt-10 border-t border-stone-200 pt-6 text-sm text-stone-600">
        Checked out as a guest?{" "}
        <Link href="/order/lookup" className="underline">
          Find your order
        </Link>
      </p>
    </div>
  );
}
