"use client";

import { useState, useTransition } from "react";
import { resendVerification } from "@/app/(shop)/account/actions";

export function ResendVerification() {
  const [message, setMessage] = useState<string | null>(null);
  const [pending, start] = useTransition();
  if (message) return <span className="font-medium">{message}</span>;
  return (
    <button
      type="button"
      disabled={pending}
      onClick={() => start(async () => setMessage((await resendVerification()).message ?? "Sent."))}
      className="underline"
    >
      Send the link again
    </button>
  );
}
