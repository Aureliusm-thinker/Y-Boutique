import type { Metadata } from "next";
import { LoginForm } from "./LoginForm";

export const metadata: Metadata = { title: "Staff sign in", robots: { index: false } };

export default function LoginPage() {
  return (
    <div className="mx-auto max-w-sm px-4 py-20">
      <h1 className="font-serif text-3xl">Staff sign in</h1>
      <p className="mt-2 mb-8 text-sm text-stone-600">Back office for the Yvette team.</p>
      <LoginForm />
    </div>
  );
}
