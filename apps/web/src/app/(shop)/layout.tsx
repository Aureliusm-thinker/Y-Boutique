import Link from "next/link";
import { ConsentBanner } from "@/components/ConsentBanner";
import { SearchBox } from "@/components/SearchBox";
import { Suspense } from "react";
import { BagLink } from "@/components/BagLink";
import { AccountLink } from "@/components/AccountLink";
import { getCategories } from "@/lib/api";

async function Header() {
  const categories = await getCategories();
  return (
    <header className="border-b border-stone-200 print:hidden">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-5">
        <Link href="/" className="font-serif text-3xl tracking-wide">
          Yvette
        </Link>
        <nav className="flex items-center gap-5 text-sm text-stone-700">
          <Link href="/shop" className="hover:text-stone-950">
            Shop all
          </Link>
          {categories.map((c) => (
            <Link key={c.id} href={`/shop?category=${c.slug}`} className="hidden hover:text-stone-950 sm:inline">
              {c.name}
            </Link>
          ))}
          <SearchBox />
          <Suspense fallback={<span className="w-16" />}>
            <AccountLink />
          </Suspense>
          <Suspense fallback={<span>Bag</span>}>
            <BagLink />
          </Suspense>
        </nav>
      </div>
    </header>
  );
}

function Footer() {
  return (
    <footer className="mt-24 border-t border-stone-200 print:hidden">
      <div className="mx-auto max-w-6xl px-4 py-10 text-sm text-stone-500">
        <p className="font-serif text-xl text-stone-800">Yvette</p>
        <p className="mt-2">Curated for you, styled with you.</p>
      </div>
    </footer>
  );
}

export default function ShopLayout({ children }: LayoutProps<"/">) {
  return (
    <>
      <Header />
      <main className="flex-1">{children}</main>
      <Footer />
      <ConsentBanner />
    </>
  );
}
