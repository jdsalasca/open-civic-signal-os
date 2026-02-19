import { useEffect, useState } from "react";
import { Toaster, toast } from "react-hot-toast";
import { Signal, Notification } from "./types";
import { MetricsGrid } from "./components/MetricsGrid";
import { SignalTable } from "./components/SignalTable";
import { DigestSidebar } from "./components/DigestSidebar";
import { NotificationSidebar } from "./components/NotificationSidebar";
import { LoginModal } from "./components/LoginModal";

const fallbackSignals: Signal[] = [
  {
    id: "sig-003",
    title: "Unsafe crossing near school",
    description: "The zebra crossing is not visible",
    category: "safety",
    status: "NEW",
    priorityScore: 320.8,
    scoreBreakdown: { urgency: 150, impact: 125, affectedPeople: 30, communityVotes: 15.8 }
  }
];

export function App() {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ""; // Default to relative
  const [signals, setSignals] = useState<Signal[]>([]);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  // Auth state
  const [isLoggedIn, setIsLoggedIn] = useState(!!localStorage.getItem("civic_auth"));
  const [showLogin, setShowLogin] = useState(false);

  const getAuthHeader = () => {
    const auth = localStorage.getItem("civic_auth");
    return auth ? { 'Authorization': `Basic ${auth}` } : {};
  };

  const loadData = async () => {
    try {
      setLoading(true);
      const [signalsRes, notificationsRes] = await Promise.all([
        fetch(`${apiBaseUrl}/api/signals/prioritized?size=50`),
        isLoggedIn 
          ? fetch(`${apiBaseUrl}/api/notifications/recent`, { headers: getAuthHeader() })
          : Promise.resolve(null)
      ]);
      
      if (signalsRes.ok) {
        const data = await signalsRes.json();
        setSignals(data.content || []);
      } else {
        toast.error("Failed to load signals.");
      }
      
      if (notificationsRes && notificationsRes.ok) {
        setNotifications(await notificationsRes.json());
      }
      
    } catch (err) {
      setSignals(fallbackSignals);
      const msg = err instanceof Error ? err.message : "Unknown API error";
      setError(msg);
      toast.error(`Connection error: ${msg}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [apiBaseUrl, isLoggedIn]);

  const handleLogin = (user: string, pass: string) => {
    const encoded = btoa(`${user}:${pass}`);
    localStorage.setItem("civic_auth", encoded);
    setIsLoggedIn(true);
    setShowLogin(false);
    toast.success("Welcome back, Operator!");
  };

  const handleLogout = () => {
    localStorage.removeItem("civic_auth");
    setIsLoggedIn(false);
    setNotifications([]);
    toast.success("Logged out successfully.");
  };

  const handleRelay = async () => {
    try {
      const res = await fetch(`${apiBaseUrl}/api/notifications/relay/top-10`, { 
        method: 'POST',
        headers: getAuthHeader()
      });
      if (res.ok) {
        toast.success("Broadcast relay sent successfully!");
        loadData();
      } else {
        toast.error("Unauthorized or error sending relay.");
      }
    } catch (err) {
      toast.error("Error connecting to notification service.");
    }
  };

  return (
    <main className="page">
      <Toaster position="top-right" />
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
        <LoginModal 
          onLogin={handleLogin} 
          onClose={() => setShowLogin(false)} 
        />
      )}

      {error ? <p className="note">API connection status: {error}</p> : null}

      <MetricsGrid signals={signals} />

      <div className="layout-split">
        <SignalTable signals={signals} loading={loading} />

        <aside className="sidebar-group">
          <DigestSidebar signals={signals} />
          
          {isLoggedIn && (
            <NotificationSidebar notifications={notifications} />
          )}
        </aside>
      </div>
    </main>
  );
}
