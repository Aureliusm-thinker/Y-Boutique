import Link from "next/link";
import { cookies } from "next/headers";
import { CUSTOMER_COOKIE } from "@/lib/shop-api";

/** "Sign in" or "Account" in the header (reads a cookie, so render inside <Suspense>). */
export async function AccountLink() {
  const signedIn = Boolean((await cookies()).get(CUSTOMER_COOKIE)?.value);
  return (
    <Link href={signedIn ? "/account" : "/account/login"} className="hover:text-stone-950">
      {signedIn ? "Account" : "Sign in"}
    </Link>
  );
}
