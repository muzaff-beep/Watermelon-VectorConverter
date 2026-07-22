<script>
  import "../app.css";
  import { invoke } from "@tauri-apps/api/core";
  import { open } from "@tauri-apps/plugin-dialog";
  import { listen } from "@tauri-apps/api/event";
  import { onMount } from "svelte";

  let imgUrl = $state(null);
  let fileName = $state("");
  let error = $state("");
  let loading = $state(false);

  async function loadFile(path) {
    loading = true; error = "";
    try {
      const png = await invoke("render_file_preview", { path, px: 1024 });
      const blob = new Blob([new Uint8Array(png)], { type: "image/png" });
      if (imgUrl) URL.revokeObjectURL(imgUrl);
      imgUrl = URL.createObjectURL(blob);
      fileName = path.split(/[\\/]/).pop();
    } catch (e) {
      error = e?.message ?? String(e);
      imgUrl = null;
    } finally {
      loading = false;
    }
  }

  async function pickFile() {
    const path = await open({ filters: [{ name: "SVG / VectorDrawable", extensions: ["svg", "xml"] }], multiple: false });
    if (path) await loadFile(path);
  }

  onMount(async () => {
    // File that launched this instance (first run)
    const pending = await invoke("take_pending_file");
    if (pending) await loadFile(pending);

    // File passed to an already-running instance
    await listen("viewer://open-file", (e) => loadFile(e.payload));
  });
</script>

<div class="viewer">
  {#if imgUrl}
    <img src={imgUrl} alt={fileName} class="preview" />
    <div class="bar">
      <span class="filename">{fileName}</span>
      <button class="open-btn" onclick={pickFile}>Open…</button>
    </div>
  {:else}
    <div class="empty" onclick={pickFile} role="button" tabindex="0" onkeydown={(e) => e.key === "Enter" && pickFile()}>
      {#if loading}
        <p>Loading…</p>
      {:else if error}
        <p class="error">⚠ {error}</p>
        <p class="hint">Click to open a file</p>
      {:else}
        <p class="hint">Click to open an SVG or VectorDrawable XML</p>
      {/if}
    </div>
  {/if}
</div>

<style>
  .viewer { height: 100vh; display: flex; flex-direction: column; }

  .preview {
    flex: 1;
    width: 100%;
    object-fit: contain;
    background:
      linear-gradient(45deg, #1a1e24 25%, transparent 25%),
      linear-gradient(-45deg, #1a1e24 25%, transparent 25%),
      linear-gradient(45deg, transparent 75%, #1a1e24 75%),
      linear-gradient(-45deg, transparent 75%, #1a1e24 75%);
    background-size: 20px 20px;
    background-position: 0 0, 0 10px, 10px -10px, -10px 0px;
    background-color: var(--bg);
  }

  .bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 16px;
    background: var(--surface);
    border-top: 1px solid var(--border);
  }

  .filename { font-size: 13px; color: var(--text-sub); }

  .open-btn {
    background: var(--fresh-teal);
    color: #fff;
    padding: 6px 16px;
    border-radius: 50px;
    font-size: 13px;
    font-weight: 600;
  }

  .empty {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 10px;
    cursor: pointer;
  }

  .hint { color: var(--text-sub); font-size: 14px; }
  .error { color: #E63946; font-size: 13px; text-align: center; padding: 0 20px; }
</style>
