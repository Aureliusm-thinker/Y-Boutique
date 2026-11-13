"use client";

import { useActionState } from "react";
import { login, type LoginState } from "./actions";

export function LoginForm() {
  const [state, action, pending] = useActionState<LoginState, FormData>(login, {});

  if (state.ticket) {
    return (
      <form action={action} className="space-y-4">
        <input type="hidden" name="ticket" value={state.ticket} />
        <input type="hidden" name="email" value={state.email ?? ""} />
        <p className="text-sm text-stone-600">
          Two-step sign-in is on for <strong>{state.email}</strong>. Open your authenticator app and enter the
          6-digit code for Yvette.
        </p>
        <label className="block text-sm">
          Authentication code
          <input
            name="code"
            inputMode="numeric"
            autoComplete="one-time-code"
            pattern="[0-9 ]{6,7}"
            maxLength={7}
            required
            autoFocus
            className="admin-input mt-1 tracking-[0.3em]"
          />
        </label>
        {state.error && (
          <p role="alert" className="text-sm text-red-700">
            {state.error}
          </p>
        )}
        <button disabled={pending} className="admin-btn-primary w-full">
          {pending ? "Checking…" : "Verify"}
        </button>
      </form>
    );
  }

  return (
    <form action={action} className="space-y-4">
      <label className="block text-sm">
        Email
        <input
          name="email"
          type="email"
          autoComplete="username"
          required
          defaultValue={state.email}
          className="admin-input mt-1"
        />
      </label>
      <label className="block text-sm">
        Password
        <input
          name="password"
          type="password"
          autoComplete="current-password"
          required
          className="admin-input mt-1"
        />
      </label>
      {state.error && (
        <p role="alert" className="text-sm text-red-700">
          {state.error}
        </p>
      )}
      <button disabled={pending} className="admin-btn-primary w-full">
        {pending ? "Signing in…" : "Sign in"}
      </button>
    </form>
  );
}
