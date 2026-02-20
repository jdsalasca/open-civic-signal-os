import { Signal } from "../types";
import { Card } from "primereact/card";
import { useTranslation } from "react-i18next";

type Props = {
  signals: Signal[];
};

export function DigestSidebar({ signals }: Props) {
  const { t } = useTranslation();
  const top3 = [...signals]
    .sort((a, b) => b.priorityScore - a.priorityScore)
    .slice(0, 3);

  return (
    <Card title={t('dashboard.broadcast')} className="shadow-4 border-1 border-white-alpha-10 bg-surface">
      <div className="flex flex-column gap-3">
        {top3.length === 0 ? (
          <p className="text-muted text-sm italic">{t('dashboard.empty_title')}</p>
        ) : (
          top3.map((s, i) => (
            <div key={s.id} className="flex gap-3 align-items-start">
              <span className="text-2xl font-black text-main opacity-20">0{i + 1}</span>
              <div>
                <h4 className="text-sm font-bold text-main m-0 line-height-2 mb-1">{s.title}</h4>
                <div className="flex align-items-center gap-2">
                  <span className="text-xs text-cyan-500 font-black uppercase">{s.priorityScore.toFixed(0)}</span>
                  <span className="text-muted">•</span>
                  <span className="text-xs text-muted font-bold uppercase">{t(`categories.${s.category}`)}</span>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </Card>
  );
}
