"use client";

import { useEffect, useState } from "react";
import { getConsent, setConsent, type Consent } from "@/lib/analytics";

export function ConsentBanner() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    // Reading localStorage must happen after mount, so this sync is intentional.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setVisible(getConsent() === null);
  }, []);

  if (!visible) return null;

  const choose = (consent: Consent) => {
    setConsent(consent);
    setVisible(false);
  };

  return (
    <div
      role="dialog"
      aria-label="Cookie preferences"
      className="fixed inset-x-4 bottom-4 z-50 mx-auto max-w-xl rounded-lg border border-stone-200 bg-white p-4 shadow-lg sm:flex sm:items-center sm:gap-4"
    >
      <p className="text-sm text-stone-700">
        We use analytics to improve the shop and, later, to personalise recommendations for you.
      </p>
      <div className="mt-3 flex shrink-0 gap-2 sm:mt-0">
        <button
          onClick={() => choose("denied")}
          className="rounded border border-stone-300 px-3 py-1.5 text-sm hover:bg-stone-50"
        >
          Decline
        </button>
        <button
          onClick={() => choose("granted")}
          className="rounded bg-stone-900 px-3 py-1.5 text-sm text-white hover:bg-stone-700"
        >
          Accept
        </button>
      </div>
    </div>
  );
}
