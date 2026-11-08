import { Radar } from "react-chartjs-2";
import {
  Chart as ChartJS,
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
  Legend,
  type TooltipItem,
} from "chart.js";
import { CivicCard } from "./ui/CivicCard";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useSettingsStore } from "../store/useSettingsStore";

ChartJS.register(
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
  Legend
);

interface PriorityRadarProps {
  urgency: number;
  impact: number;
  votes: number;
  people: number;
}

/**
 * The four scoring factors, each as a share of its cap.
 *
 * The caps come from the published formula (urgency x30, impact x25, votes x15, people x30), so the
 * shape says which term is carrying the score. It is a read of the backend breakdown, never a
 * second calculation of the rank.
 */
export function PriorityRadar({ urgency, impact, votes, people }: PriorityRadarProps) {
  const { t } = useTranslation();
  const theme = useSettingsStore((state) => state.theme);

  const palette = useMemo(() => {
    const styles = getComputedStyle(document.documentElement);
    const token = (name: string, fallback: string) => styles.getPropertyValue(name).trim() || fallback;
    return {
      primary: token("--brand-primary", "#1d4ed8"),
      primaryGlow: token("--brand-primary-glow", "rgba(29, 78, 216, 0.35)"),
      textMuted: token("--text-muted", "#64748b"),
      grid: token("--border-subtle", "rgba(148, 163, 184, 0.35)"),
      tooltipBg: token("--tooltip-bg", "#0f172a"),
      tooltipText: token("--tooltip-text", "#ffffff"),
      onBrand: token("--on-brand", "#f8fafc"),
    };
  }, [theme]);

  const factorRows = [
    { key: "urgency", label: t("signals.formula_factors.urgency"), cap: 150, value: urgency },
    { key: "impact", label: t("signals.formula_factors.impact"), cap: 125, value: impact },
    { key: "communityVotes", label: t("signals.formula_factors.communityVotes"), cap: 15, value: votes },
    { key: "affectedPeople", label: t("signals.formula_factors.affectedPeople"), cap: 30, value: people },
  ];

  const data = {
    labels: factorRows.map((row) => row.label),
    datasets: [
      {
        label: t("signal_experience.radar_title"),
        data: factorRows.map((row) => Math.min(100, (row.value / row.cap) * 100)),
        backgroundColor: palette.primaryGlow,
        borderColor: palette.primary,
        borderWidth: 2.5,
        pointBackgroundColor: palette.primary,
        pointBorderColor: palette.onBrand,
        pointRadius: 4,
        pointBorderWidth: 2,
        pointHoverBackgroundColor: palette.onBrand,
        pointHoverBorderColor: palette.primary,
      },
    ],
  };

  const options = {
    scales: {
      r: {
        angleLines: { color: palette.grid },
        grid: { color: palette.grid },
        pointLabels: {
          color: palette.textMuted,
          font: { size: 10, weight: 700, family: "Nunito Sans" },
        },
        ticks: { display: false, stepSize: 20 },
        suggestedMin: 0,
        suggestedMax: 100,
      },
    } as any,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: palette.tooltipBg,
        titleColor: palette.tooltipText,
        bodyColor: palette.tooltipText,
        cornerRadius: 8,
        padding: 12,
        displayColors: false,
        callbacks: {
          label: (context: TooltipItem<"radar">) =>
            t("signal_experience.radar_cap_share", { percent: Math.round(Number(context.raw) || 0) }),
        },
      },
    },
    maintainAspectRatio: false,
  };

  return (
    <CivicCard title={t("signal_experience.radar_title")} variant="brand">
      <div
        style={{ height: "280px" }}
        className="flex justify-content-center py-2"
        role="img"
        aria-label={t("signal_experience.radar_aria")}
        data-testid="signal-detail-factor-radar"
      >
        <Radar data={data} options={options} />
      </div>
    </CivicCard>
  );
}
