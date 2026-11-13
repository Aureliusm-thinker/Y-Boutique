import type { Metadata } from "next";
import { LookupForm } from "@/components/account/AccountForms";

export const metadata: Metadata = { title: "Find your order", robots: { index: false } };

export default function LookupPage() {
  return (
    <div className="mx-auto max-w-sm px-4 py-16">
      <h1 className="font-serif text-4xl">Find your order</h1>
      <p className="mb-8 mt-2 text-sm text-stone-600">
        Enter the order number from your confirmation email and the email you used at checkout.
      </p>
      <LookupForm />
    </div>
  );
}
