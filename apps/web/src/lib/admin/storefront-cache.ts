import "server-only";
import { revalidateTag, updateTag } from "next/cache";

/**
 * After a staff change, make the shop show it to everyone straight away.
 * `updateTag` gives the staff member their own change immediately (read-your-own-writes); on its own,
 * other visitors were still served the old cached page for a few requests, so we also expire the tag
 * with `{ expire: 0 }` (no stale content for anyone). Only call from Server Actions.
 */
export function refreshStorefront(...tags: ("products" | "categories" | "reviews" | "content")[]) {
  for (const tag of tags) {
    updateTag(tag);
    revalidateTag(tag, { expire: 0 });
  }
}
