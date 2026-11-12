"use client";

import { useActionState } from "react";
import { createStaff, updateStaff } from "@/app/admin/(panel)/staff/actions";
import { STAFF_ROLES, type ActionResult, type Staff } from "@/lib/admin/types";
import { ActionMessage } from "./ui";

const roleLabel = (r: string) => r.charAt(0) + r.slice(1).toLowerCase();

export function CreateStaffForm() {
  const [result, action, pending] = useActionState<ActionResult | null, FormData>(createStaff, null);
  return (
    <form action={action} className="grid gap-3 sm:grid-cols-2">
      <label className="text-sm">
        Full name
        <input name="fullName" required maxLength={120} className="admin-input mt-1" />
      </label>
      <label className="text-sm">
        Email
        <input name="email" type="email" required className="admin-input mt-1" />
      </label>
      <label className="text-sm">
        Role
        <select name="role" defaultValue="INVENTORY" className="admin-input mt-1">
          {STAFF_ROLES.map((r) => (
            <option key={r} value={r}>
              {roleLabel(r)}
            </option>
          ))}
        </select>
      </label>
      <label className="text-sm">
        Temporary password <span className="text-stone-500">(12+ characters)</span>
        <input name="password" type="password" minLength={12} required autoComplete="new-password" className="admin-input mt-1" />
      </label>
      <div className="flex items-center gap-3 sm:col-span-2">
        <button disabled={pending} className="admin-btn-primary">
          Create account
        </button>
        <ActionMessage result={result} />
      </div>
    </form>
  );
}

export function StaffRow({ staff, isMe }: { staff: Staff; isMe: boolean }) {
  const [result, action, pending] = useActionState<ActionResult | null, FormData>(updateStaff, null);
  return (
    <form action={action} className="flex flex-wrap items-end gap-3">
      <input type="hidden" name="id" value={staff.id} />
      <div className="min-w-48 text-sm">
        <p className="font-medium">
          {staff.email} {isMe && <span className="text-stone-500">(you)</span>}
        </p>
        <p className="text-xs text-stone-500">
          {staff.lastLoginAt ? `Last sign-in ${new Date(staff.lastLoginAt).toLocaleString()}` : "Never signed in"}
        </p>
      </div>
      <label className="text-sm">
        Name
        <input name="fullName" defaultValue={staff.fullName} required maxLength={120} className="admin-input mt-1 w-44" />
      </label>
      <label className="text-sm">
        Role
        <select name="role" defaultValue={staff.role} className="admin-input mt-1">
          {STAFF_ROLES.map((r) => (
            <option key={r} value={r}>
              {roleLabel(r)}
            </option>
          ))}
        </select>
      </label>
      <label className="flex items-center gap-2 pb-2 text-sm">
        <input name="active" type="checkbox" defaultChecked={staff.active} disabled={isMe} />
        Active
        {/* A disabled checkbox isn't submitted; keep "you" active. */}
        {isMe && <input type="hidden" name="active" value="on" />}
      </label>
      <button disabled={pending} className="admin-btn">
        Save
      </button>
      <div className="basis-full">
        <ActionMessage result={result} />
      </div>
    </form>
  );
}
