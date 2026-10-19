import { Signal } from "../types";
import { useTranslation } from "react-i18next";
import { CivicCard } from "./ui/CivicCard";
import { PriorityScale, StatusChip } from "./signalPrimitives";
import { useSignalStatusLabel } from "../utils/signalStatus";
import { useNavigate } from "react-router-dom";

type Props = {
  signals: Signal[];
  criticalThreshold?: number;
};

/**
 * A bulletin, not a table: the three strongest items on screen, numbered, each with the same
 * priority scale and lifecycle chip used everywhere else. The ordering is the backend's; this
 * component never recomputes the rank.
 */
export function DigestSidebar({ signals, criticalThreshold = 0 }: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const statusLabel = useSignalStatusLabel();
  const top3 = [...signals]
    .sort((a, b) => b.priorityScore - a.priorityScore)
    .slice(0, 3);

  return (
    <CivicCard title={t("dashboard.digest_title")} variant="brand" data-testid="dashboard-digest">
      <div className="flex flex-column gap-3">
        {top3.length === 0 ? (
          <div className="text-center py-4 text-muted italic text-sm">
            {t("dashboard.digest_empty")}
          </div>
        ) : (
          top3.map((s, i) => (
            <button
              key={s.id}
              type="button"
              className="signal-digest-item text-left border-none w-full"
              onClick={() => navigate(`/signal/${s.id}`)}
              data-testid={`dashboard-digest-item-${i + 1}`}
            >
              <span className="signal-digest-item__rank" aria-hidden="true">{i + 1}</span>
              <span className="signal-digest-item__copy">
                <span className="signal-digest-item__title">{s.title}</span>
                <span className="signal-digest-item__meta">
                  <PriorityScale score={s.priorityScore} threshold={criticalThreshold} compact testId={`dashboard-digest-priority-${i + 1}`} />
                  <StatusChip status={s.status} label={statusLabel(s.status)} />
                </span>
              </span>
            </button>
          ))
        )}
      </div>
    </CivicCard>
  );
}
