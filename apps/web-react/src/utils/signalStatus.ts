import { useTranslation } from "react-i18next";

/**
 * Lifecycle values are identifiers for the API, not language a resident can act on.
 *
 * AGENTS.md forbids showing raw enums when a plain label exists, and this mapping is shared by every
 * screen that renders a lifecycle state so the wording cannot drift between them.
 *
 * An unmapped value resolves to "Unknown" rather than falling through to the raw string: a new
 * status added on the API must not leak an identifier into the UI on the day it ships.
 */
const STATUS_LABEL_KEYS: Record<string, string> = {
  NEW: "signals.status_new",
  IN_PROGRESS: "signals.status_in_progress",
  RESOLVED: "signals.status_resolved",
  REJECTED: "signals.status_rejected",
};

export function useSignalStatusLabel(): (status?: string | null) => string {
  const { t } = useTranslation();
  return (status) =>
    t(status ? (STATUS_LABEL_KEYS[status] ?? "signals.status_unknown") : "signals.status_unknown");
}