import type { Metadata } from "next";

export const metadata: Metadata = {
  title: { default: "Back office", template: "%s · Yvette admin" },
  robots: { index: false, follow: false },
};

export default function AdminRootLayout({ children }: LayoutProps<"/admin">) {
  return <div className="min-h-full flex-1 bg-stone-50">{children}</div>;
}
