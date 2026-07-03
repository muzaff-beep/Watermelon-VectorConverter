<!-- Batch: drop a ZIP of SVGs, convert all, download result ZIP -->
<script>
  import { invoke } from "@tauri-apps/api/core";
  import { listen } from "@tauri-apps/api/event";
  import { open, save } from "@tauri-apps/plugin-dialog";
  import { readFile, writeFile } from "@tauri-apps/plugin-fs";
  import { onMount, onDestroy } from "svelte";
  import FileDropZone from "../../components/FileDropZone.svelte";
  import WatermelonButton from "../../components/WatermelonButton.svelte";
  import ProgressBar from "../../components/ProgressBar.svelte";

  let state = "idle"; // idle | working | done | error
  let sourceName = "";
  let resultZip = null;
  let errorMsg = "";

  // Live batch progress
  let progDone = 0;
  let progTotal = 0;
  let progName = "";
  let unlisten = null;

  onMount(async () => {
    unlisten = await listen("batch://progress", (event) => {
      const p = event.payload;
      progDone = p.done;
      progTotal = p.total;
      progName = p.current_name;
    });
  });

  onDestroy(() => { if (unlisten) unlisten(); });

  async function handleZip(bytes, name) {
    sourceName = name;
    state = "working";
    resultZip = null;
    progDone = 0;
    progTotal = 0;
    progName = "";
    try {
      const result = await invoke("convert_zip", { zip: Array.from(bytes) });
      resultZip = new Uint8Array(result);
      state = "done";
    } catch (e) {
      errorMsg = e?.message ?? String(e);
      state = "error";
    }
  }

  async function pickZip() {
    const path = await open({ filters: [{ name: "ZIP archive", extensions: ["zip"] }], multiple: false });
    if (!path) return;
    const bytes = await readFile(path);
    await handleZip(bytes, path.split(/[\\/]/).pop());
  }

  async function exportZip() {
    if (!resultZip) return;
    const outName = sourceName.replace(/\.zip$/i, "_vectors.zip");
    const path = await save({ defaultPath: outName, filters: [{ name: "ZIP archive", extensions: ["zip"] }] });
    if (!path) return;
    await writeFile(path, resultZip);
  }

  function reset() { state = "idle"; sourceName = ""; resultZip = null; errorMsg = ""; progDone = 0; progTotal = 0; progName = ""; }

  $: progPct = progTotal > 0 ? Math.round((progDone / progTotal) * 100) : 0;
</script>

<section class="batch">
  <h1 class="page-title">Batch convert a ZIP</h1>
  <p class="subtitle">Drop a ZIP containing SVG files. All SVGs will be converted and returned as a ZIP of VectorDrawable XMLs.</p>

  {#if state === "idle"}
    <FileDropZone accept=".zip" on:file={(e) => handleZip(e.detail.bytes, e.detail.name)} />
    <WatermelonButton on:click={pickZip} label="Browse for ZIP…" variant="primary" />

  {:else if state === "working"}
    {#if progTotal > 0}
      <ProgressBar label="{progDone} / {progTotal} — {progName}" value={progPct} />
    {:else}
      <ProgressBar label="Reading {sourceName}…" indeterminate />
    {/if}

  {:else if state === "done"}
    <div class="done-box">
      <p class="success">✓ Batch complete — {sourceName}</p>
      <div class="actions">
        <WatermelonButton on:click={exportZip} label="Save result ZIP" variant="teal" />
        <WatermelonButton on:click={reset} label="New batch" variant="outline" />
      </div>
    </div>

  {:else if state === "error"}
    <div class="error-box">
      <p>⚠ Batch failed</p>
      <p class="error-msg">{errorMsg}</p>
      <WatermelonButton on:click={reset} label="Try again" variant="outline" />
    </div>
  {/if}
</section>

<style>
  .batch { max-width: 700px; margin: 0 auto; }
  .page-title { font-size: 22px; font-weight: 700; color: var(--deep-navy); margin-bottom: 8px; }
  .subtitle { color: var(--slate-gray); font-size: 14px; margin-bottom: 28px; }
  .done-box { background: #f0faf8; border: 1px solid #a8ddd5; border-radius: var(--radius); padding: 28px; }
  .success { font-weight: 600; color: var(--fresh-teal); margin-bottom: 16px; }
  .actions { display: flex; gap: 10px; }
  .error-box { background: #fff0f0; border: 1px solid #f5c6c6; border-radius: var(--radius); padding: 24px; }
  .error-box p { color: var(--watermelon-red); font-weight: 600; margin-bottom: 8px; }
  .error-msg { font-family: monospace; font-size: 13px; color: #c0392b; margin-bottom: 16px; }
</style>
