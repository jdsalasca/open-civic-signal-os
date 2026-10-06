import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { WEB_PORT } from "./ports";

const proxyTarget = process.env.VITE_PROXY_TARGET ?? "http://localhost:8081";

export default defineConfig({
  plugins: [react()],
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