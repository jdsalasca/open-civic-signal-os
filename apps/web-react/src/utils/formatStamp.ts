/**
 * Renders timestamps the same way for every reader.
 *
 * `toLocaleString()` with no argument renders from the browser's default locale, so the same audit
 * trail reads "4/1/2026, 10:00:00 AM" to one resident and "1.4.2026, 10:00:00" to the next, and the
 * ambiguous day/month order can be read as either 1 April or 4 January. `toLocaleTimeString` is
 * worse on its own: with no locale argument it drops the meridiem, so 6 PM renders as "18:00" in one
 * browser and "6:00" in another, which is not a time at all.
 *
 * Fixed ISO-like output is unambiguous in English and Spanish and sorts correctly. It is built from
 * local date components rather than a locale lookup, which preserves the existing semantics: a
 * `LocalDateTime` on the wire carries no offset, so none is invented here.
 *
 * All three return `undefined` for missing or unparseable input, so callers keep their own fallback
 * rather than rendering "Invalid Date" the way `new Date(x).toLocaleString()` does.
 */

function parts(value?: string | null): Date | undefined {
  if (!value) return undefined;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? undefined : date;
}

const pad = (part: number) => String(part).padStart(2, "0");

/** `YYYY-MM-DD`, for places that show a calendar day and no time. */
export function formatDate(value?: string | null): string | undefined {
  const date = parts(value);
  if (!date) return undefined;
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** `HH:mm` in 24-hour form, which is the convention in every locale this app ships. */
export function formatTime(value?: string | null): string | undefined {
  const date = parts(value);
  if (!date) return undefined;
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/** `YYYY-MM-DD HH:mm`, for places that show when something happened. */
export function formatStamp(value?: string | null): string | undefined {
  const date = parts(value);
  if (!date) return undefined;
  return `${formatDate(value)} ${formatTime(value)}`;
}