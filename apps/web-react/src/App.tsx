import { BrowserRouter, Routes, Route, Link } from "react-router-dom";
import { Toaster } from "react-hot-toast";
import { Dashboard } from "./views/Dashboard";
import { ReportSignal } from "./views/ReportSignal";
import { SignalDetail } from "./views/SignalDetail";
import { NotFound } from "./views/NotFound";

export function App() {
  return (
    <BrowserRouter>
      <Toaster position="top-right" />
      <main className="page">
        <nav className="top-nav">
          <Link to="/" className="nav-logo">SIGNAL OS</Link>
          <div className="nav-links">
            <Link to="/">Dashboard</Link>
            <Link to="/report" className="report-link">Report Issue</Link>
          </div>
        </nav>

        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/report" element={<ReportSignal />} />
          <Route path="/signal/:id" element={<SignalDetail />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
    </BrowserRouter>
  );
}
