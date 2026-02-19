import { useMemo } from "react";
import { Signal } from "../types";
import { Card } from "primereact/card";

type Props = {
  signals: Signal[];
};

export function MetricsGrid({ signals }: Props) {
  const metrics = useMemo(() => {
    const total = signals.length;
    const inProgress = signals.filter((s) => s.status === "IN_PROGRESS").length;
    const newlyReported = signals.filter((s) => s.status === "NEW").length;
    const avgScore =
      total === 0
        ? "0"
        : (signals.reduce((acc, s) => acc + s.priorityScore, 0) / total).toFixed(1);

    return [
      { title: "Total Signals", value: String(total), icon: "pi-list", color: "text-cyan-400", border: "" },
      { title: "Newly Reported", value: String(newlyReported), icon: "pi-plus-circle", color: "text-blue-400", border: "blue" },
      { title: "Under Analysis", value: String(inProgress), icon: "pi-spin pi-cog", color: "text-orange-400", border: "orange" },
      { title: "Priority Avg", value: avgScore, icon: "pi-chart-bar", color: "text-green-400", border: "green" },
    ];
  }, [signals]);

  return (
    <div className="grid">
      {metrics.map((m) => (
        <div key={m.title} className="col-12 md:col-6 lg:col-3 animate-fade-in">
          <Card className={`shadow-2 border-1 border-gray-800 bg-gray-900 metric-card-inner ${m.border}`}>
            <div className="flex justify-content-between mb-2">
              <div>
                <span className="block text-gray-500 font-medium mb-2 uppercase text-xs tracking-wider">{m.title}</span>
                <div className={`text-4xl font-bold ${m.color}`}>{m.value}</div>
              </div>
              <div className="flex align-items-center justify-content-center bg-gray-800 border-round shadow-1" style={{ width: '2.5rem', height: '2.5rem' }}>
                <i className={`pi ${m.icon} text-xl ${m.color}`}></i>
              </div>
            </div>
          </Card>
        </div>
      ))}
    </div>
  );
}
