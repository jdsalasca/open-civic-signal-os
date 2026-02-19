import { useMemo } from "react";
import { Signal } from "../types";

type Props = {
  signals: Signal[];
};

export function DigestSidebar({ signals }: Props) {
  const topUnresolved = useMemo(() => {
    return signals
      .filter((s) => s.status === "NEW")
      .sort((a, b) => b.priorityScore - a.priorityScore)
      .slice(0, 10);
  }, [signals]);

  return (
    <section className="digest-sidebar">
      <h2>Top 10 Unresolved</h2>
      <p className="small-note">Weekly high-priority community needs.</p>
      <div className="digest-list">
        {topUnresolved.map((signal, idx) => (
          <div key={signal.id} className="digest-item">
            <span className="digest-rank">#{idx + 1}</span>
            <div className="digest-info">
              <span className="digest-title">{signal.title}</span>
              <span className="digest-meta">
                {signal.category} • Score: {signal.priorityScore.toFixed(0)}
              </span>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
