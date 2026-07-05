// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Shared reactive settings store. Persisted to localStorage.

import { writable } from "svelte/store";

export interface Settings {
  previewSize: number;   // px used for preview rendering
  darkMode: boolean;
  hasSeenAssocNotice: boolean; // first-run file-association dialog shown?
}

const DEFAULTS: Settings = {
  previewSize: 400,
  darkMode: false,
  hasSeenAssocNotice: false,
};

const STORAGE_KEY = "wvgc-settings";

function load(): Settings {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return { ...DEFAULTS, ...JSON.parse(raw) };
  } catch {
    /* ignore */
  }
  return { ...DEFAULTS };
}

function createSettings() {
  const { subscribe, set, update } = writable<Settings>(load());

  return {
    subscribe,
    set: (s: Settings) => {
      try { localStorage.setItem(STORAGE_KEY, JSON.stringify(s)); } catch { /* ignore */ }
      set(s);
    },
    update: (fn: (s: Settings) => Settings) =>
      update((s) => {
        const next = fn(s);
        try { localStorage.setItem(STORAGE_KEY, JSON.stringify(next)); } catch { /* ignore */ }
        return next;
      }),
    reset: () => set({ ...DEFAULTS }),
  };
}

export const settings = createSettings();
