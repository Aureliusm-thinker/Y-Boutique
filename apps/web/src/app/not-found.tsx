import Link from "next/link";

export default function NotFound() {
  return (
    <div className="mx-auto max-w-xl px-4 py-24 text-center">
      <h1 className="font-serif text-4xl">We couldn&apos;t find that</h1>
      <p className="mt-4 text-stone-600">The page or product you&apos;re looking for isn&apos;t available.</p>
      <Link href="/shop" className="mt-8 inline-block underline underline-offset-4">
        Continue shopping
      </Link>
    </div>
  );
}
