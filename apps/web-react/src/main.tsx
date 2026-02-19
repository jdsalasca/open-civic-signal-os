import React from 'react'
import ReactDOM from 'react-dom/client'
import { App } from './App.tsx'
import './styles.css'
import './i18n' // Import i18n configuration
import { useSettingsStore } from './store/useSettingsStore'

// PrimeReact basic styles
import "primereact/resources/themes/lara-dark-cyan/theme.css";
import "primereact/resources/primereact.min.css";
import "primeicons/primeicons.css";
import "primeflex/primeflex.css";

// Initial Theme Application
const initialTheme = useSettingsStore.getState().theme;
if (initialTheme === 'dark') {
  document.documentElement.classList.add('dark-theme');
} else {
  document.documentElement.classList.add('light-theme');
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
