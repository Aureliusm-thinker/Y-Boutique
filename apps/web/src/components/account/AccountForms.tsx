"use client";

import Link from "next/link";
import { useActionState, useTransition } from "react";
import {
  changePassword,
  deleteAccount,
  deleteAddress,
  forgotPassword,
  login,
  lookupOrder,
  register,
  resetPassword,
  saveAddress,
  updatePreferences,
  updateProfile,
  type FormState,
} from "@/app/(shop)/account/actions";
import type { Address, CustomerProfile } from "@/lib/account";

export const field = "mt-1 w-full border border-stone-300 bg-white px-3 py-2 text-sm focus:border-stone-900 focus:outline-none";
const button = "bg-stone-900 px-6 py-3 text-sm uppercase tracking-widest text-white hover:bg-stone-700 disabled:opacity-50";

export function Message({ state }: { state: FormState }) {
  if (!state.message) return null;
  return (
    <div role={state.ok ? "status" : "alert"} className={`text-sm ${state.ok ? "text-green-800" : "text-red-700"}`}>
      {state.message}
      {state.errors && state.errors.length > 0 && (
        <ul className="mt-1 list-disc pl-5">
          {state.errors.map((e) => (
            <li key={e}>{e}</li>
          ))}
        </ul>
      )}
    </div>
  );
}

export function LoginForm({ next }: { next?: string }) {
  const [state, action, pending] = useActionState<FormState, FormData>(login, {});
  return (
    <form action={action} className="space-y-4">
      <input type="hidden" name="next" value={next ?? "/account"} />
      <label className="block text-sm">
        Email
        <input name="email" type="email" required autoComplete="email" defaultValue={state.values?.email} className={field} />
      </label>
      <label className="block text-sm">
        Password
        <input name="password" type="password" required autoComplete="current-password" className={field} />
      </label>
      <Message state={state} />
      <button disabled={pending} className={`${button} w-full`}>
        {pending ? "Signing in…" : "Sign in"}
      </button>
      <p className="text-center text-sm">
        <Link href="/account/forgot" className="underline">
          Forgot your password?
        </Link>
      </p>
    </form>
  );
}

export function RegisterForm({ email }: { email?: string }) {
  const [state, action, pending] = useActionState<FormState, FormData>(register, {});
  return (
    <form action={action} className="space-y-4">
      <label className="block text-sm">
        Full name
        <input name="fullName" required autoComplete="name" defaultValue={state.values?.fullName} className={field} />
      </label>
      <label className="block text-sm">
        Email
        <input name="email" type="email" required autoComplete="email" defaultValue={state.values?.email ?? email} className={field} />
      </label>
      <label className="block text-sm">
        Password <span className="text-stone-500">(at least 8 characters)</span>
        <input name="password" type="password" required minLength={8} autoComplete="new-password" className={field} />
      </label>
      <label className="flex items-start gap-2 text-sm">
        <input type="checkbox" name="marketingOptIn" className="mt-1" />
        Send me new arrivals and offers by email. You can unsubscribe at any time.
      </label>
      <Message state={state} />
      <button disabled={pending} className={`${button} w-full`}>
        {pending ? "Creating account…" : "Create account"}
      </button>
      <p className="text-xs text-stone-500">
        By creating an account you agree to our <Link href="/pages/terms" className="underline">terms</Link> and{" "}
        <Link href="/pages/privacy" className="underline">privacy policy</Link>.
      </p>
    </form>
  );
}

export function ForgotForm() {
  const [state, action, pending] = useActionState<FormState, FormData>(forgotPassword, {});
  if (state.ok) return <Message state={state} />;
  return (
    <form action={action} className="space-y-4">
      <label className="block text-sm">
        Email
        <input name="email" type="email" required autoComplete="email" defaultValue={state.values?.email} className={field} />
      </label>
      <Message state={state} />
      <button disabled={pending} className={`${button} w-full`}>
        Email me a reset link
      </button>
    </form>
  );
}

export function ResetForm({ token }: { token: string }) {
  const [state, action, pending] = useActionState<FormState, FormData>(resetPassword, {});
  return (
    <form action={action} className="space-y-4">
      <input type="hidden" name="token" value={token} />
      <label className="block text-sm">
        New password
        <input name="password" type="password" required minLength={8} autoComplete="new-password" className={field} />
      </label>
      <label className="block text-sm">
        Confirm new password
        <input name="confirm" type="password" required minLength={8} autoComplete="new-password" className={field} />
      </label>
      <Message state={state} />
      <button disabled={pending} className={`${button} w-full`}>
        Save new password
      </button>
    </form>
  );
}

export function LookupForm() {
  const [state, action, pending] = useActionState<FormState, FormData>(lookupOrder, {});
  return (
    <form action={action} className="space-y-4">
      <label className="block text-sm">
        Order number
        <input name="number" required placeholder="YV-100001" defaultValue={state.values?.number} className={field} />
      </label>
      <label className="block text-sm">
        Email used for the order
        <input name="email" type="email" required defaultValue={state.values?.email} className={field} />
      </label>
      <Message state={state} />
      <button disabled={pending} className={`${button} w-full`}>
        Find my order
      </button>
    </form>
  );
}

export function ProfileForm({ profile }: { profile: CustomerProfile }) {
  const [state, action, pending] = useActionState<FormState, FormData>(updateProfile, {});
  return (
    <form action={action} className="space-y-4">
      <label className="block text-sm">
        Full name
        <input name="fullName" required defaultValue={profile.fullName} className={field} />
      </label>
      <label className="block text-sm">
        Mobile number
        <input name="phone" type="tel" defaultValue={profile.phone ? `0${profile.phone.slice(3)}` : ""} className={field} />
      </label>
      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" name="marketingOptIn" defaultChecked={profile.marketingOptIn} />
        Email me new arrivals and offers
      </label>
      <Message state={state} />
      <button disabled={pending} className={button}>
        Save
      </button>
    </form>
  );
}

