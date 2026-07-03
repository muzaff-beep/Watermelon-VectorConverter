<!-- Convert: single SVG (left) + batch ZIP (right) — side by side on PC -->
<script>
  import { invoke } from "@tauri-apps/api/core";
  import { listen } from "@tauri-apps/api/event";
  import { open, save } from "@tauri-apps/plugin-dialog";
  import { readFile, writeFile } from "@tauri-apps/plugin-fs";
  import { get } from "svelte/store";
  import { settings } from "../lib/settings";
  import { onMount, onDestroy } from "svelte";
  import FileDropZone from "../components/FileDropZone.svelte";
  import PreviewPane from "../components/PreviewPane.svelte";
  import WatermelonButton from "../components/WatermelonButton.svelte";
  import ProgressBar from "../components/ProgressBar.svelte";

  // ── Single file state ──
  let sState = "idle";
  let svgBytes = null;
  let svgPreviewPng = null;
  let vdPreviewPng = null;
  let vdXml = "";
  let sourceName = "";
  let sError = "";

  $: vdSizeKb = vdXml ? (new TextEncoder().encode(vdXml).length / 1024).toFixed(1) : "0";
  $: vdLines  = vdXml ? vdXml.split("\n").length : 0;

  async function handleSvg(bytes, name) {
    svgBytes = bytes; sourceName = name;
    sState = "working"; svgPreviewPng = null; vdPreviewPng = null; vdXml = "";
    try {
      vdXml = await invoke("convert_svg", { svg: Array.from(bytes) });
      const px = get(settings).previewSize;
      const [sp, vp] = await Promise.all([
        invoke("render_svg_preview", { svg: Array.from(bytes), px }),
        invoke("render_vd_preview", { vdXml, px }),
      ]);
      svgPreviewPng = sp; vdPreviewPng = vp; sState = "done";
    } catch (e) { sError = e?.message ?? String(e); sState = "error"; }
  }

  async function pickSvg() {
    const path = await open({ filters: [{ name: "SVG", extensions: ["svg"] }], multiple: false });
    if (!path) return;
    const bytes = await readFile(path);
    await handleSvg(bytes, path.split(/[\\\/]/).pop());
  }

  async function exportSvg() {
    if (!vdXml) return;
    const path = await save({ defaultPath: sourceName.replace(/\.svg$/i, ".xml"), filters: [{ name: "VectorDrawable", extensions: ["xml"] }] });
    if (!path) return;
    await writeFile(path, new TextEncoder().encode(vdXml));
  }

  function resetSingle() { sState = "idle"; svgBytes = null; svgPreviewPng = null; vdPreviewPng = null; vdXml = ""; sourceName = ""; sError = ""; }

  // ── Batch state ──
  let bState = "idle";
  let bSourceName = "";
  let resultZip = null;
  let bError = "";
  let progDone = 0, progTotal = 0, progName = "";
  let unlisten = null;

  onMount(async () => {
    unlisten = await listen("batch://progress", (e) => {
      const p = e.payload;
      progDone = p.done; progTotal = p.total; progName = p.current_name;
    });
  });
  onDestroy(() => { if (unlisten) unlisten(); });

  async function handleZip(bytes, name) {
    bSourceName = name; bState = "working"; resultZip = null;
    progDone = 0; progTotal = 0; progName = "";
    try {
      const result = await invoke("convert_zip", { zip: Array.from(bytes) });
      resultZip = new Uint8Array(result); bState = "done";
    } catch (e) { bError = e?.message ?? String(e); bState = "error"; }
  }

  async function pickZip() {
    const path = await open({ filters: [{ name: "ZIP", extensions: ["zip"] }], multiple: false });
    if (!path) return;
    const bytes = await readFile(path);
    await handleZip(bytes, path.split(/[\\\/]/).pop());
  }

  async function exportZip() {
    if (!resultZip) return;
    const path = await save({ defaultPath: bSourceName.replace(/\.zip$/i, "_vectors.zip"), filters: [{ name: "ZIP", extensions: ["zip"] }] });
    if (!path) return;
    await writeFile(path, resultZip);
  }

  function resetBatch() { bState = "idle"; bSourceName = ""; resultZip = null; bError = ""; progDone = 0; progTotal = 0; progName = ""; }

  $: progPct = progTotal > 0 ? Math.round((progDone / progTotal) * 100) : 0;
</script>

