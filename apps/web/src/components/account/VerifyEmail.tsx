"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { verifyEmail } from "@/app/(shop)/account/actions";

/**
 * Confirms the email when the page opens. Runs from the browser (not during server rendering) so
 * link scanners that pre-fetch emails don't use up the one-time link.
 */
export function VerifyEmail({ token }: { token: string }) {
  const [result, setResult] = useState<{ ok?: boolean; message?: string } | null>(null);
  const started = useRef(false);
  useEffect(() => {
    if (started.current || !token) return;
    started.current = true;
    verifyEmail(token).then(setResult);
  }, [token]);

  if (!token) return <p>This link is incomplete.</p>;
  if (!result) return <p role="status">Confirming…</p>;
  if (!result.ok) {
    return (
      <div role="alert">
        <p>{result.message}</p>
        <Link href="/account" className="mt-4 inline-block underline">
          Go to your account to send a new link
        </Link>
      </div>
    );
  }
  return (
    <div role="status">
      <p>Thank you — your email is confirmed. Any orders you placed as a guest with this email are now in your account.</p>
      <Link href="/account" className="mt-6 inline-block bg-stone-900 px-6 py-3 text-sm uppercase tracking-widest text-white">
        Go to my account
      </Link>
    </div>
  );
}
