<script>
  import { createEventDispatcher } from "svelte";
  import { open } from "@tauri-apps/plugin-dialog";
  import { readFile } from "@tauri-apps/plugin-fs";
  const dispatch = createEventDispatcher();
  export let accept = ".svg"; // ".svg" | ".xml" | ".zip"

  $: extWord = accept === ".zip" ? "ZIP" : accept === ".xml" ? "XML" : "SVG";
  $: fileExt = accept === ".zip" ? "zip" : accept === ".xml" ? "xml" : "svg";

  let dragOver = false;

  function onDrop(e) {
    e.preventDefault();
    dragOver = false;
    const file = e.dataTransfer?.files?.[0];
    if (!file) return;
    readDropped(file);
  }

  function onDragOver(e) { e.preventDefault(); dragOver = true; }
  function onDragLeave() { dragOver = false; }

  function readDropped(file) {
    const reader = new FileReader();
    reader.onload = () => {
      dispatch("file", { bytes: new Uint8Array(reader.result), name: file.name });
    };
    reader.readAsArrayBuffer(file);
  }

  async function onClick() {
    const path = await open({ filters: [{ name: extWord, extensions: [fileExt] }], multiple: false });
    if (!path) return;
    const bytes = await readFile(path);
    dispatch("file", { bytes, name: path.split(/[\\/]/).pop() });
  }
</script>

<div
  class="dropzone"
  class:drag-over={dragOver}
  on:drop={onDrop}
  on:dragover={onDragOver}
  on:dragleave={onDragLeave}
  on:click={onClick}
  role="button"
  tabindex="0"
  on:keydown={(e) => e.key === "Enter" && onClick()}
  aria-label="Drop or click to choose file"
>
  <span class="drop-icon">📂</span>
  <p class="drop-label">Drop {accept === ".zip" ? "a ZIP" : `an ${extWord}`} here or click to browse</p>
</div>

<style>
  .dropzone {
    border: 2px dashed var(--border);
    border-radius: var(--radius);
    background: var(--surface);
    padding: 36px 20px;
    text-align: center;
    margin-bottom: 14px;
    transition: border-color .2s, background .2s;
    cursor: pointer;
  }

  .dropzone:hover,
  .dropzone.drag-over {
    border-color: var(--fresh-teal);
    background: color-mix(in srgb, var(--fresh-teal) 8%, var(--surface));
  }

  .drop-icon { font-size: 32px; display: block; margin-bottom: 10px; }
  .drop-label { font-size: 14px; font-weight: 600; color: var(--text-main); }
</style>
