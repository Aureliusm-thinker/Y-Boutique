import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { RegisterForm } from "@/components/account/AccountForms";

export const metadata: Metadata = { title: "Create an account", robots: { index: false } };

async function Form({ searchParams }: Pick<PageProps<"/account/register">, "searchParams">) {
  const { email } = await searchParams;
  return <RegisterForm email={typeof email === "string" ? email : undefined} />;
}

export default function RegisterPage({ searchParams }: PageProps<"/account/register">) {
  return (
    <div className="mx-auto max-w-sm px-4 py-16">
      <h1 className="font-serif text-4xl">Create an account</h1>
      <p className="mb-8 mt-2 text-sm text-stone-600">
        Track orders, save addresses and keep a wishlist. Already have one?{" "}
        <Link href="/account/login" className="underline">
          Sign in
        </Link>
      </p>
      <Suspense fallback={<div className="h-80" />}>
        <Form searchParams={searchParams} />
      </Suspense>
    </div>
  );
}
