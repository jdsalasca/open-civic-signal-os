import { Radar } from "react-chartjs-2";
import {
  Chart as ChartJS,
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
  Legend,
} from "chart.js";
import { CivicCard } from "./ui/CivicCard";
import { useMemo } from "react";
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

export function PriorityRadar({ urgency, impact, votes, people }: PriorityRadarProps) {
  const theme = useSettingsStore((state) => state.theme);
  const palette = useMemo(() => {
    const styles = getComputedStyle(document.documentElement);
    const token = (name: string, fallback: string) => styles.getPropertyValue(name).trim() || fallback;
    const primary = token("--brand-primary", "#1d4ed8");
    return {
      primary,
      primaryGlow: token("--brand-primary-glow", "rgba(29, 78, 216, 0.35)"),
      textMuted: token("--text-muted", "#64748b"),
      grid: token("--border-subtle", "rgba(148, 163, 184, 0.35)"),
      tooltipBg: token("--tooltip-bg", "#0f172a"),
      tooltipText: token("--tooltip-text", "#ffffff"),
      onBrand: token("--on-brand", "#f8fafc")
    };
  }, [theme]);

  const data = {
    labels: ["Urgency", "Impact", "Community Support", "Population Scale"],
    datasets: [
      {
        label: "Priority Factors",
        data: [
          (urgency / 150) * 100,
          (impact / 125) * 100,
          (votes / 15) * 100,
          (people / 30) * 100,
        ],
        backgroundColor: palette.primaryGlow,
        borderColor: palette.primary,
        borderWidth: 2,
        pointBackgroundColor: palette.primary,
        pointBorderColor: palette.onBrand,
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
          font: { size: 10, weight: 700, family: "Plus Jakarta Sans" },
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
      },
    },
    maintainAspectRatio: false,
  };

  return (
    <CivicCard title="Intelligence Profile" variant="brand">
      <div style={{ height: "280px" }} className="flex justify-content-center py-2">
        <Radar data={data} options={options} />
      </div>
    </CivicCard>
  );
}
