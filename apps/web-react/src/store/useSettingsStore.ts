import { create } from "zustand";
import { persist } from "zustand/middleware";

/**
 * How much the client is willing to fetch.
 *
 * <p>Distinct from `interfaceMode`, which changes how much chrome is shown. This changes how much
 * data crosses the network, because a field worker on a slow connection pays for bytes, not for
 * buttons. The two are independent on purpose: someone on a fast connection may still want the
 * simple layout, and someone on a slow one may still want every panel.
 */
export type DataMode = "full" | "field";

interface SettingsState {
  language: 'en' | 'es';
  theme: 'light' | 'dark';
  interfaceMode: 'simple' | 'advanced';
  dataMode: DataMode;
  setLanguage: (lang: 'en' | 'es') => void;
  setTheme: (theme: 'light' | 'dark') => void;
  setInterfaceMode: (mode: 'simple' | 'advanced') => void;
  setDataMode: (mode: DataMode) => void;
}

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      language: 'en',
      theme: 'dark',
      interfaceMode: 'simple',
      // Defaults to full: a field mode that silently reduced what everyone sees would be a change
      // nobody asked for. It is opt-in, and the setting says what it drops.
      dataMode: 'full',
      setLanguage: (language) => set({ language }),
      setTheme: (theme) => set({ theme }),
      setInterfaceMode: (interfaceMode) => set({ interfaceMode }),
      setDataMode: (dataMode) => set({ dataMode })
    }),
    {
      name: 'settings-storage'
    }
  )
);

/**
 * What field mode drops, in one place so the setting and the dashboard cannot disagree.
 *
 * <p>Each entry is a request the dashboard would otherwise make. Stated as data rather than as
 * scattered conditionals so the setting screen can list exactly what is being skipped, which is the
 * difference between a mode and a mystery.
 */
export const FIELD_MODE_SKIPS = [
  'signals/duplicates',
  'signals/aging',
  'notifications/recent',
] as const;

/** Rows requested per page in each mode. Field mode asks for fewer because each row is bytes. */
export const PAGE_SIZE_BY_DATA_MODE: Record<DataMode, number> = {
  full: 10,
  field: 5,
};
