import type { Metadata } from "next";
import { Suspense } from "react";
import { VerifyEmail } from "@/components/account/VerifyEmail";

export const metadata: Metadata = { title: "Confirm your email", robots: { index: false } };

async function Verify({ searchParams }: Pick<PageProps<"/account/verify">, "searchParams">) {
  const { token } = await searchParams;
  return <VerifyEmail token={typeof token === "string" ? token : ""} />;
}

export default function VerifyPage({ searchParams }: PageProps<"/account/verify">) {
  return (
    <div className="mx-auto max-w-md px-4 py-16 text-center">
      <h1 className="mb-6 font-serif text-4xl">Confirm your email</h1>
      <Suspense fallback={<p>Checking your link…</p>}>
        <Verify searchParams={searchParams} />
      </Suspense>
    </div>
  );
}
