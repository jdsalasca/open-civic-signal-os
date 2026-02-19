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
    <div className="grid grid-nogutter gap-3 mb-4">
      {metrics.map((m) => (
        <div key={m.title} className="col-12 sm:col-6 lg:col-3 flex">
          <Card className={`shadow-4 border-1 border-gray-800 bg-gray-900 metric-card-inner w-full ${m.border}`}>
            <div className="flex justify-content-between align-items-center">
              <div className="flex flex-column gap-1">
                <span className="text-gray-600 font-bold uppercase" style={{ fontSize: '10px', letterSpacing: '1.5px' }}>{m.title}</span>
                <div className={`text-4xl font-black ${m.color}`}>{m.value}</div>
              </div>
              <div className="flex align-items-center justify-content-center bg-gray-800 border-round-xl shadow-2" style={{ width: '3rem', height: '3rem' }}>
                <i className={`pi ${m.icon} text-xl ${m.color}`}></i>
              </div>
            </div>
          </Card>
        </div>
      ))}
    </div>
  );
}
