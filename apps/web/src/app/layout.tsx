import type { Metadata } from "next";
import { Cormorant_Garamond, Inter } from "next/font/google";
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

// Storefront chrome lives in app/(shop)/layout.tsx; the back office in app/admin.
export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className={`${sans.variable} ${serif.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col">{children}</body>
    </html>
  );
}
