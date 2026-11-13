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
        <div className="flex flex-wrap justify-between gap-8">
          <div>
            <p className="font-serif text-xl text-stone-800">Yvette</p>
            <p className="mt-2">Curated for you, styled with you.</p>
          </div>
          <nav aria-label="Footer" className="grid grid-cols-2 gap-x-10 gap-y-2">
            <Link href="/account" className="hover:text-stone-900">My account</Link>
            <Link href="/order/lookup" className="hover:text-stone-900">Find an order</Link>
            <Link href="/pages/shipping" className="hover:text-stone-900">Delivery</Link>
            <Link href="/pages/returns" className="hover:text-stone-900">Returns</Link>
            <Link href="/pages/privacy" className="hover:text-stone-900">Privacy</Link>
            <Link href="/pages/terms" className="hover:text-stone-900">Terms</Link>
            <Link href="/pages/cookies" className="hover:text-stone-900">Cookies</Link>
            <Link href="/pages/size-guide" className="hover:text-stone-900">Size guide</Link>
          </nav>
        </div>
        <p className="mt-8 text-xs">Prices in Kenyan shillings. Pay with M-Pesa or card.</p>
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
