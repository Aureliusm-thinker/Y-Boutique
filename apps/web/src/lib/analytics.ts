"use client";

// First-party behavioural tracking — the data foundation for AI/ML (docs/04-ai-ml-strategy.md §3.1).
// Events are only sent after the visitor accepts analytics in the consent banner.

const PUBLIC_API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const CONSENT_KEY = "yvette.consent";
const ANON_KEY = "yvette.anonymousId";

export type Consent = "granted" | "denied";

function storage(): Storage | null {
  try {
    return window.localStorage;
  } catch {
    return null;
  }
}

export function getConsent(): Consent | null {
  const value = storage()?.getItem(CONSENT_KEY);
  return value === "granted" || value === "denied" ? value : null;
}

export function setConsent(consent: Consent) {
  storage()?.setItem(CONSENT_KEY, consent);
}

function anonymousId(): string {
  const store = storage();
  let id = store?.getItem(ANON_KEY);
  if (!id) {
    id = crypto.randomUUID();
    store?.setItem(ANON_KEY, id);
  }
  return id;
}

export function track(name: string, properties: Record<string, unknown> = {}) {
  if (getConsent() !== "granted") return;
  const body = JSON.stringify({
    eventId: crypto.randomUUID(),
    name,
    anonymousId: anonymousId(),
    properties,
    occurredAt: new Date().toISOString(),
  });
  // keepalive lets the request finish even if the user navigates away.
  fetch(`${PUBLIC_API_URL}/api/v1/events`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body,
    keepalive: true,
  }).catch(() => {
    /* tracking must never break the shop */
  });
}
