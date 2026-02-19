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
    <Card title={t('dashboard.broadcast')} className="shadow-4 border-1 border-white-alpha-10 bg-gray-900">
      <div className="flex flex-column gap-3">
        {top3.length === 0 ? (
          <p className="text-gray-500 text-sm italic">{t('dashboard.empty_title')}</p>
        ) : (
          top3.map((s, i) => (
            <div key={s.id} className="flex gap-3 align-items-start">
              <span className="text-2xl font-black text-white-alpha-20">0{i + 1}</span>
              <div>
                <h4 className="text-sm font-bold text-white m-0 line-height-2 mb-1">{s.title}</h4>
                <div className="flex align-items-center gap-2">
                  <span className="text-xs text-cyan-500 font-black uppercase">{s.priorityScore.toFixed(0)}</span>
                  <span className="text-gray-600">•</span>
                  <span className="text-xs text-gray-500 font-bold uppercase">{t(`categories.${s.category}`)}</span>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </Card>
  );
}
