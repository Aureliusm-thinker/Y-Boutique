import Link from "next/link";
import { logout } from "@/app/(shop)/account/actions";

const LINKS = [
  { href: "/account", label: "Orders" },
  { href: "/account/wishlist", label: "Wishlist" },
  { href: "/account/profile", label: "Profile & style" },
  { href: "/account/addresses", label: "Addresses" },
  { href: "/account/privacy", label: "Privacy & data" },
];

/** Side navigation for the signed-in account area. */
export function AccountShell({ title, active, children }: { title: string; active: string; children: React.ReactNode }) {
  return (
    <div className="mx-auto grid max-w-6xl gap-10 px-4 py-12 md:grid-cols-[12rem_1fr]">
      <nav aria-label="Account" className="text-sm">
        <ul className="flex flex-wrap gap-x-4 gap-y-2 md:flex-col">
          {LINKS.map((l) => (
            <li key={l.href}>
              <Link href={l.href} aria-current={active === l.href ? "page" : undefined} className="hover:underline aria-[current=page]:font-medium aria-[current=page]:underline">
                {l.label}
              </Link>
            </li>
          ))}
          <li>
            <form action={logout}>
              <button className="text-stone-500 hover:underline">Sign out</button>
            </form>
          </li>
        </ul>
      </nav>
      <div>
        <h1 className="mb-8 font-serif text-4xl">{title}</h1>
        {children}
      </div>
    </div>
  );
}
