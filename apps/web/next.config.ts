import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  images: {
    // Placeholder images for the demo catalogue. Replace with the image CDN (Cloudinary/S3) host.
    // Object form (no `search` key) so URLs with query strings like ?text=... are allowed.
    remotePatterns: [{ protocol: "https", hostname: "placehold.co", pathname: "/**" }],
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
