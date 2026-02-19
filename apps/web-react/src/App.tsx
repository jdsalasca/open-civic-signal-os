import { useEffect, useMemo, useState } from "react";

type Signal = {
  id: string;
  title: string;
  category: string;
  status: string;
  priorityScore: number;
  scoreBreakdown: {
    urgency: number;
    impact: number;
    affectedPeople: number;
    communityVotes: number;
  };
};

const fallbackSignals: Signal[] = [
  {
    id: "sig-003",
    title: "Unsafe crossing near school",
    category: "safety",
    status: "NEW",
    priorityScore: 320.8,
    scoreBreakdown: { urgency: 150, impact: 125, affectedPeople: 30, communityVotes: 15.8 }
  }
];

export function App() {`n  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8081";
  const [signals, setSignals] = useState<Signal[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  // Filters
  const [filterCategory, setFilterCategory] = useState("all");
  const [filterStatus, setFilterStatus] = useState("all");

  useEffect(() => {
    async function loadSignals() {
      try {
        const response = await fetch(`${apiBaseUrl}/api/signals/prioritized`);
        if (!response.ok) {
          throw new Error(`Request failed with status ${response.status}`);
        }
        const data = (await response.json()) as Signal[];
        setSignals(data);
      } catch (err) {
        setSignals(fallbackSignals);
        setError(err instanceof Error ? err.message : "Unknown API error");
      } finally {
        setLoading(false);
      }
    }

    loadSignals();
  }, [apiBaseUrl]);

  const categories = useMemo(() => {
    const cats = new Set(signals.map(s => s.category));
    return ["all", ...Array.from(cats)];
  }, [signals]);

  const statuses = ["all", "NEW", "IN_PROGRESS", "RESOLVED"];

  const filteredSignals = useMemo(() => {
    return signals.filter(s => {
      const matchCat = filterCategory === "all" || s.category === filterCategory;
      const matchStatus = filterStatus === "all" || s.status === filterStatus;
      return matchCat && matchStatus;
    });
  }, [signals, filterCategory, filterStatus]);

  const topUnresolved = useMemo(() => {
    return signals
      .filter(s => s.status === "NEW")
      .sort((a, b) => b.priorityScore - a.priorityScore)
      .slice(0, 10);
  }, [signals]);

  const metrics = useMemo(() => {
    const total = signals.length;
    const inProgress = signals.filter((signal) => signal.status === "IN_PROGRESS").length;
    const newlyReported = signals.filter((signal) => signal.status === "NEW").length;
    const avgScore =
      total === 0
        ? "0"
        : (signals.reduce((acc, signal) => acc + signal.priorityScore, 0) / total).toFixed(1);

    return [
      { title: "Signals", value: String(total) },
      { title: "New", value: String(newlyReported) },
      { title: "In Progress", value: String(inProgress) },
      { title: "Avg Score", value: avgScore }
    ];
  }, [signals]);

  return (
    <main className="page">
      <header>
        <h1>Open Civic Signal OS</h1>
        <p>Transparent civic prioritization dashboard (React + API integration).</p>
      </header>

      {error ? <p className="note">API unavailable, showing fallback sample. Reason: {error}</p> : null}

      <section className="grid">
        {metrics.map((card) => (
          <article key={card.title} className="card">
            <h2>{card.value}</h2>
            <p>{card.title}</p>
          </article>
        ))}
      </section>

      <div className="layout-split">
        <section className="tableCard main-content">
          <h2>Prioritized Signals</h2>
          
          <div className="controls">
            <select value={filterCategory} onChange={(e) => setFilterCategory(e.target.value)}>
              {categories.map(c => <option key={c} value={c}>{c === "all" ? "All Categories" : c}</option>)}
            </select>
            <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
              {statuses.map(s => <option key={s} value={s}>{s === "all" ? "All Statuses" : s}</option>)}
            </select>
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
                    <td style={{ fontWeight: 'bold' }}>{signal.priorityScore.toFixed(1)}</td>
                    <td>
                      <span className="breakdown-tag" title="Urgency">U: {signal.scoreBreakdown.urgency}</span>
                      <span className="breakdown-tag" title="Impact">I: {signal.scoreBreakdown.impact}</span>
                      <span className="breakdown-tag" title="Reach">R: {signal.scoreBreakdown.affectedPeople}</span>
                      <span className="breakdown-tag" title="Community">C: {signal.scoreBreakdown.communityVotes}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <aside className="digest-sidebar">
          <h2>Top 10 Unresolved</h2>
          <p className="small-note">Weekly high-priority community needs.</p>
          <div className="digest-list">
            {topUnresolved.map((signal, idx) => (
              <div key={signal.id} className="digest-item">
                <span className="digest-rank">#{idx + 1}</span>
                <div className="digest-info">
                  <span className="digest-title">{signal.title}</span>
                  <span className="digest-meta">{signal.category} • Score: {signal.priorityScore.toFixed(0)}</span>
                </div>
              </div>
            ))}
          </div>
        </aside>
      </div>
    </main>
  );
}

