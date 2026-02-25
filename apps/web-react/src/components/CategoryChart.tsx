import { useMemo } from "react";
import { Doughnut } from "react-chartjs-2";
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from "chart.js";
import { useTranslation } from "react-i18next";
import { Signal } from "../types";
import { CivicCard } from "./ui/CivicCard";
import { useSettingsStore } from "../store/useSettingsStore";

ChartJS.register(ArcElement, Tooltip, Legend);

type Props = {
  signals: Signal[];
};

export function CategoryChart({ signals }: Props) {
  const { t } = useTranslation();
  const theme = useSettingsStore((state) => state.theme);

  const palette = useMemo(() => {
    const styles = getComputedStyle(document.documentElement);
    const token = (name: string, fallback: string) => styles.getPropertyValue(name).trim() || fallback;
    return {
      colors: [
        token("--chart-1", "#1d4ed8"),
        token("--chart-2", "#0d9488"),
        token("--chart-3", "#f59e0b"),
        token("--chart-4", "#e11d48"),
        token("--chart-5", "#0284c7"),
        token("--chart-6", "#2563eb")
      ],
      textMuted: token("--text-muted", "#64748b"),
      tooltipBg: token("--tooltip-bg", "#0f172a"),
      tooltipText: token("--tooltip-text", "#ffffff")
    };
  }, [theme]);

  const chartData = useMemo(() => {
    const categories = signals.reduce((acc: Record<string, number>, s) => {
      acc[s.category] = (acc[s.category] || 0) + 1;
      return acc;
    }, {});

    return {
      labels: Object.keys(categories).map(c => t(`categories.${c}`)),
      datasets: [
        {
          data: Object.values(categories),
          backgroundColor: palette.colors,
          hoverOffset: 15,
          borderWidth: 0,
          borderRadius: 4,
          cutout: '75%'
        },
      ],
    };
  }, [palette.colors, signals, t]);

  const options = {
    plugins: {
      legend: {
        position: "bottom" as const,
        labels: {
          color: palette.textMuted,
          usePointStyle: true,
          pointStyle: 'circle',
          padding: 25,
          font: { size: 10, weight: 700, family: 'Plus Jakarta Sans' },
        },
      },
      tooltip: {
        backgroundColor: palette.tooltipBg,
        titleColor: palette.tooltipText,
        bodyColor: palette.tooltipText,
        titleFont: { size: 13, weight: 800 },
        bodyFont: { size: 12 },
        padding: 12,
        cornerRadius: 8,
        displayColors: true
      }
    },
    maintainAspectRatio: false,
  };

  return (
    <CivicCard title="Sector Distribution">
      <div style={{ height: "280px" }} className="flex justify-content-center py-2">
        <Doughnut data={chartData} options={options} />
      </div>
    </CivicCard>
  );
}
