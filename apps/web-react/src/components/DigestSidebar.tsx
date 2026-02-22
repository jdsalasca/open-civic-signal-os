import { Signal } from "../types";
import { useTranslation } from "react-i18next";
import { CivicCard } from "./ui/CivicCard";
import { CivicBadge } from "./ui/CivicBadge";

type Props = {
  signals: Signal[];
};

export function DigestSidebar({ signals }: Props) {
  const { t } = useTranslation();
  const top3 = [...signals]
    .sort((a, b) => b.priorityScore - a.priorityScore)
    .slice(0, 3);

  return (
    <CivicCard title="High Priority Digest" variant="brand">
      <div className="flex flex-column gap-5">
        {top3.length === 0 ? (
          <div className="text-center py-4 text-muted italic text-sm">
            No priority data available for digest.
          </div>
        ) : (
          top3.map((s, i) => (
            <div key={s.id} className="flex gap-4 align-items-start group cursor-pointer">
              <span className="text-3xl font-black text-brand-primary opacity-20 group-hover:opacity-100 transition-opacity">0{i + 1}</span>
              <div className="flex flex-column gap-1 overflow-hidden">
                <h4 className="text-sm font-black text-main m-0 leading-tight truncate group-hover:text-brand-primary transition-colors">
                  {s.title}
                </h4>
                <div className="flex align-items-center gap-2">
                  <span className="text-xs text-brand-primary font-black">{s.priorityScore.toFixed(0)} Pts</span>
                  <span className="text-white-alpha-10 font-bold">|</span>
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
