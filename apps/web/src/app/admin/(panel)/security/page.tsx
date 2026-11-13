import type { Metadata } from "next";
import { Suspense } from "react";
import { MfaDisable, MfaSetup } from "@/components/admin/MfaForms";
import { Loading, PageTitle } from "@/components/admin/ui";
import { getCurrentStaff } from "@/lib/admin/api";

export const metadata: Metadata = { title: "Security" };

async function SecurityPanel() {
  const me = await getCurrentStaff();
  return (
    <section className="admin-card max-w-2xl">
      <h2 className="font-medium">Two-step sign-in</h2>
      <p className="mb-4 mt-1 text-sm text-stone-600">
        After your password, you&apos;ll enter a code from an app on your phone. This keeps the shop safe even if
        your password leaks.
      </p>
      {me.mfaEnabled ? (
        <div className="space-y-4">
          <p className="text-sm text-green-800">✓ Two-step sign-in is on.</p>
          {!me.mfaRequired && <MfaDisable />}
        </div>
      ) : (
        <>
          {me.mfaRequired && (
            <p role="status" className="mb-4 rounded border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
              This store requires two-step sign-in. Set it up to continue to the back office.
            </p>
          )}
          <MfaSetup />
        </>
      )}
    </section>
  );
}

export default function SecurityPage() {
  return (
    <>
      <PageTitle>Security</PageTitle>
      <Suspense fallback={<Loading />}>
        <SecurityPanel />
      </Suspense>
    </>
  );
}
