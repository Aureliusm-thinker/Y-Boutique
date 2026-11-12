import type { NextConfig } from "next";
import type { RemotePattern } from "next/dist/shared/lib/image-config";

// Where uploaded product images are served from (the Java API locally; a CDN in production).
const mediaBase = new URL(process.env.MEDIA_PUBLIC_BASE_URL ?? "http://localhost:8080/media");
const mediaPattern: RemotePattern = {
  protocol: mediaBase.protocol.replace(":", "") as "http" | "https",
  hostname: mediaBase.hostname,
  port: mediaBase.port,
  pathname: `${mediaBase.pathname.replace(/\/$/, "")}/**`,
};
const mediaIsLocal = ["localhost", "127.0.0.1"].includes(mediaBase.hostname);

const nextConfig: NextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  images: {
    remotePatterns: [
      // Object form (no `search` key) so URLs with query strings like ?text=... are allowed.
      { protocol: "https", hostname: "placehold.co", pathname: "/**" }, // demo catalogue placeholders
      mediaPattern,
    ],
    // Only true when media is served from this machine (local development).
    dangerouslyAllowLocalIP: mediaIsLocal,
  },
  experimental: {
    serverActions: {
      // Product image uploads go through a server action (10 MB images + multipart overhead).
      bodySizeLimit: "11mb",
    },
  },
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
