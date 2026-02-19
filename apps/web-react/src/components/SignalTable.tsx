import { useMemo, useState } from "react";
import { Signal } from "../types";

type Props = {
  signals: Signal[];
  loading: boolean;
};

export function SignalTable({ signals, loading }: Props) {
  const [filterCategory, setFilterCategory] = useState("all");
  const [filterStatus, setFilterStatus] = useState("all");

  const categories = useMemo(() => {
    const cats = new Set(signals.map((s) => s.category));
    return ["all", ...Array.from(cats)];
  }, [signals]);

  const statuses = ["all", "NEW", "IN_PROGRESS", "RESOLVED"];

  const filteredSignals = useMemo(() => {
    return signals.filter((s) => {
      const matchCat = filterCategory === "all" || s.category === filterCategory;
      const matchStatus = filterStatus === "all" || s.status === filterStatus;
      return matchCat && matchStatus;
    });
  }, [signals, filterCategory, filterStatus]);

  return (
    <section className="tableCard main-content">
      <div className="title-row">
        <h2>Prioritized Signals</h2>
        <div className="controls">
          <select value={filterCategory} onChange={(e) => setFilterCategory(e.target.value)}>
            {categories.map((c) => (
              <option key={c} value={c}>
                {c === "all" ? "All Categories" : c}
              </option>
            ))}
          </select>
          <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
            {statuses.map((s) => (
              <option key={s} value={s}>
                {s === "all" ? "All Statuses" : s}
              </option>
            ))}
          </select>
        </div>
      </div>

      {loading ? <p>Loading...</p> : null}
      <div className="tableWrap">
        <table>
          <thead>
            <tr>
              <th>Title</th>
              <th>Category</th>
              <th>Status</th>
              <th>Score</th>
              <th>Breakdown</th>
            </tr>
          </thead>
          <tbody>
            {filteredSignals.map((signal) => (
              <tr key={signal.id}>
                <td>{signal.title}</td>
                <td>{signal.category}</td>
                <td>
                  <span className={`status-pill status-${signal.status.toLowerCase().replace("_", "")}`}>
                    {signal.status}
                  </span>
                </td>
                <td style={{ fontWeight: "bold" }}>{signal.priorityScore.toFixed(1)}</td>
                <td>
                  {signal.scoreBreakdown && (
                    <>
                      <span className="breakdown-tag" title="Urgency">U: {signal.scoreBreakdown.urgency}</span>
                      <span className="breakdown-tag" title="Impact">I: {signal.scoreBreakdown.impact}</span>
                      <span className="breakdown-tag" title="Reach">R: {signal.scoreBreakdown.affectedPeople}</span>
                      <span className="breakdown-tag" title="Community">C: {signal.scoreBreakdown.communityVotes}</span>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
