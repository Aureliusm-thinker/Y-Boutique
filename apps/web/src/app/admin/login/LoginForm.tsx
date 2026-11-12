"use client";

import { useActionState } from "react";
import { login, type LoginState } from "./actions";

export function LoginForm() {
  const [state, action, pending] = useActionState<LoginState, FormData>(login, {});
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
