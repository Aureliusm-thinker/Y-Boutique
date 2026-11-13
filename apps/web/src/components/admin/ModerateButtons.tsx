"use client";

import { useState, useTransition } from "react";
import { moderateReview } from "@/app/admin/(panel)/reviews/actions";

export function ModerateButtons({ id }: { id: number }) {
  const [pending, start] = useTransition();
  const [error, setError] = useState<string | null>(null);
  const act = (approve: boolean) =>
    start(async () => {
      const r = await moderateReview(id, approve);
      if (!r.ok) setError(r.message ?? "Failed");
    });
  return (
    <div className="mt-3 flex items-center gap-2">
      <button disabled={pending} onClick={() => act(true)} className="admin-btn-primary">
        Publish
      </button>
      <button disabled={pending} onClick={() => act(false)} className="admin-btn">
        Reject
      </button>
      {error && <span className="text-red-700">{error}</span>}
    </div>
  );
}
