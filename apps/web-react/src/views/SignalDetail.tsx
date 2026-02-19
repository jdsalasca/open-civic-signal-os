import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { Signal } from "../types";

export function SignalDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [signal, setSignal] = useState<Signal | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchSignal() {
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
    }
    fetchSignal();
  }, [id, navigate]);

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
        <p style={{ lineHeight: '1.6', fontSize: '1.1rem', whiteSpace: 'pre-wrap' }}>
          {signal.description || "No detailed description provided."}
        </p>
        
        <hr style={{ margin: '30px 0', opacity: 0.1 }} />
        
        <h3>Full Score Breakdown</h3>
        <div className="grid-form" style={{ marginTop: '10px' }}>
          <div>
            <strong>Urgency:</strong> {signal.scoreBreakdown.urgency}
          </div>
          <div>
            <strong>Impact:</strong> {signal.scoreBreakdown.impact}
          </div>
          <div>
            <strong>Reach:</strong> {signal.scoreBreakdown.affectedPeople}
          </div>
          <div>
            <strong>Community:</strong> {signal.scoreBreakdown.communityVotes}
          </div>
        </div>
      </div>
    </div>
  );
}
