/**
 * Renders a timestamp the same way for every reader.
 *
 * `toLocaleString()` with no argument renders from the browser's default locale, so the same audit
 * trail reads "4/1/2026, 10:00:00 AM" to one resident and "1.4.2026, 10:00:00" to the next, and the
 * ambiguous day/month order can be read as either 1 April or 4 January. It also prints seconds and a
 * half-day clock, which the source `LocalDateTime` does not carry.
 *
 * Fixed `YYYY-MM-DD HH:mm` is unambiguous in English and Spanish and sorts correctly. It is built
 * from local date components rather than a locale lookup, which preserves the existing semantics:
 * a `LocalDateTime` on the wire has no offset, so one is not invented here.
 *
 * Returns undefined for a missing or unparseable value so callers can fall back to their own copy.
 */
export function formatStamp(value?: string | null): string | undefined {
  if (!value) return undefined;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return undefined;
  const pad = (part: number) => String(part).padStart(2, "0");
  const day = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  return `${day} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}