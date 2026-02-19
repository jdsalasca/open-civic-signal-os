import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Signal } from "../types";

type Props = {
  signals: Signal[];
  loading: boolean;
};

export function SignalTable({ signals, loading }: Props) {
  const navigate = useNavigate();
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
    <section className="tableCard main-content card">
      <div className="title-row">
        <h2>Prioritized Backlog</h2>
        <div className="controls">
          <select value={filterCategory} onChange={(e) => setFilterCategory(e.target.value)}>
            {categories.map((c) => (
              <option key={c} value={c}>
                {c === "all" ? "Categories" : c}
              </option>
            ))}
          </select>
          <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
            {statuses.map((s) => (
              <option key={s} value={s}>
                {s === "all" ? "Status" : s}
              </option>
            ))}
          </select>
        </div>
      </div>

      {loading && <div style={{ padding: '20px', textAlign: 'center', color: 'var(--text-muted)' }}>Updating signals...</div>}
      
      <div className="tableWrap">
        <table>
          <thead>
            <tr>
              <th>Civic Need</th>
              <th>Category</th>
              <th>Status</th>
              <th>Priority</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {filteredSignals.map((signal) => (
              <tr key={signal.id} style={{ cursor: 'pointer' }} onClick={() => navigate(`/signal/${signal.id}`)}>
                <td>
                  <div style={{ fontWeight: 600 }}>{signal.title}</div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                    ID: {signal.id.substring(0, 8)}...
                  </div>
                </td>
                <td>
                  <span style={{ opacity: 0.7 }}>{signal.category}</span>
                </td>
                <td>
                  <span className={`status-pill status-${signal.status.toLowerCase().replace("_", "")}`}>
                    {signal.status}
                  </span>
                </td>
                <td>
                  <div style={{ display: 'flex', alignItems: 'baseline', gap: '4px' }}>
                    <span style={{ fontWeight: 800, fontSize: '1.1rem', color: 'var(--accent-primary)' }}>
                      {signal.priorityScore.toFixed(0)}
                    </span>
                  </div>
                </td>
                <td style={{ textAlign: 'right', paddingRight: '24px' }}>
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ opacity: 0.3 }}>
                    <path d="m9 18 6-6-6-6"/>
                  </svg>
                </td>
              </tr>
            ))}
            {filteredSignals.length === 0 && !loading && (
              <tr>
                <td colSpan={5} style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>
                  No civic signals match the selected filters.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