export function PasswordForm() {
  const [state, action, pending] = useActionState<FormState, FormData>(changePassword, {});
  return (
    <form action={action} className="space-y-4">
      <label className="block text-sm">
        Current password
        <input name="currentPassword" type="password" required autoComplete="current-password" className={field} />
      </label>
      <label className="block text-sm">
        New password
        <input name="newPassword" type="password" required minLength={8} autoComplete="new-password" className={field} />
      </label>
      <Message state={state} />
      <button disabled={pending} className={button}>
        Change password
      </button>
    </form>
  );
}

const SIZES = ["XS", "S", "M", "L", "XL"];
const STYLES = ["minimal", "classic", "boho", "romantic", "street", "statement"];

/** Optional style profile (FR-CA-07) — used later to personalise recommendations. */
export function PreferencesForm({ profile }: { profile: CustomerProfile }) {
  const [state, action, pending] = useActionState<FormState, FormData>(updatePreferences, {});
  const p = profile.preferences;
  return (
    <form action={action} className="space-y-4 text-sm">
      <fieldset>
        <legend>Your usual sizes</legend>
        <div className="mt-2 flex flex-wrap gap-3">
          {SIZES.map((s) => (
            <label key={s} className="flex items-center gap-1">
              <input type="checkbox" name="sizes" value={s} defaultChecked={p.sizes.includes(s)} /> {s}
            </label>
          ))}
        </div>
      </fieldset>
      <fieldset>
        <legend>Styles you like</legend>
        <div className="mt-2 flex flex-wrap gap-3">
          {STYLES.map((s) => (
            <label key={s} className="flex items-center gap-1 capitalize">
              <input type="checkbox" name="styles" value={s} defaultChecked={p.styles.includes(s)} /> {s}
            </label>
          ))}
        </div>
      </fieldset>
      <label className="block">
        Favourite colours <span className="text-stone-500">(comma separated)</span>
        <input name="colours" defaultValue={p.colours.join(", ")} placeholder="black, sand, emerald" className={field} />
      </label>
      <Message state={state} />
      <button disabled={pending} className={button}>
        Save preferences
      </button>
    </form>
  );
}

export function AddressForm({ address, counties, onDone }: { address?: Address; counties: string[]; onDone?: () => void }) {
  const [state, action, pending] = useActionState<FormState, FormData>(async (prev, fd) => {
    const r = await saveAddress(prev, fd);
    if (r.ok) onDone?.();
    return r;
  }, {});
  return (
    <form action={action} className="grid gap-3 text-sm sm:grid-cols-2">
      {address && <input type="hidden" name="id" value={address.id} />}
      <label className="block">
        Label <span className="text-stone-500">(e.g. Home)</span>
        <input name="label" defaultValue={address?.label ?? ""} className={field} />
      </label>
      <label className="block">
        Recipient name
        <input name="fullName" required defaultValue={address?.fullName} className={field} />
      </label>
      <label className="block sm:col-span-2">
        Street address, estate or building
        <input name="line1" required defaultValue={address?.line1} className={field} />
      </label>
      <label className="block sm:col-span-2">
        Apartment, house number <span className="text-stone-500">(optional)</span>
        <input name="line2" defaultValue={address?.line2 ?? ""} className={field} />
      </label>
      <label className="block">
        Town or city
        <input name="city" required defaultValue={address?.city} className={field} />
      </label>
      <label className="block">
        County
        <select name="county" required defaultValue={address?.county ?? "Nairobi"} className={field}>
          {counties.map((c) => (
            <option key={c}>{c}</option>
          ))}
        </select>
      </label>
      <label className="block">
        Phone <span className="text-stone-500">(optional)</span>
        <input name="phone" type="tel" defaultValue={address?.phone ? `0${address.phone.slice(3)}` : ""} className={field} />
      </label>
      <label className="block">
        Postal code <span className="text-stone-500">(optional)</span>
        <input name="postalCode" defaultValue={address?.postalCode ?? ""} className={field} />
      </label>
      <label className="flex items-center gap-2 sm:col-span-2">
        <input type="checkbox" name="makeDefault" defaultChecked={address?.isDefault} /> Use as my default address
      </label>
      <div className="flex items-center gap-4 sm:col-span-2">
        <button disabled={pending} className={button}>
          Save address
        </button>
        <Message state={state} />
      </div>
    </form>
  );
}

export function DeleteAddressButton({ id }: { id: number }) {
  const [pending, start] = useTransition();
  return (
    <button
      type="button"
      disabled={pending}
      onClick={() => confirm("Delete this address?") && start(async () => void (await deleteAddress(id)))}
      className="text-sm text-red-700 underline"
    >
      Delete
    </button>
  );
}

export function DeleteAccountForm() {
  const [state, action, pending] = useActionState<FormState, FormData>(deleteAccount, {});
  return (
    <form action={action} className="space-y-4 text-sm">
      <label className="block">
        Your password
        <input name="password" type="password" required autoComplete="current-password" className={field} />
      </label>
      <label className="block">
        Type <strong>DELETE</strong> to confirm
        <input name="confirm" required className={field} />
      </label>
      <Message state={state} />
      <button disabled={pending} className="border border-red-700 px-6 py-3 text-sm uppercase tracking-widest text-red-700 hover:bg-red-700 hover:text-white">
        Delete my account
      </button>
    </form>
  );
}
