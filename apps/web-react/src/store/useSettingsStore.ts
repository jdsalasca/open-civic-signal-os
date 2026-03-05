import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface SettingsState {
  language: 'en' | 'es';
  theme: 'light' | 'dark';
  interfaceMode: 'simple' | 'advanced';
  setLanguage: (lang: 'en' | 'es') => void;
  setTheme: (theme: 'light' | 'dark') => void;
  setInterfaceMode: (mode: 'simple' | 'advanced') => void;
}

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      language: 'en',
      theme: 'dark',
      interfaceMode: 'simple',
      setLanguage: (language) => set({ language }),
      setTheme: (theme) => set({ theme }),
      setInterfaceMode: (interfaceMode) => set({ interfaceMode })
    }),
    {
      name: 'settings-storage'
    }
  )
);
