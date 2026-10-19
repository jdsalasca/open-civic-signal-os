import { useTranslation } from "react-i18next";
import { CivicButton } from "./ui/CivicButton";
import { PRIORITY_LEVEL_ORDER, priorityLevelFor } from "../utils/priorityScale";

/**
 * Shared visual primitives for the core signal experience.
 *
 * The priority scale, the lifecycle chip, the sparkline and the empty state appear on the
 * dashboard, the list, the case, the public backlog and the digest. They live here, once, so the
 * same score cannot be drawn two different ways on two different screens.
 */

/**
 * Priority as a clear visual scale with an accessible text label.
 *
 * The number is never the only signal: five marks carry both a distinct colour and a distinct
 * pattern, and the visible label spells the level out. `aria-label` carries the same words for a
 * screen reader, so the scale is not a colour test.
 */
export function PriorityScale({
  score,
  threshold = 0,
  testId,
  compact = false,
  showLabel = true,
}: {
  score: number;
  threshold?: number;
  testId?: string;
  compact?: boolean;
  showLabel?: boolean;
}) {
  const { t } = useTranslation();
  const level = priorityLevelFor(score, threshold);
  const filled = PRIORITY_LEVEL_ORDER.indexOf(level) + 1;
  const levelLabel = t(`signal_experience.priority_level_${level}`);
  const accessible = t("signal_experience.priority_label", { level: levelLabel, score: Math.round(score) });

  return (
    <span
      className={`signal-priority ${compact ? "signal-priority--compact" : ""}`}
      data-level={level}
      data-testid={testId}
      role="img"
      aria-label={accessible}
      title={accessible}
    >
      <span className="signal-priority__track" aria-hidden="true">
        {[1, 2, 3, 4, 5].map((mark) => (
          <span key={mark} className={`signal-priority__segment ${mark <= filled ? "is-on" : ""}`} />
        ))}
      </span>
      {showLabel ? <span className="signal-priority__label">{levelLabel}</span> : null}
    </span>
  );
}

/**
 * A lifecycle chip: plain-language label, coloured dot, and a dot shape per state so the status
 * survives greyscale and colour-blind readers.
 */
export function StatusChip({
  status,
  label,
  testId,
  className,
}: {
  status?: string | null;
  label: string;
  testId?: string;
  className?: string;
}) {
  return (
    <span className={`signal-status-chip ${className ?? ""}`} data-status={status ?? "UNKNOWN"} data-testid={testId}>
      <span className="signal-status-chip__dot" aria-hidden="true" />
      <span>{label}</span>
    </span>
  );
}

/**
 * A tiny direction line for a metric card. Decorative only: the number beside it carries the
 * meaning, so the SVG is hidden from assistive tech.
 */
export function Sparkline({
  values,
  testId,
  className,
}: {
  values: number[];
  testId?: string;
  className?: string;
}) {
  const width = 64;
  const height = 22;
  const series = values.filter((value) => Number.isFinite(value));
  if (series.length === 0) return null;

  const max = Math.max(...series);
  const min = Math.min(...series);
  const span = max - min || 1;
  const points = series.map((value, index) => {
    const x = series.length === 1 ? width / 2 : (index / (series.length - 1)) * width;
    const y = height - ((value - min) / span) * height;
    return `${x.toFixed(1)},${y.toFixed(1)}`;
  });
  const color = "var(--chart-1)";

  return (
    <svg
      className={`signal-sparkline ${className ?? ""}`}
      viewBox={`0 0 ${width} ${height}`}
      preserveAspectRatio="none"
      aria-hidden="true"
      focusable="false"
      data-testid={testId}
    >
      <polygon className="signal-sparkline__area" fill={color} points={`0,${height} ${points.join(" ")} ${width},${height}`} />
      <polyline className="signal-sparkline__line" stroke={color} points={points.join(" ")} />
    </svg>
  );
}

/** A friendly, action-oriented empty state shared by the list and the public backlog. */
export function SignalEmpty({
  icon,
  title,
  description,
  actionLabel,
  onAction,
  testId,
}: {
  icon: string;
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
  testId?: string;
}) {
  return (
    <div className="signal-empty" data-testid={testId}>
      <div className="signal-empty__icon">
        <i className={`pi ${icon} text-2xl`} aria-hidden="true" />
      </div>
      <p className="signal-empty__title">{title}</p>
      <p className="signal-empty__desc">{description}</p>
      {actionLabel && onAction ? (
        <CivicButton type="button" label={actionLabel} icon="pi pi-plus" onClick={onAction} />
      ) : null}
    </div>
  );
}
