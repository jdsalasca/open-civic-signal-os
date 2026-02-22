import { useMemo } from "react";
import { Doughnut } from "react-chartjs-2";
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from "chart.js";
import { useTranslation } from "react-i18next";
import { Signal } from "../types";
import { CivicCard } from "./ui/CivicCard";

ChartJS.register(ArcElement, Tooltip, Legend);

type Props = {
  signals: Signal[];
};

export function CategoryChart({ signals }: Props) {
  const { t } = useTranslation();
  
  const colors = [
    "#6366f1", // Indigo 500
    "#10b981", // Emerald 500
    "#f59e0b", // Amber 500
    "#f43f5e", // Rose 500
    "#0ea5e9", // Sky 500
    "#8b5cf6", // Violet 500
  ];

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
          backgroundColor: colors,
          hoverOffset: 15,
          borderWidth: 0,
          borderRadius: 4,
          cutout: '75%'
        },
      ],
    };
  }, [signals, t]);

  const options = {
    plugins: {
      legend: {
        position: "bottom" as const,
        labels: {
          color: "#94a3b8",
          usePointStyle: true,
          pointStyle: 'circle',
          padding: 25,
          font: { size: 10, weight: 700, family: 'Plus Jakarta Sans' },
        },
      },
      tooltip: {
        backgroundColor: '#1e293b',
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
