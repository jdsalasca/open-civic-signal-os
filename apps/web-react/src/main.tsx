import React from 'react'
import ReactDOM from 'react-dom/client'
import { App } from './App.tsx'
import i18n from './i18n'
import { detectSystemLanguage, useSettingsStore } from './store/useSettingsStore'

// PrimeReact base styles first, so the Paper Craft tokens and component
// overrides in main.scss win the cascade without more !important.
import "primereact/resources/themes/lara-dark-cyan/theme.css";
import "primereact/resources/primereact.min.css";
import "primeicons/primeicons.css";
import "primeflex/primeflex.css";

import './styles/main.scss';

// Initial Theme Application
const initialSettings = useSettingsStore.getState();
const initialTheme = initialSettings.theme;
const initialInterfaceMode = initialSettings.interfaceMode;
document.documentElement.dataset.textScale = initialSettings.textScale ?? 'normal';
document.documentElement.classList.add(`${initialTheme}-theme`);
// The attribute mirrors the class: CSS understands both, and a future
// "follow the system" mode can simply drop both and let prefers-color-scheme
// decide.
document.documentElement.dataset.theme = initialTheme;
document.documentElement.classList.add(`interface-mode-${initialInterfaceMode}`);

// A language the person never picked is only a guess: recompute it from the device and the
// timezone, so an English browser installed in Colombia does not persist an English UI forever.
const initialLanguage = initialSettings.languageExplicit
  ? initialSettings.language
  : detectSystemLanguage();
if (initialLanguage !== initialSettings.language) {
  useSettingsStore.setState({ language: initialLanguage });
}
if (i18n.language !== initialLanguage) {
  i18n.changeLanguage(initialLanguage);
}

// Field use means flaky mobile networks. The production build registers a service worker that keeps
// the shell and hashed assets, so reopening the app offline lands on the form instead of a browser
// error. Development is excluded: a cached shell would serve yesterday's code after every edit.
if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {
      // A missing worker only costs offline support; the app itself still works.
    });
  });
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
