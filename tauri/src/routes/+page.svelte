<!-- Convert: two directions stacked vertically, each with a Single/Batch tab. -->
<script>
  import { invoke } from "@tauri-apps/api/core";
  import { listen } from "@tauri-apps/api/event";
  import { save } from "@tauri-apps/plugin-dialog";
  import { writeFile } from "@tauri-apps/plugin-fs";
  import { get } from "svelte/store";
  import { settings } from "../lib/settings";
  import { onMount, onDestroy } from "svelte";
  import FileDropZone from "../components/FileDropZone.svelte";
  import PreviewPane from "../components/PreviewPane.svelte";
  import WatermelonButton from "../components/WatermelonButton.svelte";
  import ProgressBar from "../components/ProgressBar.svelte";

  // ── Forward direction: SVG -> XML ──
  let fwdTab = "single"; // "single" | "batch"

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

  async function exportSvg() {
    if (!vdXml) return;
    const path = await save({ defaultPath: sourceName.replace(/\.svg$/i, ".xml"), filters: [{ name: "VectorDrawable", extensions: ["xml"] }] });
    if (!path) return;
    await writeFile(path, new TextEncoder().encode(vdXml));
  }

  function resetSingle() { sState = "idle"; svgBytes = null; svgPreviewPng = null; vdPreviewPng = null; vdXml = ""; sourceName = ""; sError = ""; }

  let bState = "idle";
  let bSourceName = "";
  let resultZip = null;
  let bError = "";

  async function handleZip(bytes, name) {
    bSourceName = name; bState = "working"; resultZip = null;
    progDone = 0; progTotal = 0; progName = "";
    try {
      const result = await invoke("convert_zip", { zip: Array.from(bytes) });
      resultZip = new Uint8Array(result); bState = "done";
    } catch (e) { bError = e?.message ?? String(e); bState = "error"; }
  }

  async function exportZip() {
    if (!resultZip) return;
    const path = await save({ defaultPath: bSourceName.replace(/\.zip$/i, "_vectors.zip"), filters: [{ name: "ZIP", extensions: ["zip"] }] });
    if (!path) return;
    await writeFile(path, resultZip);
  }

  function resetBatch() { bState = "idle"; bSourceName = ""; resultZip = null; bError = ""; progDone = 0; progTotal = 0; progName = ""; }

  // ── Reverse direction: XML -> SVG ──
  let revTab = "single"; // "single" | "batch"

  let rState = "idle";
  let vdBytes = null;
  let rVdPreviewPng = null;
  let rSvgPreviewPng = null;
  let svgOut = "";
  let rSourceName = "";
  let rError = "";

  $: svgSizeKb = svgOut ? (new TextEncoder().encode(svgOut).length / 1024).toFixed(1) : "0";
  $: svgLines  = svgOut ? svgOut.split("\n").length : 0;

  async function handleVd(bytes, name) {
    vdBytes = bytes; rSourceName = name;
    rState = "working"; rVdPreviewPng = null; rSvgPreviewPng = null; svgOut = "";
    try {
      svgOut = await invoke("convert_vd", { vdXml: Array.from(bytes) });
      const px = get(settings).previewSize;
      const [vp, sp] = await Promise.all([
        invoke("render_vd_preview", { vdXml: new TextDecoder().decode(bytes), px }),
        invoke("render_svg_preview", { svg: Array.from(new TextEncoder().encode(svgOut)), px }),
      ]);
      rVdPreviewPng = vp; rSvgPreviewPng = sp; rState = "done";
    } catch (e) { rError = e?.message ?? String(e); rState = "error"; }
  }

  async function exportVdAsSvg() {
    if (!svgOut) return;
    const path = await save({ defaultPath: rSourceName.replace(/\.xml$/i, ".svg"), filters: [{ name: "SVG", extensions: ["svg"] }] });
    if (!path) return;
    await writeFile(path, new TextEncoder().encode(svgOut));
  }

  function resetRevSingle() { rState = "idle"; vdBytes = null; rVdPreviewPng = null; rSvgPreviewPng = null; svgOut = ""; rSourceName = ""; rError = ""; }

  let rbState = "idle";
  let rbSourceName = "";
  let rResultZip = null;
  let rbError = "";

  async function handleVdZip(bytes, name) {
    rbSourceName = name; rbState = "working"; rResultZip = null;
    progDone = 0; progTotal = 0; progName = "";
    try {
      const result = await invoke("convert_vd_zip", { zip: Array.from(bytes) });
      rResultZip = new Uint8Array(result); rbState = "done";
    } catch (e) { rbError = e?.message ?? String(e); rbState = "error"; }
  }

  async function exportVdZip() {
    if (!rResultZip) return;
    const path = await save({ defaultPath: rbSourceName.replace(/\.zip$/i, "_svgs.zip"), filters: [{ name: "ZIP", extensions: ["zip"] }] });
    if (!path) return;
    await writeFile(path, rResultZip);
  }

  function resetRevBatch() { rbState = "idle"; rbSourceName = ""; rResultZip = null; rbError = ""; progDone = 0; progTotal = 0; progName = ""; }

  // ── Shared batch progress (only one batch direction runs at a time) ──
  let progDone = 0, progTotal = 0, progName = "";
  let unlisten = null;

  onMount(async () => {
    unlisten = await listen("batch://progress", (e) => {
      const p = e.payload;
      progDone = p.done; progTotal = p.total; progName = p.current_name;
    });
  });
  onDestroy(() => { if (unlisten) unlisten(); });

  $: progPct = progTotal > 0 ? Math.round((progDone / progTotal) * 100) : 0;
</script>

