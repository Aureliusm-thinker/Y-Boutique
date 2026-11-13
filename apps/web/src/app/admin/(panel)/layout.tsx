import Link from "next/link";
import { Suspense } from "react";
import { getCurrentStaff } from "@/lib/admin/api";
import { can } from "@/lib/admin/types";

// Every admin page depends on the signed-in user, so these routes render per request.
export const instant = false;

async function Nav() {
  const me = await getCurrentStaff();
  const links = [
    { href: "/admin", label: "Dashboard", show: true },
    { href: "/admin/products", label: "Products", show: can.viewCatalog(me.role) },
    { href: "/admin/categories", label: "Categories", show: can.viewCatalog(me.role) },
    { href: "/admin/inventory", label: "Inventory", show: can.viewCatalog(me.role) },
    { href: "/admin/staff", label: "Staff", show: can.manageStaff(me.role) },
    { href: "/admin/security", label: "Security", show: true },
  ];
  return (
    <>
      <nav className="flex flex-wrap gap-1 text-sm">
        {links
          .filter((l) => l.show)
          .map((l) => (
            <Link key={l.href} href={l.href} className="rounded px-3 py-1.5 text-stone-700 hover:bg-stone-200">
              {l.label}
            </Link>
          ))}
      </nav>
      <div className="flex items-center gap-3 text-sm">
        <span className="text-stone-600">
          {me.fullName} · <span className="text-xs uppercase tracking-wide">{me.role.toLowerCase()}</span>
        </span>
        {/* Plain HTML post (not a server action) so sign-out is a full page load. */}
        <form method="post" action="/admin/logout">
          <button className="admin-btn">Sign out</button>
        </form>
      </div>
    </>
  );
}

export default function PanelLayout({ children }: LayoutProps<"/admin">) {
  return (
    <>
      <header className="border-b border-stone-200 bg-white">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-3">
          <Link href="/admin" className="font-serif text-2xl">
            Yvette <span className="text-sm font-sans text-stone-500">admin</span>
          </Link>
          <Suspense fallback={<div className="h-8" />}>
            <Nav />
          </Suspense>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8">{children}</main>
    </>
  );
}
