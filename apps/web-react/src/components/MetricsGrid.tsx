import { useMemo } from "react";
import { Signal } from "../types";

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
      { title: "Signals", value: String(total) },
      { title: "New", value: String(newlyReported) },
      { title: "In Progress", value: String(inProgress) },
      { title: "Avg Score", value: avgScore },
    ];
  }, [signals]);

  return (
    <section className="grid">
      {metrics.map((card) => (
        <article key={card.title} className="card">
          <h2>{card.value}</h2>
          <p>{card.title}</p>
        </article>
      ))}
    </section>
  );
}
