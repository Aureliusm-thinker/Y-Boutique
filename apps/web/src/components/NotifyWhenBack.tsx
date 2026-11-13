"use client";

import { useState, useTransition } from "react";
import { notifyWhenBack } from "@/app/(shop)/account/actions";

/** "Email me when it's back" for a sold-out size (back-in-stock alerts). */
export function NotifyWhenBack({ variantId, label }: { variantId: number; label: string }) {
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null);
  const [pending, start] = useTransition();
  if (message?.ok) return <p role="status" className="mt-2 text-sm text-green-800">{message.text}</p>;
  return (
    <form
      className="mt-2 space-y-2"
      onSubmit={(e) => {
        e.preventDefault();
        const email = String(new FormData(e.currentTarget).get("email") ?? "");
        start(async () => {
          const r = await notifyWhenBack(variantId, email);
          setMessage({ ok: Boolean(r.ok), text: r.message ?? "" });
        });
      }}
    >
      <p className="text-sm">{label} is sold out. Get an email when it&apos;s back:</p>
      <div className="flex gap-2">
        <input
          name="email"
          type="email"
          required
          placeholder="Your email"
          aria-label="Your email"
          className="w-full border border-stone-300 px-3 py-2 text-sm"
        />
        <button disabled={pending} className="shrink-0 bg-stone-900 px-4 text-sm text-white">
          Notify me
        </button>
      </div>
      {message && !message.ok && <p role="alert" className="text-sm text-red-700">{message.text}</p>}
    </form>
  );
}
