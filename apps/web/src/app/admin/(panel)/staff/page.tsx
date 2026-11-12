import type { Metadata } from "next";
import { Suspense } from "react";
import { CreateStaffForm, StaffRow } from "@/components/admin/StaffForms";
import { Loading, PageTitle } from "@/components/admin/ui";
import { adminFetch, getCurrentStaff } from "@/lib/admin/api";
import { can, type Staff } from "@/lib/admin/types";

export const metadata: Metadata = { title: "Staff" };

async function StaffList() {
  const me = await getCurrentStaff();
  if (!can.manageStaff(me.role)) return <p className="text-stone-600">Only the store owner can manage staff.</p>;
  const staff = await adminFetch<Staff[]>("/api/v1/admin/staff");
  return (
    <div className="space-y-6">
      <section className="admin-card">
        <h2 className="mb-1 font-medium">Add a team member</h2>
        <p className="mb-4 text-sm text-stone-500">
          Roles control what each person can do — see docs/03-actors-and-use-cases.md for the full permission matrix.
        </p>
        <CreateStaffForm />
      </section>
      <section className="admin-card">
        <h2 className="mb-3 font-medium">Team ({staff.length})</h2>
        <ul className="divide-y divide-stone-100">
          {staff.map((s) => (
            <li key={s.id} className={`py-4 ${s.active ? "" : "opacity-60"}`}>
              <StaffRow staff={s} isMe={s.id === me.id} />
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

export default function StaffPage() {
  return (
    <>
      <PageTitle>Staff</PageTitle>
      <Suspense fallback={<Loading />}>
        <StaffList />
      </Suspense>
    </>
  );
}
