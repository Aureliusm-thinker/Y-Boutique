"use client";

import { useActionState, useState, useTransition } from "react";
import {
  disableMfa,
  enableMfa,
  resetStaffMfa,
  startMfaSetup,
  type MfaSetupState,
} from "@/app/admin/(panel)/security/actions";
import type { ActionResult } from "@/lib/admin/types";
import { ActionMessage } from "./ui";

export function MfaSetup() {
  const [setup, setSetup] = useState<MfaSetupState | null>(null);
  const [starting, startTransition] = useTransition();
  const [result, action, pending] = useActionState<MfaSetupState, FormData>(enableMfa, {} as MfaSetupState);

  if (result.ok && result.message) return <ActionMessage result={result} />;

  if (!setup?.secret) {
    return (
      <div className="space-y-3">
        <button
          className="admin-btn-primary"
          disabled={starting}
          onClick={() => startTransition(async () => setSetup(await startMfaSetup()))}
        >
          {starting ? "Preparing…" : "Set up two-step sign-in"}
        </button>
        <ActionMessage result={setup} />
      </div>
    );
  }

  return (
    <div className="grid gap-6 sm:grid-cols-[auto_1fr]">
      <div
        className="h-[200px] w-[200px] border border-stone-200 bg-white"
        role="img"
        aria-label="QR code to scan with your authenticator app"
        // SVG generated on the server from our own otpauth:// URI.
        dangerouslySetInnerHTML={{ __html: setup.qrSvg ?? "" }}
      />
      <form action={action} className="space-y-3 text-sm">
        <ol className="list-decimal space-y-1 pl-5 text-stone-700">
          <li>Install an authenticator app (Google Authenticator, Microsoft Authenticator, 1Password…).</li>
          <li>Scan the QR code. Can&apos;t scan? Enter this key instead:</li>
        </ol>
        <code className="block break-all rounded bg-stone-100 px-2 py-1 font-mono text-xs" data-testid="mfa-secret">
          {setup.secret}
        </code>
        <label className="block">
          3. Enter the 6-digit code the app shows
          <input
            name="code"
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={7}
            required
            className="admin-input mt-1 w-40 tracking-[0.3em]"
          />
        </label>
        <button disabled={pending} className="admin-btn-primary">
          Turn on
        </button>
        <ActionMessage result={result.ok === false ? result : null} />
      </form>
    </div>
  );
}

export function MfaDisable() {
  const [result, action, pending] = useActionState<ActionResult | null, FormData>(disableMfa, null);
  return (
    <form action={action} className="flex flex-wrap items-end gap-3 text-sm">
      <label>
        Current code
        <input name="code" inputMode="numeric" maxLength={7} required className="admin-input mt-1 w-32" />
      </label>
      <button disabled={pending} className="admin-btn">
        Turn off
      </button>
      <ActionMessage result={result} />
    </form>
  );
}

export function ResetMfaButton({ staffId }: { staffId: number }) {
  const [result, setResult] = useState<ActionResult | null>(null);
  const [pending, startTransition] = useTransition();
  return (
    <span className="inline-flex items-center gap-2">
      <button
        type="button"
        className="admin-btn"
        disabled={pending}
        onClick={() => {
          if (confirm("Reset two-step sign-in for this person? Use this if they lost their phone.")) {
            startTransition(async () => setResult(await resetStaffMfa(staffId)));
          }
        }}
      >
        Reset 2-step
      </button>
      {result && !result.ok && <span className="text-xs text-red-700">{result.message}</span>}
    </span>
  );
}
