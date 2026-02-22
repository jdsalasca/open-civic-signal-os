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
        backgroundColor: "rgba(99, 102, 241, 0.2)",
        borderColor: "#6366f1",
        borderWidth: 2,
        pointBackgroundColor: "#6366f1",
        pointBorderColor: "#fff",
        pointHoverBackgroundColor: "#fff",
        pointHoverBorderColor: "#6366f1",
      },
    ],
  };

  const options = {
    scales: {
      r: {
        angleLines: { color: "rgba(255, 255, 255, 0.1)" },
        grid: { color: "rgba(255, 255, 255, 0.1)" },
        pointLabels: {
          color: "#94a3b8",
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
        backgroundColor: "#1e293b",
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
