import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";

export function ReportSignal() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    title: "",
    description: "",
    category: "infrastructure",
    urgency: 3,
    impact: 3,
    affectedPeople: 10
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch("/api/signals", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form)
      });

      if (res.ok) {
        toast.success("Signal reported successfully!");
        navigate("/");
      } else {
        const err = await res.json();
        toast.error(`Error: ${Object.values(err).join(", ")}`);
      }
    } catch (err) {
      toast.error("Network error reporting signal.");
    }
  };

  return (
    <div className="form-container">
      <header className="main-header">
        <h1>Report Civic Signal</h1>
        <button className="login-btn secondary" onClick={() => navigate("/")}>Back to Dashboard</button>
      </header>

      <form className="report-form card" onSubmit={handleSubmit}>
        <div className="input-group">
          <label>Short Title</label>
          <input 
            type="text" required placeholder="e.g. Broken bench in Central Park"
            value={form.title} onChange={e => setForm({...form, title: e.target.value})}
          />
        </div>

        <div className="input-group">
          <label>Detailed Description</label>
          <textarea 
            rows={4} placeholder="Describe the problem..."
            value={form.description} onChange={e => setForm({...form, description: e.target.value})}
          />
        </div>

        <div className="grid-form">
          <div className="input-group">
            <label>Category</label>
            <select value={form.category} onChange={e => setForm({...form, category: e.target.value})}>
              <option value="infrastructure">Infrastructure</option>
              <option value="safety">Safety</option>
              <option value="education">Education</option>
              <option value="environment">Environment</option>
            </select>
          </div>

          <div className="input-group">
            <label>Urgency (1-5)</label>
            <input type="number" min="1" max="5" value={form.urgency} onChange={e => setForm({...form, urgency: parseInt(e.target.value)})} />
          </div>

          <div className="input-group">
            <label>Impact (1-5)</label>
            <input type="number" min="1" max="5" value={form.impact} onChange={e => setForm({...form, impact: parseInt(e.target.value)})} />
          </div>

          <div className="input-group">
            <label>Est. People Affected</label>
            <input type="number" min="1" value={form.affectedPeople} onChange={e => setForm({...form, affectedPeople: parseInt(e.target.value)})} />
          </div>
        </div>

        <button type="submit" className="relay-btn" style={{ width: '100%', marginTop: '20px' }}>
          Submit Signal to Prioritization
        </button>
      </form>
    </div>
  );
}
