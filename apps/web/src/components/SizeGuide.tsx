"use client";

import Link from "next/link";
import { useRef } from "react";

const ROWS = [
  ["XS", "6", "80–84", "62–66", "86–90"],
  ["S", "8", "84–88", "66–70", "90–94"],
  ["M", "10", "88–94", "70–76", "94–100"],
  ["L", "12", "94–100", "76–82", "100–106"],
  ["XL", "14", "100–106", "82–88", "106–112"],
];

/** Size guide popup (FR-SF-05). Measurements in centimetres. */
export function SizeGuide() {
  const dialog = useRef<HTMLDialogElement>(null);
  return (
    <>
      <button type="button" onClick={() => dialog.current?.showModal()} className="text-sm underline">
        Size guide
      </button>
      <dialog ref={dialog} className="m-auto w-[min(36rem,92vw)] p-6 backdrop:bg-black/50" aria-labelledby="size-guide-title">
        <h2 id="size-guide-title" className="font-serif text-2xl">
          Size guide
        </h2>
        <p className="mt-1 text-sm text-stone-600">Body measurements in centimetres. Between sizes? Size up for a relaxed fit.</p>
        <table className="mt-4 w-full text-left text-sm">
          <thead className="border-b border-stone-200 text-stone-500">
            <tr>
              <th className="py-2">Size</th>
              <th>UK/KE</th>
              <th>Bust</th>
              <th>Waist</th>
              <th>Hips</th>
            </tr>
          </thead>
          <tbody>
            {ROWS.map((r) => (
              <tr key={r[0]} className="border-b border-stone-100">
                {r.map((c, i) => (
                  <td key={i} className="py-2">
                    {c}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
        <p className="mt-4 text-sm">
          <Link href="/pages/size-guide" className="underline">How to measure yourself</Link>
        </p>
        <form method="dialog" className="mt-6 text-right">
          <button className="bg-stone-900 px-4 py-2 text-sm text-white">Close</button>
        </form>
      </dialog>
    </>
  );
}
