import { useMemo } from "react";
import { Bar } from "react-chartjs-2";
import {
  Chart as ChartJS,
  BarElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend,
  type TooltipItem,
} from "chart.js";
import { useTranslation } from "react-i18next";
import { Signal } from "../types";
import { CivicCard } from "./ui/CivicCard";
import { useSettingsStore } from "../store/useSettingsStore";

ChartJS.register(BarElement, CategoryScale, LinearScale, Tooltip, Legend);

type Props = {
  signals: Signal[];
};

const CHART_TOKENS = ["--chart-1", "--chart-2", "--chart-3", "--chart-4", "--chart-5", "--chart-6"];

/**
 * Six mark shapes, one per sector slot.
 *
 * Colour alone is not an encoding: two sectors can share a hue for a colour-blind reader and every
 * hue collapses in a greyscale print. Each bar therefore carries a pattern as well, drawn in the
 * same token colour so the two encodings always agree.
 */
function patternFor(color: string, index: number): string | CanvasPattern {
  const canvas = document.createElement("canvas");
  canvas.width = 10;
  canvas.height = 10;
  const ctx = canvas.getContext("2d");
  if (!ctx) return color;

  ctx.fillStyle = color;
  ctx.fillRect(0, 0, 10, 10);
  ctx.strokeStyle = "rgba(0, 0, 0, 0.4)";
  ctx.lineWidth = 2;

  switch (index % 6) {
    case 0:
      ctx.beginPath();
      ctx.moveTo(-2, 12);
      ctx.lineTo(12, -2);
      ctx.stroke();
      break;
    case 1:
      ctx.beginPath();
      ctx.moveTo(0, 3);
      ctx.lineTo(10, 3);
      ctx.stroke();
      break;
    case 2:
      ctx.fillStyle = "rgba(0, 0, 0, 0.4)";
      ctx.beginPath();
      ctx.arc(5, 5, 1.6, 0, Math.PI * 2);
      ctx.fill();
      break;
    case 3:
      ctx.beginPath();
      ctx.moveTo(-2, -2);
      ctx.lineTo(12, 12);
      ctx.stroke();
      break;
    case 4:
      ctx.beginPath();
      ctx.moveTo(3, 0);
      ctx.lineTo(3, 10);
      ctx.stroke();
      break;
    default:
      ctx.beginPath();
      ctx.moveTo(0, 3);
      ctx.lineTo(10, 3);
      ctx.moveTo(3, 0);
      ctx.lineTo(3, 10);
      ctx.stroke();
      break;
  }

  return ctx.createPattern(canvas, "repeat") ?? color;
}

export function CategoryChart({ signals }: Props) {
  const { t } = useTranslation();
  const theme = useSettingsStore((state) => state.theme);

  const palette = useMemo(() => {
    const styles = getComputedStyle(document.documentElement);
    const token = (name: string, fallback: string) => styles.getPropertyValue(name).trim() || fallback;
    return {
      colors: CHART_TOKENS.map((name, index) => token(name, ["#b53328", "#2a7f7a", "#a8721a", "#7c5a72", "#2f6e8c", "#7a6d5e"][index])),
      textMuted: token("--text-muted", "#64748b"),
      textMain: token("--text-main", "#2a1d16"),
      grid: token("--border-subtle", "rgba(148, 163, 184, 0.35)"),
      tooltipBg: token("--tooltip-bg", "#4a3a32"),
      tooltipText: token("--tooltip-text", "#fbf1e2"),
    };
  }, [theme]);

  const chartData = useMemo(() => {
    const categories = signals.reduce((acc: Record<string, number>, s) => {
      acc[s.category] = (acc[s.category] || 0) + 1;
      return acc;
    }, {});
    // Most reported first: the tallest bar should be the one the eye lands on.
    const ordered = Object.entries(categories).sort((a, b) => b[1] - a[1]);

    return {
      labels: ordered.map(([category]) => t(`categories.${category}`)),
      datasets: [
        {
          label: t("dashboard.distribution_title"),
          data: ordered.map(([, count]) => count),
          backgroundColor: ordered.map((_, index) => patternFor(palette.colors[index % palette.colors.length], index)),
          borderColor: ordered.map((_, index) => palette.colors[index % palette.colors.length]),
          borderWidth: 1.5,
          borderRadius: 8,
          borderSkipped: false as const,
          barThickness: 18,
        },
      ],
    };
  }, [palette.colors, signals, t]);

  const options = {
    indexAxis: "y" as const,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: palette.tooltipBg,
        titleColor: palette.tooltipText,
        bodyColor: palette.tooltipText,
        titleFont: { size: 13, weight: 800 as const },
        bodyFont: { size: 12 },
        padding: 12,
        cornerRadius: 8,
        displayColors: false,
        callbacks: {
          label: (context: TooltipItem<"bar">) =>
            t("public_backlog.open_count", { count: context.parsed.x ?? 0 }),
        },
      },
    },
    scales: {
      x: {
        beginAtZero: true,
        ticks: {
          color: palette.textMuted,
          precision: 0,
          font: { size: 10, weight: 700 as const, family: "Nunito Sans" },
        },
        grid: { color: palette.grid },
      },
      y: {
        ticks: {
          color: palette.textMain,
          font: { size: 11, weight: 800 as const, family: "Nunito Sans" },
        },
        grid: { display: false },
      },
    },
    maintainAspectRatio: false,
  };

  return (
    <CivicCard title={t("dashboard.distribution_title")} data-testid="dashboard-distribution-card">
      <p className="signal-copy text-xs mt-0 mb-3">{t("dashboard.distribution_desc")}</p>
      <div
        style={{ height: `${Math.max(160, chartData.labels.length * 34)}px` }}
        className="flex justify-content-center py-2"
        role="img"
        aria-label={t("dashboard.distribution_aria")}
        data-testid="dashboard-distribution-chart"
      >
        <Bar data={chartData} options={options} />
      </div>
    </CivicCard>
  );
}
