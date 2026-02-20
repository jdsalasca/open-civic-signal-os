import { useMemo } from "react";
import { Pie } from "react-chartjs-2";
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from "chart.js";
import { Card } from "primereact/card";
import { useTranslation } from "react-i18next";
import { Signal } from "../types";

ChartJS.register(ArcElement, Tooltip, Legend);

type Props = {
  signals: Signal[];
};

export function CategoryChart({ signals }: Props) {
  const { t } = useTranslation();
  const textMuted =
    typeof window !== "undefined"
      ? getComputedStyle(document.documentElement).getPropertyValue("--text-muted").trim() || "#94a3b8"
      : "#94a3b8";
  const chartData = useMemo(() => {
    const categories = signals.reduce((acc: Record<string, number>, s) => {
      acc[s.category] = (acc[s.category] || 0) + 1;
      return acc;
    }, {});

    return {
      labels: Object.keys(categories),
      datasets: [
        {
          data: Object.values(categories),
          backgroundColor: [
            "#06b6d4",
            "#8b5cf6",
            "#ec4899",
            "#f59e0b",
            "#10b981",
            "#3b82f6",
          ],
          borderWidth: 0,
        },
      ],
    };
  }, [signals]);

  const options = {
    plugins: {
      legend: {
        position: "bottom" as const,
        labels: {
          color: textMuted,
          usePointStyle: true,
          padding: 20,
          font: { size: 11, weight: "bold" as const },
        },
      },
    },
    maintainAspectRatio: false,
  };

  return (
    <Card title={t('dashboard.distribution_title')} className="shadow-4 border-1 border-white-alpha-10 bg-surface overflow-hidden">
      <div style={{ height: "250px" }} className="flex justify-content-center">
        <Pie data={chartData} options={options} />
      </div>
    </Card>
  );
}
