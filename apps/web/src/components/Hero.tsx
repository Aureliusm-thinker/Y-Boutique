import Image from "next/image";
import Link from "next/link";
import type { HomeContent } from "@/lib/api";

/** The hero, as edited under Content in the back office (FR-SF-01 / FR-AD-10). */
export function Hero({ home, preview = false }: { home: HomeContent; preview?: boolean }) {
  return (
    <section className="relative overflow-hidden bg-stone-200/60">
      {home.heroImage && /^https?:\/\/.+/.test(home.heroImage) && (
        <Image src={home.heroImage} alt="" fill priority={!preview} unoptimized={preview} sizes="100vw" className="object-cover opacity-80" />
      )}
      <div className="relative mx-auto max-w-6xl px-4 py-24 text-center sm:py-32">
        {home.heroEyebrow && <p className="text-xs uppercase tracking-[0.3em] text-stone-600">{home.heroEyebrow}</p>}
        <h1 className="mt-4 font-serif text-5xl leading-tight sm:text-7xl">{home.heroTitle ?? "Curated for you."}</h1>
        {home.heroText && <p className="mx-auto mt-4 max-w-md text-stone-700">{home.heroText}</p>}
        {home.heroCta && (
          <Link
            href={home.heroLink || "/shop"}
            className="mt-8 inline-block bg-stone-900 px-8 py-3 text-sm uppercase tracking-widest text-white hover:bg-stone-700"
          >
            {home.heroCta}
          </Link>
        )}
      </div>
    </section>
  );
}
