"use client";

export function PrintButton({ label = "Print or save as PDF" }: { label?: string }) {
  return (
    <button type="button" onClick={() => window.print()} className="bg-stone-900 px-5 py-2 text-sm text-white">
      {label}
    </button>
  );
}
