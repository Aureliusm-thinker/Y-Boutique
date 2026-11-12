import type { Metadata } from "next";
import { Cormorant_Garamond, Inter } from "next/font/google";
import Link from "next/link";
import { ConsentBanner } from "@/components/ConsentBanner";
import { getCategories } from "@/lib/api";
import "./globals.css";

const sans = Inter({ variable: "--font-sans-family", subsets: ["latin"] });
const serif = Cormorant_Garamond({
  variable: "--font-serif-family",
  subsets: ["latin"],
  weight: ["400", "500", "600"],
});

export const metadata: Metadata = {
  title: { default: "Yvette — Modern Boutique", template: "%s · Yvette" },
  description: "A curated modern boutique. Your personal boutique — curated for you, styled with you.",
};

async function Header() {
  const categories = await getCategories();
  return (
    <header className="border-b border-stone-200">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-5">
        <Link href="/" className="font-serif text-3xl tracking-wide">
          Yvette
        </Link>
        <nav className="flex gap-5 text-sm text-stone-700">
          <Link href="/shop" className="hover:text-stone-950">
            Shop all
          </Link>
          {categories.map((c) => (
            <Link key={c.id} href={`/shop?category=${c.slug}`} className="hidden hover:text-stone-950 sm:inline">
              {c.name}
            </Link>
          ))}
        </nav>
      </div>
    </header>
  );
}

function Footer() {
  return (
    <footer className="mt-24 border-t border-stone-200">
      <div className="mx-auto max-w-6xl px-4 py-10 text-sm text-stone-500">
        <p className="font-serif text-xl text-stone-800">Yvette</p>
        <p className="mt-2">Curated for you, styled with you.</p>
      </div>
    </footer>
  );
}

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className={`${sans.variable} ${serif.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col">
        <Header />
        <main className="flex-1">{children}</main>
        <Footer />
        <ConsentBanner />
      </body>
    </html>
  );
}
