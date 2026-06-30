import adapter from "@sveltejs/adapter-static";
import { vitePreprocess } from "@sveltejs/vite-plugin-svelte";

export default {
  preprocess: vitePreprocess(),
  kit: {
    adapter: adapter({
      // Output to dist/ — matches tauri.conf.json frontendDist: "../dist"
      pages: "dist",
      assets: "dist",
      fallback: "index.html",
      precompress: false,
      strict: false,
    }),
  },
};
