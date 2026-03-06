import { Signal } from "../types";
import { useTranslation } from "react-i18next";
import { CivicCard } from "./ui/CivicCard";
import { CivicBadge } from "./ui/CivicBadge";
import { useNavigate } from "react-router-dom";

type Props = {
  signals: Signal[];
};

export function DigestSidebar({ signals }: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const top3 = [...signals]
    .sort((a, b) => b.priorityScore - a.priorityScore)
    .slice(0, 3);

  return (
    <CivicCard title={t('dashboard.digest_title')} variant="brand">
      <div className="flex flex-column gap-5">
        {top3.length === 0 ? (
          <div className="text-center py-4 text-muted italic text-sm">
            {t('dashboard.digest_empty')}
          </div>
        ) : (
          top3.map((s, i) => (
            <div key={s.id} className="u-surface-note flex gap-4 align-items-start group cursor-pointer hover:bg-surface-soft transition-colors" onClick={() => navigate(`/signal/${s.id}`)}>
              <span className="text-3xl font-black text-brand-primary opacity-20 group-hover:opacity-100 transition-opacity flex-shrink-0">0{i + 1}</span>
              <div className="flex flex-column gap-2 overflow-hidden flex-1 min-w-0">
                <h4 className="u-list-item-title m-0 group-hover:text-brand-primary transition-colors">
                  {s.title}
                </h4>
                <div className="u-card-meta-row">
                  <span className="text-xs text-brand-primary font-black">{s.priorityScore.toFixed(0)} Pts</span>
                  <span className="text-muted font-bold">|</span>
                  <CivicBadge label={t(`categories.${s.category}`)} type="category" />
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </CivicCard>
  );
}