<div class="convert-layout">

  <!-- ── Left: Single SVG ── -->
  <section class="panel">
    <h2 class="panel-title">Single SVG</h2>

    {#if sState === "idle"}
      <FileDropZone on:file={(e) => handleSvg(e.detail.bytes, e.detail.name)} />
      <WatermelonButton on:click={pickSvg} label="Browse for SVG…" variant="primary" />

    {:else if sState === "working"}
      <ProgressBar label="Converting {sourceName}…" indeterminate />

    {:else if sState === "done"}
      <div class="result-header">
        <span class="success-badge">✓ {sourceName}</span>
        <div class="row-gap">
          <WatermelonButton on:click={exportSvg} label="Export .xml" variant="teal" />
          <WatermelonButton on:click={resetSingle} label="New" variant="outline" />
        </div>
      </div>
      <div class="report-card">
        <div class="report-item">
          <span class="report-value">{vdSizeKb} KB</span>
          <span class="report-label">Output size</span>
        </div>
        <div class="report-divider"></div>
        <div class="report-item">
          <span class="report-value">{vdLines}</span>
          <span class="report-label">XML lines</span>
        </div>
      </div>
      <PreviewPane {svgPreviewPng} {vdPreviewPng} {vdXml} />

    {:else if sState === "error"}
      <div class="error-box">
        <p class="error-title">⚠ Conversion failed</p>
        <p class="error-msg">{sError}</p>
        <WatermelonButton on:click={resetSingle} label="Try again" variant="outline" />
      </div>
    {/if}
  </section>

  <div class="divider"></div>

  <!-- ── Right: Batch ZIP ── -->
  <section class="panel">
    <h2 class="panel-title">Batch ZIP</h2>
    <p class="panel-sub">Drop a ZIP of SVG files — convert all at once.</p>

    {#if bState === "idle"}
      <FileDropZone accept=".zip" on:file={(e) => handleZip(e.detail.bytes, e.detail.name)} />
      <WatermelonButton on:click={pickZip} label="Browse for ZIP…" variant="primary" />

    {:else if bState === "working"}
      {#if progTotal > 0}
        <ProgressBar label="{progDone} / {progTotal} — {progName}" value={progPct} />
      {:else}
        <ProgressBar label="Reading {bSourceName}…" indeterminate />
      {/if}

    {:else if bState === "done"}
      <div class="done-box">
        <p class="success-badge">✓ {bSourceName}</p>
        <div class="row-gap" style="margin-top:14px">
          <WatermelonButton on:click={exportZip} label="Save result ZIP" variant="teal" />
          <WatermelonButton on:click={resetBatch} label="New batch" variant="outline" />
        </div>
      </div>

    {:else if bState === "error"}
      <div class="error-box">
        <p class="error-title">⚠ Batch failed</p>
        <p class="error-msg">{bError}</p>
        <WatermelonButton on:click={resetBatch} label="Try again" variant="outline" />
      </div>
    {/if}
  </section>

</div>

<style>
  .convert-layout {
    display: flex;
    gap: 0;
    height: 100%;
    overflow: hidden;
  }

  .panel {
    flex: 1;
    overflow-y: auto;
    padding: 4px 4px 4px 0;
  }

  .panel:last-child { padding: 4px 0 4px 4px; }

  .divider {
    width: 1px;
    background: var(--border);
    margin: 0 20px;
    flex-shrink: 0;
  }

  .panel-title {
    font-size: 17px;
    font-weight: 700;
    color: var(--text-main);
    margin-bottom: 6px;
  }

  .panel-sub {
    font-size: 13px;
    color: var(--text-sub);
    margin-bottom: 16px;
  }

  .result-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;
    flex-wrap: wrap;
    gap: 8px;
  }

  .success-badge { font-weight: 600; color: var(--fresh-teal); font-size: 14px; }

  .row-gap { display: flex; gap: 8px; }

  .report-card {
    display: flex;
    align-items: center;
    background: var(--card-bg);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    padding: 14px 22px;
    margin-bottom: 16px;
    gap: 22px;
  }
  .report-item { display: flex; flex-direction: column; gap: 2px; }
  .report-value { font-size: 18px; font-weight: 700; color: var(--text-main); }
  .report-label { font-size: 11px; color: var(--text-sub); text-transform: uppercase; letter-spacing: .04em; }
  .report-divider { width: 1px; height: 32px; background: var(--border); }

  .done-box {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    padding: 24px;
  }

  .error-box {
    background: color-mix(in srgb, var(--watermelon-red) 8%, var(--card-bg));
    border: 1px solid color-mix(in srgb, var(--watermelon-red) 30%, transparent);
    border-radius: var(--radius);
    padding: 20px;
  }
  .error-title { color: var(--watermelon-red); font-weight: 600; margin-bottom: 6px; }
  .error-msg { font-family: monospace; font-size: 12px; color: var(--text-sub); margin-bottom: 14px; word-break: break-all; }
</style>
