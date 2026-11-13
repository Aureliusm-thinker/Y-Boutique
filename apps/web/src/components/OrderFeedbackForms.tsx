"use client";

import Image from "next/image";
import { useActionState } from "react";
import { requestReturn, submitReview, type ReturnRequestState } from "@/app/(shop)/order/actions";
import type { OrderLine } from "@/lib/types";

const REASONS = [
  ["TOO_SMALL", "Too small"],
  ["TOO_LARGE", "Too large"],
  ["NOT_AS_DESCRIBED", "Not as described"],
  ["DAMAGED", "Arrived damaged"],
  ["CHANGED_MIND", "Changed my mind"],
  ["OTHER", "Other"],
];

function Result({ state }: { state: ReturnRequestState }) {
  if (!state.message) return null;
  return (
    <p role={state.ok ? "status" : "alert"} className={`text-sm ${state.ok ? "text-green-800" : "text-red-700"}`}>
      {state.message}
    </p>
  );
}

/** Return request: quantity and a reason per item (size issues tracked separately — FR-CA-06). */
export function ReturnForm({ number, token, lines }: { number: string; token: string; lines: (OrderLine & { returnable: number })[] }) {
  const [state, action, pending] = useActionState<ReturnRequestState, FormData>(requestReturn.bind(null, number, token), {});
  if (state.ok) return <Result state={state} />;
  return (
    <form action={action} className="space-y-6">
      <ul className="divide-y divide-stone-200 border-y border-stone-200">
        {lines.map((l) => (
          <li key={l.id} className="flex flex-wrap items-center gap-4 py-4 text-sm">
            <div className="relative h-20 w-16 shrink-0 bg-stone-100">
              {l.imageUrl && <Image src={l.imageUrl} alt="" fill sizes="64px" className="object-cover" />}
            </div>
            <div className="min-w-40 flex-1">
              <p>{l.title}</p>
              <p className="text-stone-500">{[l.size, l.colour].filter(Boolean).join(" · ")}</p>
            </div>
            {l.returnable === 0 ? (
              <p className="text-stone-500">Already returned</p>
            ) : (
              <>
                <label>
                  <span className="sr-only">Quantity to return of {l.title}</span>
                  <select name={`qty-${l.id}`} defaultValue="0" className="border border-stone-300 px-2 py-1">
                    {Array.from({ length: l.returnable + 1 }, (_, n) => (
                      <option key={n} value={n}>
                        {n === 0 ? "Keep" : `Return ${n}`}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  <span className="sr-only">Reason for returning {l.title}</span>
                  <select name={`reason-${l.id}`} defaultValue="" className="border border-stone-300 px-2 py-1">
                    <option value="">Reason…</option>
                    {REASONS.map(([v, label]) => (
                      <option key={v} value={v}>
                        {label}
                      </option>
                    ))}
                  </select>
                </label>
              </>
            )}
          </li>
        ))}
      </ul>
      <label className="block text-sm">
        Anything else we should know? <span className="text-stone-500">(optional)</span>
        <textarea name="comment" rows={3} maxLength={1000} className="mt-1 w-full border border-stone-300 px-3 py-2" />
      </label>
      <Result state={state} />
      <button disabled={pending} className="bg-stone-900 px-6 py-3 text-sm uppercase tracking-widest text-white disabled:opacity-50">
        Request return
      </button>
    </form>
  );
}

export function ReviewForm({ number, token, productId, title }: { number: string; token: string; productId: number; title: string }) {
  const [state, action, pending] = useActionState<ReturnRequestState, FormData>(
    submitReview.bind(null, number, token, productId),
    {},
  );
  if (state.ok) return <Result state={state} />;
  return (
    <form action={action} className="space-y-4 text-sm">
      <fieldset>
        <legend>Your rating for {title}</legend>
        <div className="mt-1 flex flex-row-reverse justify-end gap-1">
          {[5, 4, 3, 2, 1].map((n) => (
            <label key={n} className="cursor-pointer text-2xl text-stone-300 has-[:checked]:text-stone-900 [label:has(:checked)~&]:text-stone-900">
              <input type="radio" name="rating" value={n} className="sr-only" aria-label={`${n} star${n > 1 ? "s" : ""}`} />★
            </label>
          ))}
        </div>
      </fieldset>
      <fieldset>
        <legend>How did it fit?</legend>
        <div className="mt-1 flex flex-wrap gap-4">
          {[
            ["RUNS_SMALL", "Runs small"],
            ["TRUE_TO_SIZE", "True to size"],
            ["RUNS_LARGE", "Runs large"],
          ].map(([v, label]) => (
            <label key={v} className="flex items-center gap-1">
              <input type="radio" name="fit" value={v} /> {label}
            </label>
          ))}
        </div>
      </fieldset>
      <label className="block">
        Headline <span className="text-stone-500">(optional)</span>
        <input name="title" maxLength={120} className="mt-1 w-full border border-stone-300 px-3 py-2" />
      </label>
      <label className="block">
        Your review <span className="text-stone-500">(optional)</span>
        <textarea name="body" rows={3} maxLength={2000} className="mt-1 w-full border border-stone-300 px-3 py-2" />
      </label>
      <Result state={state} />
      <button disabled={pending} className="bg-stone-900 px-6 py-2 text-sm uppercase tracking-widest text-white disabled:opacity-50">
        Submit review
      </button>
    </form>
  );
}
