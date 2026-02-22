import { useMemo } from "react";
import { Signal } from "../types";
import { useTranslation } from "react-i18next";
import { CivicCard } from "./ui/CivicCard";

type Props = {
  signals: Signal[];
};

export function MetricsGrid({ signals }: Props) {
  const { t } = useTranslation();
  const metrics = useMemo(() => {
    const total = signals.length;
    const inProgress = signals.filter((s) => s.status === "IN_PROGRESS").length;
    const newlyReported = signals.filter((s) => s.status === "NEW").length;
    const avgScore =
      total === 0
        ? "0"
        : (signals.reduce((acc, s) => acc + s.priorityScore, 0) / total).toFixed(1);

    return [
      { id: 'total', title: t('metrics.total'), value: String(total), icon: "pi-list", color: "text-main", variant: "neutral" as const },
      { id: 'new', title: t('metrics.new'), value: String(newlyReported), icon: "pi-plus-circle", color: "text-status-new", variant: "brand" as const },
      { id: 'analysis', title: t('metrics.analysis'), value: String(inProgress), icon: "pi-cog", color: "text-status-progress", variant: "warning" as const },
      { id: 'avg', title: t('metrics.avg'), value: avgScore, icon: "pi-chart-bar", color: "text-status-resolved", variant: "success" as const },
    ];
  }, [signals, t]);

  return (
    <div className="grid grid-nogutter gap-4 mb-4" role="region" aria-label="Civic Metrics Overview">
      {metrics.map((m) => (
        <div key={m.title} className="col-12 sm:col-6 lg:col-3 flex">
          <CivicCard 
            variant={m.variant}
            className="w-full transition-transform transition-duration-200 hover:scale-105"
            data-testid={`metric-card-${m.id}`}
          >
            <div className="flex justify-content-between align-items-center">
              <div className="flex flex-column gap-1">
                <span className="text-muted font-black uppercase tracking-widest text-xs" aria-hidden="true">{m.title}</span>
                <div className={`text-4xl font-black ${m.color} tracking-tight`} data-testid={`metric-value-${m.id}`}>{m.value}</div>
              </div>
              <div className="flex align-items-center justify-content-center bg-white-alpha-5 border-round-xl shadow-4 border-1 border-white-alpha-10" style={{ width: '3.5rem', height: '3.5rem' }} aria-hidden="true">
                <i className={`pi ${m.icon} text-2xl ${m.color}`}></i>
              </div>
            </div>
          </CivicCard>
        </div>
      ))}
    </div>
  );
}
