import { useMemo } from "react";
import { Signal } from "../types";
import { useTranslation } from "react-i18next";
import { Sparkline } from "./signalPrimitives";

type MetricId = "total" | "new" | "analysis" | "avg";

type Props = {
  signals: Signal[];
  onMetricSelect?: (metricId: MetricId) => void;
};

const TREND_DAYS = 7;

/**
 * The trend line on a metric card is built from the dates the signals themselves carry.
 *
 * It is deliberately coarse: a count per day over the last week, drawn from whatever page of
 * signals is on screen. It exists to give the big number a direction, not to be an analytics
 * claim, which is why the caption under it names the window instead of asserting a percentage.
 */
function dailyTrend(signals: Signal[]): number[] {
  const days = new Array(TREND_DAYS).fill(0);
  const start = new Date();
  start.setHours(0, 0, 0, 0);
  start.setDate(start.getDate() - (TREND_DAYS - 1));

  signals.forEach((signal) => {
    if (!signal.createdAt) return;
    const created = new Date(signal.createdAt);
    if (Number.isNaN(created.getTime())) return;
    created.setHours(0, 0, 0, 0);
    const offset = Math.round((created.getTime() - start.getTime()) / 86_400_000);
    if (offset >= 0 && offset < TREND_DAYS) {
      days[offset] += 1;
    }
  });

  return days;
}

export function MetricsGrid({ signals, onMetricSelect }: Props) {
  const { t } = useTranslation();

  const metrics = useMemo(() => {
    const total = signals.length;
    const inProgress = signals.filter((s) => s.status === "IN_PROGRESS");
    const newlyReported = signals.filter((s) => s.status === "NEW");
    const avgScore =
      total === 0
        ? "0"
        : (signals.reduce((acc, s) => acc + s.priorityScore, 0) / total).toFixed(1);

    return [
      {
        id: "total" as MetricId,
        title: t("metrics.total"),
        value: String(total),
        support: t("metrics.total_support"),
        icon: "pi-list",
        trend: dailyTrend(signals),
      },
      {
        id: "new" as MetricId,
        title: t("metrics.new"),
        value: String(newlyReported.length),
        support: t("metrics.new_support"),
        icon: "pi-plus-circle",
        trend: dailyTrend(newlyReported),
      },
      {
        id: "analysis" as MetricId,
        title: t("metrics.analysis"),
        value: String(inProgress.length),
        support: t("metrics.analysis_support"),
        icon: "pi-cog",
        trend: dailyTrend(inProgress),
      },
      {
        id: "avg" as MetricId,
        title: t("metrics.avg"),
        value: avgScore,
        support: t("metrics.avg_support"),
        icon: "pi-chart-bar",
        trend: signals.map((s) => s.priorityScore ?? 0),
      },
    ];
  }, [signals, t]);

  return (
    <div className="signal-metric-grid" role="region" aria-label={t("metrics.region_label")}>
      {metrics.map((m) => (
        <article
          key={m.id}
          className={`signal-metric-card${m.id === "new" ? " signal-metric-card--accent" : ""}${onMetricSelect ? " is-interactive" : ""}`}
          data-testid={`metric-card-${m.id}`}
          role={onMetricSelect ? "button" : undefined}
          tabIndex={onMetricSelect ? 0 : undefined}
          onClick={onMetricSelect ? () => onMetricSelect(m.id) : undefined}
          onKeyDown={
            onMetricSelect
              ? (event) => {
                  if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    onMetricSelect(m.id);
                  }
                }
              : undefined
          }
        >
          <div className="signal-metric-card__top">
            <span className="signal-metric-card__label">{m.title}</span>
            <i className={`pi ${m.icon} signal-metric-card__icon`} aria-hidden="true" />
          </div>
          <div className="signal-metric-card__value" data-testid={`metric-value-${m.id}`}>
            {m.value}
          </div>
          <div className="signal-metric-card__foot">
            <span className="signal-metric-card__support">{m.support}</span>
            <span className="signal-metric-card__spark">
              <Sparkline values={m.trend} testId={`metric-spark-${m.id}`} />
              <span className="signal-metric-card__trend">{t("metrics.trend_label")}</span>
            </span>
          </div>
        </article>
      ))}
    </div>
  );
}
