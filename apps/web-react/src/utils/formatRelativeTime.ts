/**
 * A relative time in the reader's language, e.g. "hace 7 minutos" or "7 minutes ago".
 *
 * <p>The feed used to render the backend's English phrase, so a Spanish interface showed
 * "updated 7m ago". This keeps one timestamp on the wire and lets the client localize it. The
 * fallback is an empty string; callers keep their own placeholder rather than printing "Invalid
 * Date".
 */
export function formatRelativeTime(value?: string | null, language = 'es'): string {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';

  const diff = date.getTime() - Date.now();
  const abs = Math.abs(diff);
  const formatter = new Intl.RelativeTimeFormat(language, { numeric: 'auto' });
  const units: Array<[Intl.RelativeTimeFormatUnit, number]> = [
    ['year', 31536000000],
    ['month', 2592000000],
    ['week', 604800000],
    ['day', 86400000],
    ['hour', 3600000],
    ['minute', 60000],
  ];

  for (const [unit, ms] of units) {
    if (abs >= ms) {
      return formatter.format(Math.round(diff / ms), unit);
    }
  }
  return formatter.format(Math.round(diff / 1000), 'second');
}
