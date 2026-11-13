import type { Metadata } from "next";
import { ForgotForm } from "@/components/account/AccountForms";

export const metadata: Metadata = { title: "Reset your password", robots: { index: false } };

export default function ForgotPage() {
  return (
    <div className="mx-auto max-w-sm px-4 py-16">
      <h1 className="font-serif text-4xl">Forgot your password?</h1>
      <p className="mb-8 mt-2 text-sm text-stone-600">Enter your email and we&apos;ll send you a link to choose a new one.</p>
      <ForgotForm />
    </div>
  );
}
