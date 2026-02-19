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

type Notification = {
  id: string;
  channel: string;
  message: string;
  recipientGroup: string;
  sentAt: string;
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

export function App() {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8081";
  const [signals, setSignals] = useState<Signal[]>([]);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  // Auth state
  const [isLoggedIn, setIsLoggedIn] = useState(!!localStorage.getItem("civic_auth"));
  const [showLogin, setShowLogin] = useState(false);
  const [credentials, setCredentials] = useState({ user: "", pass: "" });

  // Filters
  const [filterCategory, setFilterCategory] = useState("all");
  const [filterStatus, setFilterStatus] = useState("all");

  const getAuthHeader = () => {
    const auth = localStorage.getItem("civic_auth");
    return auth ? { 'Authorization': `Basic ${auth}` } : {};
  };

  const loadData = async () => {
    try {
      const signalsRes = await fetch(`${apiBaseUrl}/api/signals/prioritized`);
      if (signalsRes.ok) setSignals(await signalsRes.json());
      
      if (isLoggedIn) {
        const notificationsRes = await fetch(`${apiBaseUrl}/api/notifications/recent`, {
          headers: getAuthHeader()
        });
        if (notificationsRes.ok) setNotifications(await notificationsRes.json());
      }
      
    } catch (err) {
      setSignals(fallbackSignals);
      setError(err instanceof Error ? err.message : "Unknown API error");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [apiBaseUrl, isLoggedIn]);

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    const encoded = btoa(`${credentials.user}:${credentials.pass}`);
    localStorage.setItem("civic_auth", encoded);
    setIsLoggedIn(true);
    setShowLogin(false);
  };

  const handleLogout = () => {
    localStorage.removeItem("civic_auth");
    setIsLoggedIn(false);
    setNotifications([]);
  };

  const handleRelay = async () => {
    try {
      const res = await fetch(`${apiBaseUrl}/api/notifications/relay/top-10`, { 
        method: 'POST',
        headers: getAuthHeader()
      });
      if (res.ok) {
        alert("Broadcast relay sent successfully!");
        loadData();
      } else {
        alert("Unauthorized or error sending relay.");
      }
    } catch (err) {
      alert("Error sending relay.");
    }
  };

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
      <header className="main-header">
        <div>
          <h1>Open Civic Signal OS</h1>
          <p>Transparent civic prioritization dashboard (React + API integration).</p>
        </div>
        <div className="header-actions">
          {isLoggedIn ? (
            <>
              <button className="relay-btn" onClick={handleRelay}>
                📢 Broadcast Top 10 Relay
              </button>
              <button className="login-btn secondary" onClick={handleLogout}>Logout</button>
            </>
          ) : (
            <button className="login-btn" onClick={() => setShowLogin(true)}>Operator Login</button>
          )}
        </div>
      </header>

      {showLogin && (
        <div className="modal-overlay">
          <form className="login-modal" onSubmit={handleLogin}>
            <h2>Operator Login</h2>
            <input 
              type="text" placeholder="Username" required
              value={credentials.user} onChange={e => setCredentials({...credentials, user: e.target.value})}
            />
            <input 
              type="password" placeholder="Password" required
              value={credentials.pass} onChange={e => setCredentials({...credentials, pass: e.target.value})}
            />
            <div className="modal-actions">
              <button type="submit">Login</button>
              <button type="button" onClick={() => setShowLogin(false)}>Cancel</button>
            </div>
            <p className="hint">Try admin / civic2026</p>
          </form>
        </div>
      )}

      {error ? <p className="note">API connection status: {error}</p> : null}

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
          <div className="title-row">
            <h2>Prioritized Signals</h2>
            <div className="controls">
              <select value={filterCategory} onChange={(e) => setFilterCategory(e.target.value)}>
                {categories.map(c => <option key={c} value={c}>{c === "all" ? "All Categories" : c}</option>)}
              </select>
              <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
                {statuses.map(s => <option key={s} value={s}>{s === "all" ? "All Statuses" : s}</option>)}
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

        <aside className="sidebar-group">
          <section className="digest-sidebar">
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
          </section>

          {isLoggedIn && (
            <section className="notifications-sidebar">
              <h3>Recent Relays (Admin Only)</h3>
              <div className="notif-list">
                {notifications.length === 0 && <p className="small-note">No recent relays.</p>}
                {notifications.map(n => (
                  <div key={n.id} className="notif-item">
                    <div className="notif-header">
                      <span className="channel-badge">{n.channel}</span>
                      <span className="notif-date">{new Date(n.sentAt).toLocaleTimeString()}</span>
                    </div>
                    <p className="notif-msg">{n.message.substring(0, 60)}...</p>
                    <span className="notif-group">To: {n.recipientGroup}</span>
                  </div>
                ))}
              </div>
            </section>
          )}
        </aside>
      </div>
    </main>
  );
}
