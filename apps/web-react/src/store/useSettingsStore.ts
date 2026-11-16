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
export type TextScale = "normal" | "large";

/**
 * The browser locale decides the first language; a saved choice always wins.
 *
 * <p>Chrome in Colombia often reports `en-US` because the browser UI was installed in English,
 * while the person reads Spanish and lives in a Spanish-speaking timezone. Checking every
 * preferred language first and the operating-system timezone second keeps the app in the language
 * of the place, not of the installer. A location lookup can refine this later; an explicit choice
 * in the preferences wheel is never overridden.
 */
const SPANISH_TIMEZONES =
  /^(America\/(Bogota|Mexico_City|Cancun|Merida|Monterrey|Guatemala|El_Salvador|Tegucigalpa|Managua|Costa_Rica|Panama|Havana|Santo_Domingo|Puerto_Rico|Caracas|Lima|Guayaquil|La_Paz|Santiago|Asuncion|Montevideo|Argentina\/|Easter)|Atlantic\/Canary|Europe\/Madrid|Africa\/Ceuta)$/i;

const SPANISH_COUNTRIES = new Set([
  'colombia', 'méxico', 'mexico', 'guatemala', 'el salvador', 'honduras', 'nicaragua',
  'costa rica', 'panamá', 'panama', 'cuba', 'república dominicana', 'republica dominicana',
  'puerto rico', 'venezuela', 'perú', 'peru', 'ecuador', 'bolivia', 'chile', 'paraguay',
  'uruguay', 'argentina', 'españa', 'espana', 'spain'
]);

/** A country only ever adds Spanish; it never forces English over a person's device. */
export function languageForCountry(country?: string | null): 'es' | null {
  return country && SPANISH_COUNTRIES.has(country.trim().toLowerCase()) ? 'es' : null;
}

export function detectSystemLanguage(): 'en' | 'es' {
  if (typeof navigator !== 'undefined') {
    const preferred = [navigator.language, ...(navigator.languages ?? [])].filter(Boolean);
    if (preferred.some((lang) => lang.toLowerCase().startsWith('es'))) {
      return 'es';
    }
  }
  try {
    const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone ?? '';
    if (SPANISH_TIMEZONES.test(timeZone)) {
      return 'es';
    }
  } catch {
    // Older engines without Intl fall through to English.
  }
  return 'en';
}

interface SettingsState {
  language: 'en' | 'es';
  /** True only after the person picked a language; auto-detection is allowed to change the rest. */
  languageExplicit: boolean;
  theme: 'light' | 'dark' | 'contrast';
  interfaceMode: 'simple' | 'advanced';
  dataMode: DataMode;
  /** A single comfortable-reading switch: rem-based sizes scale together. */
  textScale: TextScale;
  setLanguage: (lang: 'en' | 'es') => void;
  /** Applies a language inferred from the country without turning it into a manual choice. */
  setDetectedLanguage: (lang: 'en' | 'es') => void;
  setTheme: (theme: 'light' | 'dark' | 'contrast') => void;
  setInterfaceMode: (mode: 'simple' | 'advanced') => void;
  setDataMode: (mode: DataMode) => void;
  setTextScale: (scale: TextScale) => void;
}

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      language: detectSystemLanguage(),
      languageExplicit: false,
      theme: 'dark',
      interfaceMode: 'simple',
      // Defaults to full: a field mode that silently reduced what everyone sees would be a change
      // nobody asked for. It is opt-in, and the setting says what it drops.
      dataMode: 'full',
      textScale: 'normal',
      setLanguage: (language) => set({ language, languageExplicit: true }),
      setDetectedLanguage: (language) =>
        set((state) => (state.languageExplicit ? state : { language })),
      setTheme: (theme) => set({ theme }),
      setInterfaceMode: (interfaceMode) => set({ interfaceMode }),
      setDataMode: (dataMode) => set({ dataMode }),
      setTextScale: (textScale) => set({ textScale })
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
