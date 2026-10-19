import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { WEB_PORT } from "./ports";

const proxyTarget = process.env.VITE_PROXY_TARGET ?? "http://localhost:8081";

/**
 * Vendor split for long-term caching and a smaller entry chunk.
 *
 * <p>Without this, every dependency reachable from main.tsx lands in the single
 * entry bundle: any app change invalidates React, the router and i18n for all
 * visitors. Splitting them keeps the hashed vendor files stable across deploys
 * and lets chart.js stay in its own lazy-loaded chunk instead of riding along
 * with whatever imports it. Only packages listed here are grouped; everything
 * else keeps Vite's automatic chunking, so no lazy view leaks into the initial
 * module graph.
 */
const VENDOR_GROUPS: Array<{ name: string; packages: RegExp }> = [
  { name: "vendor-react", packages: /^node_modules\/(react|react-dom|react-router|react-router-dom|scheduler|use-sync-external-store)\// },
  { name: "vendor-chart", packages: /^node_modules\/(chart\.js|react-chartjs-2)\// },
  { name: "vendor-i18n", packages: /^node_modules\/(i18next|i18next-browser-languagedetector|react-i18next)\// },
];

export default defineConfig({
  plugins: [react()],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          const match = id.replace(/\\/g, "/").match(/node_modules\/(.+)$/);
          if (!match) {
            return undefined;
          }
          const group = VENDOR_GROUPS.find((candidate) => candidate.packages.test(`node_modules/${match[1]}`));
          return group ? `${group.name}` : undefined;
        }
      }
    }
  },
  server: {
    host: true,
    port: WEB_PORT,
    proxy: {
      "/api": {
        target: proxyTarget,
        changeOrigin: true
      }
    }
  },
  // The suite runs against `vite preview`, so preview answering on the same port as
  // playwright.config's baseURL is what lets the workflow omit the flag.
  preview: {
    port: WEB_PORT
  }
});