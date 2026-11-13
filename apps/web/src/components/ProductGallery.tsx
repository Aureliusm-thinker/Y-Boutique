"use client";

import Image from "next/image";
import { useRef, useState } from "react";

type Img = { url: string; altText: string | null };

/** Product photos with thumbnails and a full-screen zoom view (FR-SF-05). */
export function ProductGallery({ images, title }: { images: Img[]; title: string }) {
  const [index, setIndex] = useState(0);
  const [zoom, setZoom] = useState<{ x: number; y: number } | null>(null);
  const dialog = useRef<HTMLDialogElement>(null);
  const current = images[index];
  if (!current) return <div className="aspect-[4/5] bg-stone-100" />;

  return (
    <div>
      <button
        type="button"
        onClick={() => dialog.current?.showModal()}
        className="relative block aspect-[4/5] w-full cursor-zoom-in bg-stone-100"
        aria-label="Open larger image"
      >
        <Image
          src={current.url}
          alt={current.altText ?? title}
          fill
          priority
          sizes="(min-width: 768px) 50vw, 100vw"
          className="object-cover"
        />
      </button>

      {images.length > 1 && (
        <div className="mt-3 flex gap-2" role="list" aria-label="More photos">
          {images.map((img, i) => (
            <button
              key={img.url}
              type="button"
              role="listitem"
              onClick={() => setIndex(i)}
              aria-label={`Show photo ${i + 1}`}
              aria-current={i === index}
              className="relative h-20 w-16 bg-stone-100 outline-offset-2 aria-[current=true]:outline aria-[current=true]:outline-stone-900"
            >
              <Image src={img.url} alt="" fill sizes="64px" className="object-cover" />
            </button>
          ))}
        </div>
      )}

      <dialog
        ref={dialog}
        onClose={() => setZoom(null)}
        className="m-auto h-[95vh] w-[95vw] max-w-none bg-white p-0 backdrop:bg-black/70"
        aria-label={`${title} — enlarged photo`}
      >
        <div
          className={`relative h-full w-full overflow-hidden ${zoom ? "cursor-zoom-out" : "cursor-zoom-in"}`}
          onClick={(e) => {
            const r = e.currentTarget.getBoundingClientRect();
            setZoom(zoom ? null : { x: ((e.clientX - r.left) / r.width) * 100, y: ((e.clientY - r.top) / r.height) * 100 });
          }}
          onMouseMove={(e) => {
            if (!zoom) return;
            const r = e.currentTarget.getBoundingClientRect();
            setZoom({ x: ((e.clientX - r.left) / r.width) * 100, y: ((e.clientY - r.top) / r.height) * 100 });
          }}
        >
          <Image
            src={current.url}
            alt={current.altText ?? title}
            fill
            sizes="95vw"
            className="object-contain transition-transform duration-200"
            style={zoom ? { transform: "scale(2.2)", transformOrigin: `${zoom.x}% ${zoom.y}%` } : undefined}
          />
        </div>
        <button
          type="button"
          onClick={() => dialog.current?.close()}
          className="absolute right-3 top-3 bg-white px-3 py-1 text-sm shadow"
        >
          Close
        </button>
      </dialog>
    </div>
  );
}
