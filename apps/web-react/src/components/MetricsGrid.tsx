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
      { id: 'total', title: "Total Signals", value: String(total), icon: "pi-list", color: "text-cyan-400", border: "" },
      { id: 'new', title: "Newly Reported", value: String(newlyReported), icon: "pi-plus-circle", color: "text-blue-400", border: "blue" },
      { id: 'analysis', title: "Under Analysis", value: String(inProgress), icon: "pi-spin pi-cog", color: "text-orange-400", border: "orange" },
      { id: 'avg', title: "Priority Avg", value: avgScore, icon: "pi-chart-bar", color: "text-green-400", border: "green" },
    ];
  }, [signals]);

  return (
    <div className="grid grid-nogutter gap-3 mb-4" role="region" aria-label="Civic Metrics Overview">
      {metrics.map((m) => (
        <div key={m.title} className="col-12 sm:col-6 lg:col-3 flex">
          <Card 
            className={`shadow-8 border-1 border-gray-800 bg-gray-900 metric-card-inner w-full transition-transform transition-duration-200 hover:scale-105 ${m.border}`}
            data-testid={`metric-card-${m.id}`}
          >
            <div className="flex justify-content-between align-items-center">
              <div className="flex flex-column gap-1">
                <span className="text-gray-400 font-black uppercase tracking-widest" style={{ fontSize: '10px' }}>{m.title}</span>
                <div className={`text-4xl font-black ${m.color} tracking-tight`} data-testid={`metric-value-${m.id}`}>{m.value}</div>
              </div>
              <div className="flex align-items-center justify-content-center bg-gray-800 border-round-xl shadow-4 border-1 border-white-alpha-10" style={{ width: '3.5rem', height: '3rem' }}>
                <i className={`pi ${m.icon} text-xl ${m.color} font-bold`}></i>
              </div>
            </div>
          </Card>
        </div>
      ))}
    </div>
  );
}