<div class="convert-stack">

  <!-- ── Section 1: SVG -> XML ── -->
  <section class="direction-section">
    <div class="section-head">
      <h2 class="section-title">Convert SVG to XML</h2>
      <div class="tabs">
        <button class="tab" class:active={fwdTab === "single"} on:click={() => fwdTab = "single"}>Single</button>
        <button class="tab" class:active={fwdTab === "batch"} on:click={() => fwdTab = "batch"}>Batch</button>
      </div>
    </div>

    {#if fwdTab === "single"}
      {#if sState === "idle"}
        <FileDropZone accept=".svg" on:file={(e) => handleSvg(e.detail.bytes, e.detail.name)} />
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
          <div class="report-item"><span class="report-value">{vdSizeKb} KB</span><span class="report-label">Output size</span></div>
          <div class="report-divider"></div>
          <div class="report-item"><span class="report-value">{vdLines}</span><span class="report-label">XML lines</span></div>
        </div>
        <PreviewPane {svgPreviewPng} {vdPreviewPng} vdXml={vdXml} direction="forward" />
      {:else if sState === "error"}
        <div class="error-box">
          <p class="error-title">⚠ Conversion failed</p>
          <p class="error-msg">{sError}</p>
          <WatermelonButton on:click={resetSingle} label="Try again" variant="outline" />
        </div>
      {/if}
    {:else}
      <p class="panel-sub">Drop a ZIP of SVG files — convert all at once.</p>
      {#if bState === "idle"}
        <FileDropZone accept=".zip" on:file={(e) => handleZip(e.detail.bytes, e.detail.name)} />
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
    {/if}
  </section>

  <div class="section-divider"></div>

  <!-- ── Section 2: XML -> SVG ── -->
  <section class="direction-section">
    <div class="section-head">
      <h2 class="section-title">Convert XML to SVG</h2>
      <div class="tabs">
        <button class="tab" class:active={revTab === "single"} on:click={() => revTab = "single"}>Single</button>
        <button class="tab" class:active={revTab === "batch"} on:click={() => revTab = "batch"}>Batch</button>
      </div>
    </div>

    {#if revTab === "single"}
      {#if rState === "idle"}
        <FileDropZone accept=".xml" on:file={(e) => handleVd(e.detail.bytes, e.detail.name)} />
      {:else if rState === "working"}
        <ProgressBar label="Converting {rSourceName}…" indeterminate />
      {:else if rState === "done"}
        <div class="result-header">
          <span class="success-badge">✓ {rSourceName}</span>
          <div class="row-gap">
            <WatermelonButton on:click={exportVdAsSvg} label="Export .svg" variant="teal" />
            <WatermelonButton on:click={resetRevSingle} label="New" variant="outline" />
          </div>
        </div>
        <div class="report-card">
          <div class="report-item"><span class="report-value">{svgSizeKb} KB</span><span class="report-label">Output size</span></div>
          <div class="report-divider"></div>
          <div class="report-item"><span class="report-value">{svgLines}</span><span class="report-label">SVG lines</span></div>
        </div>
        <PreviewPane svgPreviewPng={rSvgPreviewPng} vdPreviewPng={rVdPreviewPng} vdXml={svgOut} direction="reverse" />
      {:else if rState === "error"}
        <div class="error-box">
          <p class="error-title">⚠ Conversion failed</p>
          <p class="error-msg">{rError}</p>
          <WatermelonButton on:click={resetRevSingle} label="Try again" variant="outline" />
        </div>
      {/if}
    {:else}
      <p class="panel-sub">Drop a ZIP of VectorDrawable XML files — convert all at once.</p>
      {#if rbState === "idle"}
        <FileDropZone accept=".zip" on:file={(e) => handleVdZip(e.detail.bytes, e.detail.name)} />
      {:else if rbState === "working"}
        {#if progTotal > 0}
          <ProgressBar label="{progDone} / {progTotal} — {progName}" value={progPct} />
        {:else}
          <ProgressBar label="Reading {rbSourceName}…" indeterminate />
        {/if}
      {:else if rbState === "done"}
        <div class="done-box">
          <p class="success-badge">✓ {rbSourceName}</p>
          <div class="row-gap" style="margin-top:14px">
            <WatermelonButton on:click={exportVdZip} label="Save result ZIP" variant="teal" />
            <WatermelonButton on:click={resetRevBatch} label="New batch" variant="outline" />
          </div>
        </div>
      {:else if rbState === "error"}
        <div class="error-box">
          <p class="error-title">⚠ Batch failed</p>
          <p class="error-msg">{rbError}</p>
          <WatermelonButton on:click={resetRevBatch} label="Try again" variant="outline" />
        </div>
      {/if}
    {/if}
  </section>

</div>

<style>
  .convert-stack {
    display: flex;
    flex-direction: column;
    height: 100%;
    overflow-y: auto;
    padding: 4px;
    gap: 0;
  }

  .direction-section { padding: 4px 0; }

  .section-divider {
    height: 1px;
    background: var(--border);
    margin: 20px 0;
    flex-shrink: 0;
  }

  .section-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;
    flex-wrap: wrap;
    gap: 10px;
  }

  .section-title {
    font-size: 17px;
    font-weight: 700;
    color: var(--text-main);
  }

  .tabs {
    display: flex;
    gap: 4px;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 999px;
    padding: 3px;
  }

  .tab {
    border: none;
    background: none;
    padding: 6px 16px;
    font-size: 13px;
    font-weight: 600;
    color: var(--text-sub);
    border-radius: 999px;
    cursor: pointer;
    transition: background .15s, color .15s;
  }

  .tab.active {
    background: var(--fresh-teal);
    color: white;
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
