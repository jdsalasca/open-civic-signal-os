/**
 * Priority scale mapping.
 *
 * Kept apart from the visual primitives so the mapping can be reasoned about (and later tested)
 * without a renderer, and so the component module stays components-only for fast refresh.
 */

// The formula caps each factor, so the largest score a signal can carry is
// 150 + 125 + 30 + 15. The scale is anchored on that maximum rather than on
// whatever happened to be on the current page.
export const MAX_PRIORITY_SCORE = 320;

export type PriorityLevel = "low" | "moderate" | "high" | "urgent" | "critical";

export const PRIORITY_LEVEL_ORDER: PriorityLevel[] = ["low", "moderate", "high", "urgent", "critical"];

/**
 * Map a score onto the five visual levels.
 *
 * When the backend publishes a critical threshold the scale is anchored to it, so the top level
 * agrees with the CRITICAL filter the dashboard already uses. Without a threshold the scale falls
 * back to the formula's own maximum, which keeps the visual honest even before /meta resolves.
 */
export function priorityLevelFor(score: number, threshold = 0): PriorityLevel {
  const safeScore = Number.isFinite(score) && score > 0 ? score : 0;
  const anchor = threshold > 0 ? threshold : MAX_PRIORITY_SCORE * 0.69;
  if (safeScore >= anchor) return "critical";
  if (safeScore >= anchor * 0.8) return "urgent";
  if (safeScore >= anchor * 0.55) return "high";
  if (safeScore >= anchor * 0.35) return "moderate";
  return "low";
}
