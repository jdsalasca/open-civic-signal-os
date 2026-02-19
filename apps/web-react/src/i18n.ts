import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

const resources = {
  en: {
    translation: {
      "common": {
        "loading": "Loading...",
        "error": "Error",
        "success": "Success",
        "save": "Save",
        "cancel": "Cancel",
        "discard": "Discard",
        "back": "Back",
        "actions": "Actions",
        "status": "Status",
        "category": "Category",
        "score": "Score",
        "description": "Description",
        "title": "Title"
      },
      "nav": {
        "insights": "Insights",
        "report": "Public Report",
        "my_contributions": "My Contributions",
        "moderation": "Moderation",
        "settings": "Settings",
        "main_navigation": "Main Navigation",
        "open_navigation": "Open navigation menu",
        "sign_out": "Sign Out",
        "sign_in": "Sign In",
        "join_now": "Join Now",
        "protocol_version": "Protocol v0.3.0-hardened",
        "global_standard": "Global Governance Standard"
      },
      "auth": {
        "login_title": "Access Portal",
        "login_subtitle": "Open Civic Signal OS Management",
        "username": "Username",
        "username_placeholder": "Enter your username",
        "password": "Password",
        "password_placeholder": "Enter your password",
        "sign_in_button": "Sign In to OS",
        "no_account": "Don't have an account?",
        "create_one": "Create one",
        "join_title": "Join Signal OS",
        "join_subtitle": "Create your citizen account",
        "email": "Email Address",
        "email_placeholder": "Enter your email",
        "clearance_level": "Default Security Clearance: Citizen",
        "create_account": "Create Account",
        "already_member": "Already a member?",
        "sign_in_here": "Sign in here",
        "welcome_back": "Welcome back",
        "login_success": "Welcome back, {{name}}!",
        "register_success": "Account created! You can now log in.",
        "logout_warn": "Server-side logout session could not be invalidated."
      },
      "dashboard": {
        "welcome": "Welcome back",
        "live_intel": "Live Intelligence",
        "title": "Governance Insights",
        "subtitle": "Real-time community signal prioritization and resolution lifecycle tracking.",
        "broadcast": "Weekly Broadcast",
        "broadcast_success": "Intelligence broadcast successful!",
        "new_issue": "Report New Issue",
        "feed_title": "Prioritized Feed",
        "distribution_title": "Signal Distribution",
        "system_alerts_title": "System Alerts",
        "search_placeholder": "Search signals...",
        "empty_title": "No civic signals found",
        "empty_desc": "Your community feed is currently clear. Be the first to report a local need or problem.",
        "citizen_support_title": "Civic Support",
        "citizen_support_desc": "Your votes and reports directly impact the algorithmic priority of issues in your community. Keep up the good work!",
        "citizen_verification": "Citizen Verification: Active"
      },
      "metrics": {
        "total": "Total Signals",
        "new": "Newly Reported",
        "analysis": "Under Analysis",
        "avg": "Priority Avg"
      },
      "report": {
        "title": "Report Community Signal",
        "desc": "Your input will be processed by our priority algorithm to optimize civic response.",
        "issue_title": "Issue Title",
        "issue_title_placeholder": "e.g. Broken water pipe in Main St.",
        "scale": "Scale (Estimated Citizens)",
        "context": "Intelligence Context (Description)",
        "context_placeholder": "Describe the problem, location and observed impact...",
        "urgency": "Urgency Factor",
        "urgency_low": "LOW",
        "urgency_critical": "CRITICAL",
        "impact": "Social Impact",
        "impact_minor": "MINOR",
        "impact_systemic": "SYSTEMIC",
        "submit": "Ingest Signal",
        "success": "Civic signal ingested. Prioritizing..."
      },
      "signals": {
        "ref": "REF",
        "view_details": "View Details",
        "support_button": "Support this Issue",
        "support_loading": "Registering...",
        "support_success": "Community support registered!",
        "problem_definition": "Problem Definition",
        "affected_estimation": "Affected Estimation",
        "citizens": "Citizens",
        "civic_category": "Civic Category",
        "lifecycle_admin": "Lifecycle Administration",
        "reset_new": "Reset to NEW",
        "mark_inprogress": "Mark IN PROGRESS",
        "mark_resolved": "Mark RESOLVED",
        "lifecycle_success": "Lifecycle updated to {{status}}",
        "intel_index": "Intelligence Index",
        "priority_rank": "Priority Rank",
        "urgency_factor": "Urgency Factor",
        "social_impact": "Social Impact",
        "community_trust": "Community Trust"
      },
      "my_contributions": {
        "title": "My Contributions",
        "desc": "Track the impact of the issues you've reported.",
        "total_reports": "Total Reports",
        "resolved": "Resolved",
        "community_votes": "Community Votes",
        "empty": "You haven't reported any issues yet."
      },
      "moderation": {
        "title": "Moderation Queue",
        "desc": "Review automatically flagged signals for potential abuse.",
        "suspect_title": "Suspect Title",
        "approve": "Approve",
        "reject": "Reject",
        "empty": "No signals require attention.",
        "success": "Signal {{action}} successfully."
      },
      "settings": {
        "title": "System Settings",
        "desc": "Personalize your Signal OS experience.",
        "language": "Language",
        "theme": "Interface Theme",
        "role": "Active Identity Role",
        "role_desc": "Switch between your authorized security clearances.",
        "dark": "Dark Mode",
        "light": "Light Mode",
        "admin_tools": "Administrative Tools",
        "admin_desc": "Download complete intelligence datasets for external auditing.",
        "export_button": "Export Signal Registry (CSV)",
        "export_success": "Intelligence data exported successfully.",
        "export_error": "Export failed. Insufficient clearance or network error."
      },
      "exceptions": {
        "404_title": "404",
        "404_subtitle": "Coordinate Not Found",
        "404_desc": "The civic resource you are looking for is outside the mapped territory of Signal OS.",
        "404_home": "Return to Command Center",
        "404_verify": "Verify Connection",
        "403_title": "403",
        "403_subtitle": "Clearance Required",
        "403_desc": "Your identity does not have the necessary security clearance to access this civic sector.",
        "403_home": "Back to Dashboard",
        "403_switch": "Switch Intelligence Identity",
        "chunk_title": "Protocol Resynchronization",
        "chunk_desc": "A system module failed to load due to a stale cache or temporary signal loss.",
        "chunk_button": "Refresh Intelligence"
      },
      "categories": {
        "safety": "Public Safety",
        "infrastructure": "Infrastructure",
        "environment": "Environment",
        "social": "Social Services",
        "mobility": "Mobility",
        "education": "Education"
      }
    }
  },
  es: {
    translation: {
      "common": {
        "loading": "Cargando...",
        "error": "Error",
        "success": "Éxito",
        "save": "Guardar",
        "cancel": "Cancelar",
        "discard": "Descartar",
        "back": "Volver",
        "actions": "Acciones",
        "status": "Estado",
        "category": "Categoría",
        "score": "Puntaje",
        "description": "Descripción",
        "title": "Título"
      },
      "nav": {
        "insights": "Perspectivas",
        "report": "Reporte Público",
        "my_contributions": "Mis Contribuciones",
        "moderation": "Moderación",
        "settings": "Configuración",
        "main_navigation": "Navegación Principal",
        "open_navigation": "Abrir menú de navegación",
        "sign_out": "Cerrar Sesión",
        "sign_in": "Iniciar Sesión",
        "join_now": "Unirse Ahora",
        "protocol_version": "Protocolo v0.3.0-hardened",
        "global_standard": "Estándar Global de Gobernanza"
      },
      "auth": {
        "login_title": "Portal de Acceso",
        "login_subtitle": "Gestión de Open Civic Signal OS",
        "username": "Usuario",
        "username_placeholder": "Ingrese su usuario",
        "password": "Contraseña",
        "password_placeholder": "Ingrese su contraseña",
        "sign_in_button": "Entrar al OS",
        "no_account": "¿No tiene una cuenta?",
        "create_one": "Cree una",
        "join_title": "Unirse a Signal OS",
        "join_subtitle": "Cree su cuenta de ciudadano",
        "email": "Correo Electrónico",
        "email_placeholder": "Ingrese su correo",
        "clearance_level": "Nivel de Seguridad: Ciudadano",
        "create_account": "Crear Cuenta",
        "already_member": "¿Ya es miembro?",
        "sign_in_here": "Inicie sesión aquí",
        "welcome_back": "Bienvenido de nuevo",
        "login_success": "¡Bienvenido de nuevo, {{name}}!",
        "register_success": "¡Cuenta creada! Ya puede iniciar sesión.",
        "logout_warn": "La sesión del servidor no pudo ser invalidada."
      },
      "dashboard": {
        "welcome": "Bienvenido de nuevo",
        "live_intel": "Inteligencia en Vivo",
        "title": "Perspectivas de Gobierno",
        "subtitle": "Seguimiento en tiempo real de la priorización y resolución de señales comunitarias.",
        "broadcast": "Transmisión Semanal",
        "broadcast_success": "¡Transmisión de inteligencia exitosa!",
        "new_issue": "Reportar Incidencia",
        "feed_title": "Feed Priorizado",
        "distribution_title": "Distribución de Señales",
        "system_alerts_title": "Alertas del Sistema",
        "search_placeholder": "Buscar señales...",
        "empty_title": "No se encontraron señales",
        "empty_desc": "El feed de su comunidad está despejado. Sea el primero en reportar una necesidad local.",
        "citizen_support_title": "Apoyo Ciudadano",
        "citizen_support_desc": "Sus votos y reportes impactan directamente en la prioridad algorítmica de su comunidad.",
        "citizen_verification": "Verificación Ciudadana: Activa"
      },
      "metrics": {
        "total": "Total de Señales",
        "new": "Nuevos Reportes",
        "analysis": "Bajo Análisis",
        "avg": "Promedio Prioridad"
      },
      "report": {
        "title": "Reportar Señal Comunitaria",
        "desc": "Su entrada será procesada por nuestro algoritmo para optimizar la respuesta cívica.",
        "issue_title": "Título del Problema",
        "issue_title_placeholder": "ej. Tubería rota en Calle Principal",
        "scale": "Escala (Ciudadanos Estimados)",
        "context": "Contexto de Inteligencia (Descripción)",
        "context_placeholder": "Describa el problema, ubicación e impacto observado...",
        "urgency": "Factor de Urgencia",
        "urgency_low": "BAJA",
        "urgency_critical": "CRÍTICA",
        "impact": "Impacto Social",
        "impact_minor": "MENOR",
        "impact_systemic": "SISTÉMICO",
        "submit": "Ingresar Señal",
        "success": "Señal ingresada. Priorizando..."
      },
      "signals": {
        "ref": "REF",
        "view_details": "Ver Detalles",
        "support_button": "Apoyar este Problema",
        "support_loading": "Registrando...",
        "support_success": "¡Apoyo comunitario registrado!",
        "problem_definition": "Definición del Problema",
        "affected_estimation": "Estimación de Afectados",
        "citizens": "Ciudadanos",
        "civic_category": "Categoría Cívica",
        "lifecycle_admin": "Administración del Ciclo de Vida",
        "reset_new": "Reiniciar a NUEVO",
        "mark_inprogress": "Marcar EN PROGRESO",
        "mark_resolved": "Marcar RESUELTO",
        "lifecycle_success": "Ciclo de vida actualizado a {{status}}",
        "intel_index": "Índice de Inteligencia",
        "priority_rank": "Rango de Prioridad",
        "urgency_factor": "Factor de Urgencia",
        "social_impact": "Impacto Social",
        "community_trust": "Confianza Comunitaria"
      },
      "my_contributions": {
        "title": "Mis Contribuciones",
        "desc": "Siga el impacto de los problemas que ha reportado.",
        "total_reports": "Reportes Totales",
        "resolved": "Resueltos",
        "community_votes": "Votos Comunitarios",
        "empty": "Aún no ha reportado ningún problema."
      },
      "moderation": {
        "title": "Cola de Moderación",
        "desc": "Revise señales marcadas automáticamente por posible abuso.",
        "suspect_title": "Título Sospechoso",
        "approve": "Aprobar",
        "reject": "Rechazar",
        "empty": "No hay señales que requieran atención.",
        "success": "Señal {{action}} con éxito."
      },
      "settings": {
        "title": "Configuración del Sistema",
        "desc": "Personalice su experiencia en Signal OS.",
        "language": "Idioma",
        "theme": "Tema de Interfaz",
        "role": "Rol de Identidad Activo",
        "role_desc": "Cambie entre sus niveles de seguridad autorizados.",
        "dark": "Modo Oscuro",
        "light": "Modo Claro",
        "admin_tools": "Herramientas Administrativas",
        "admin_desc": "Descargue conjuntos de datos completos para auditoría externa.",
        "export_button": "Exportar Registro (CSV)",
        "export_success": "Datos de inteligencia exportados con éxito.",
        "export_error": "Error al exportar. Permisos insuficientes o error de red."
      },
      "exceptions": {
        "404_title": "404",
        "404_subtitle": "Coordenada No Encontrada",
        "404_desc": "El recurso cívico que busca está fuera del territorio mapeado de Signal OS.",
        "404_home": "Volver al Centro de Comando",
        "404_verify": "Verificar Conexión",
        "403_title": "403",
        "403_subtitle": "Permisos Requeridos",
        "403_desc": "Su identidad no tiene el nivel de seguridad necesario para acceder a este sector cívico.",
        "403_home": "Volver al Panel",
        "403_switch": "Cambiar Identidad de Inteligencia",
        "chunk_title": "Resincronización de Protocolo",
        "chunk_desc": "Un módulo del sistema falló al cargar debido a caché obsoleta o pérdida temporal de señal.",
        "chunk_button": "Refrescar Inteligencia"
      },
      "categories": {
        "safety": "Seguridad Pública",
        "infrastructure": "Infraestructura",
        "environment": "Medio Ambiente",
        "social": "Servicios Sociales",
        "mobility": "Movilidad",
        "education": "Educación"
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
