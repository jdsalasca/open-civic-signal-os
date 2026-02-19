import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

const resources = {
  en: {
    translation: {
      "nav": {
        "insights": "Insights",
        "report": "Public Report",
        "my_contributions": "My Contributions",
        "moderation": "Moderation",
        "settings": "Settings",
        "sign_out": "Sign Out"
      },
      "dashboard": {
        "welcome": "Welcome back",
        "live_intel": "Live Intelligence",
        "title": "Governance Insights",
        "subtitle": "Real-time community signal prioritization and resolution lifecycle tracking.",
        "broadcast": "Weekly Broadcast",
        "new_issue": "Report New Issue"
      },
      "metrics": {
        "total": "Total Signals",
        "new": "Newly Reported",
        "analysis": "Under Analysis",
        "avg": "Priority Avg"
      },
      "settings": {
        "title": "System Settings",
        "desc": "Personalize your Signal OS experience.",
        "language": "Language",
        "theme": "Interface Theme",
        "role": "Active Identity Role",
        "role_desc": "Switch between your authorized security clearances.",
        "dark": "Dark Mode",
        "light": "Light Mode"
      }
    }
  },
  es: {
    translation: {
      "nav": {
        "insights": "Perspectivas",
        "report": "Reporte Público",
        "my_contributions": "Mis Contribuciones",
        "moderation": "Moderación",
        "settings": "Configuración",
        "sign_out": "Cerrar Sesión"
      },
      "dashboard": {
        "welcome": "Bienvenido de nuevo",
        "live_intel": "Inteligencia en Vivo",
        "title": "Perspectivas de Gobierno",
        "subtitle": "Seguimiento en tiempo real de la priorización y resolución de señales comunitarias.",
        "broadcast": "Transmisión Semanal",
        "new_issue": "Reportar Incidencia"
      },
      "metrics": {
        "total": "Total de Señales",
        "new": "Nuevos Reportes",
        "analysis": "Bajo Análisis",
        "avg": "Promedio Prioridad"
      },
      "settings": {
        "title": "Configuración del Sistema",
        "desc": "Personaliza tu experiencia en Signal OS.",
        "language": "Idioma",
        "theme": "Tema de Interfaz",
        "role": "Rol de Identidad Activo",
        "role_desc": "Cambia entre tus niveles de seguridad autorizados.",
        "dark": "Modo Oscuro",
        "light": "Modo Claro"
      }
    }
  }
};

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources,
    fallbackLng: 'en',
    interpolation: {
      escapeValue: false
    }
  });

export default i18n;
