"use client";

import { useEffect } from "react";
import { track } from "@/lib/analytics";

/** Fires a single analytics event when mounted (e.g. product_view on a product page). */
export function TrackView({ name, properties }: { name: string; properties: Record<string, unknown> }) {
  const key = JSON.stringify(properties);
  useEffect(() => {
    track(name, JSON.parse(key));
  }, [name, key]);
  return null;
}
