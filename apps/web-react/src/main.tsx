import React from 'react'
import ReactDOM from 'react-dom/client'
import { App } from './App.tsx'
import './styles/main.scss'
import i18n from './i18n'
import { useSettingsStore } from './store/useSettingsStore'

// PrimeReact basic styles
import "primereact/resources/themes/lara-dark-cyan/theme.css";
import "primereact/resources/primereact.min.css";
import "primeicons/primeicons.css";
import "primeflex/primeflex.css";

// Initial Theme Application
const initialSettings = useSettingsStore.getState();
const initialTheme = initialSettings.theme;
if (initialTheme === 'dark') {
  document.documentElement.classList.add('dark-theme');
} else {
  document.documentElement.classList.add('light-theme');
}

if (i18n.language !== initialSettings.language) {
  i18n.changeLanguage(initialSettings.language);
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
