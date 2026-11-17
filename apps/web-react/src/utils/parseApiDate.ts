/**
 * Timestamps arrive as Java `LocalDateTime`: a wall-clock string with no offset. The API runs in a
 * UTC container, so the value is UTC; parsing it as browser-local made a post from five hours ago
 * read "in five hours" for readers in UTC-5.
 *
 * Date-only values stay local so a calendar day does not shift to the previous day when rendered.
 */
export function parseApiDate(value?: string | null): Date | null {
  if (!value) return null;
  const trimmed = value.trim();
  if (!trimmed) return null;

  const dateOnly = /^\d{4}-\d{2}-\d{2}$/.test(trimmed);
  const hasZone = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(trimmed);
  const date = new Date(dateOnly ? `${trimmed}T00:00:00` : hasZone ? trimmed : `${trimmed}Z`);
  return Number.isNaN(date.getTime()) ? null : date;
}
