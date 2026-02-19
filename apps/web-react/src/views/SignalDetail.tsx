import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { Signal, UserRole } from "../types";

export function SignalDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [signal, setSignal] = useState<Signal | null>(null);
  const [loading, setLoading] = useState(true);

  // Get auth info
  const auth = (() => {
    const saved = localStorage.getItem("civic_auth_data");
    return saved ? JSON.parse(saved) : null;
  })();

  const fetchSignal = async () => {
    try {
      const res = await fetch(`/api/signals/${id}`);
      if (res.ok) {
        setSignal(await res.json());
      } else {
        toast.error("Signal not found.");
        navigate("/");
      }
    } catch (err) {
      toast.error("Error loading signal details.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSignal();
  }, [id, navigate]);

  const updateStatus = async (newStatus: string) => {
    const token = localStorage.getItem("civic_auth_token");
    try {
      const res = await fetch(`/api/signals/${id}/status`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Basic ${token}`
        },
        body: JSON.stringify({ status: newStatus })
      });

      if (res.ok) {
        toast.success(`Status updated to ${newStatus}`);
        fetchSignal();
      } else {
        toast.error("Failed to update status. Check permissions.");
      }
    } catch (err) {
      toast.error("Error updating status.");
    }
  };

  if (loading) return <div className="page">Loading details...</div>;
  if (!signal) return null;

  return (
    <div className="form-container">
      <header className="main-header">
        <div>
          <span className={`status-pill status-${signal.status.toLowerCase().replace("_", "")}`} style={{ marginBottom: '10px', display: 'inline-block' }}>
            {signal.status}
          </span>
          <h1>{signal.title}</h1>
          <p className="small-note">Category: {signal.category} | ID: {signal.id}</p>
        </div>
        <button className="login-btn secondary" onClick={() => navigate("/")}>Back to Dashboard</button>
      </header>

      {/* Staff Actions */}
      {auth?.role === "PUBLIC_SERVANT" && (
        <section className="card" style={{ marginBottom: '24px', borderColor: 'var(--accent-primary)', borderStyle: 'dashed' }}>
          <h3 style={{ fontSize: '1rem', marginBottom: '16px', color: 'var(--accent-primary)' }}>STAFF ACTIONS: Transition Status</h3>
          <div className="header-actions">
            <button className="login-btn" onClick={() => updateStatus('NEW')}>Set to NEW</button>
            <button className="login-btn" style={{ background: 'var(--status-progress)', color: '#000' }} onClick={() => updateStatus('IN_PROGRESS')}>Start Work (IN PROGRESS)</button>
            <button className="login-btn" style={{ background: 'var(--status-resolved)', color: '#000' }} onClick={() => updateStatus('RESOLVED')}>Resolve (FIXED)</button>
          </div>
        </section>
      )}

      <section className="grid" style={{ marginBottom: '24px' }}>
        <article className="card">
          <h2>{signal.priorityScore.toFixed(1)}</h2>
          <p>Global Priority Score</p>
        </article>
        <article className="card">
          <h2>{signal.scoreBreakdown.urgency}</h2>
          <p>Urgency Points</p>
        </article>
        <article className="card">
          <h2>{signal.scoreBreakdown.impact}</h2>
          <p>Impact Points</p>
        </article>
      </section>

      <div className="card" style={{ padding: '30px' }}>
        <h3>Problem Description</h3>
        <p style={{ lineHeight: '1.6', fontSize: '1.1rem', whiteSpace: 'pre-wrap', color: 'var(--text-main)', opacity: 0.9 }}>
          {signal.description || "No detailed description provided."}
        </p>
        
        <hr style={{ margin: '30px 0', opacity: 0.1 }} />
        
        <h3>Full Score Breakdown</h3>
        <div className="grid-form" style={{ marginTop: '10px' }}>
          <div>
            <div className="small-note">Urgency Factor</div>
            <strong>{signal.scoreBreakdown.urgency}</strong>
          </div>
          <div>
            <div className="small-note">Social Impact</div>
            <strong>{signal.scoreBreakdown.impact}</strong>
          </div>
          <div>
            <div className="small-note">Affected Population</div>
            <strong>{signal.scoreBreakdown.affectedPeople}</strong>
          </div>
          <div>
            <div className="small-note">Community Support</div>
            <strong>{signal.scoreBreakdown.communityVotes}</strong>
          </div>
        </div>
      </div>
    </div>
  );
}
