import { useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { Signal, Notification, UserRole } from "../types";
import { MetricsGrid } from "../components/MetricsGrid";
import { SignalTable } from "../components/SignalTable";
import { DigestSidebar } from "../components/DigestSidebar";
import { NotificationSidebar } from "../components/NotificationSidebar";
import { LoginModal } from "../components/LoginModal";
import { Layout } from "../components/Layout";
import { ProgressBar } from "primereact/progressbar";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { useNavigate } from "react-router-dom";

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

export function Dashboard() {
  const navigate = useNavigate();
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "";
  const [signals, setSignals] = useState<Signal[]>([]);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  
  const [auth, setAuth] = useState<{user: string, role: UserRole} | null>(() => {
    const saved = localStorage.getItem("civic_auth_data");
    return saved ? JSON.parse(saved) : null;
  });
  const [showLogin, setShowLogin] = useState(false);

  const getAuthHeader = () => {
    const token = localStorage.getItem("civic_auth_token");
    return token ? { 'Authorization': `Basic ${token}` } : {};
  };

  const loadData = async () => {
    try {
      setLoading(true);
      const [signalsRes, notificationsRes] = await Promise.all([
        fetch(`${apiBaseUrl}/api/signals/prioritized?size=50`),
        (auth?.role === "PUBLIC_SERVANT" || auth?.role === "SUPER_ADMIN")
          ? fetch(`${apiBaseUrl}/api/notifications/recent`, { headers: getAuthHeader() })
          : Promise.resolve(null)
      ]);
      
      if (signalsRes.ok) {
        const data = await signalsRes.json();
        setSignals(data.content || []);
      }
      
      if (notificationsRes && notificationsRes.ok) {
        setNotifications(await notificationsRes.json());
      }
      
    } catch (err) {
      setSignals(fallbackSignals);
      toast.error("Using fallback data. API connection issue.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [auth]);

  const handleLogin = async (user: string, pass: string) => {
    const token = btoa(`${user}:${pass}`);
    try {
      const res = await fetch(`${apiBaseUrl}/api/auth/me`, {
        headers: { 'Authorization': `Basic ${token}` }
      });

      if (res.ok) {
        const userData = await res.json();
        const roleStr = userData.roles[0].authority;
        const role: UserRole = roleStr === "ROLE_SUPER_ADMIN" ? "SUPER_ADMIN" : 
                            roleStr === "ROLE_PUBLIC_SERVANT" ? "PUBLIC_SERVANT" : "CITIZEN";

        localStorage.setItem("civic_auth_token", token);
        const authInfo = { user, role };
        localStorage.setItem("civic_auth_data", JSON.stringify(authInfo));
        
        setAuth(authInfo);
        setShowLogin(false);
        toast.success(`Welcome back, ${user}!`);
      } else {
        toast.error("Invalid credentials.");
      }
    } catch (err) {
      toast.error("Auth service unavailable.");
    }
  };

  const handleLogout = () => {
    localStorage.removeItem("civic_auth_token");
    localStorage.removeItem("civic_auth_data");
    setAuth(null);
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
        toast.success("Relay sent!");
        loadData();
      } else {
        toast.error("Unauthorized.");
      }
    } catch (err) {
      toast.error("Relay error.");
    }
  };

  const isStaff = auth?.role === "PUBLIC_SERVANT" || auth?.role === "SUPER_ADMIN";

  return (
    <Layout auth={auth} onLogout={handleLogout} onLoginClick={() => setShowLogin(true)}>
      <div className="flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="text-4xl m-0">Community Backlog</h1>
          <p className="text-gray-500 m-0">Live signals prioritized by impact and urgency.</p>
        </div>
        {isStaff && (
          <Button 
            label="Broadcast Top 10" 
            icon="pi pi-megaphone" 
            severity="danger" 
            onClick={handleRelay} 
          />
        )}
      </div>

      {loading && <ProgressBar mode="indeterminate" style={{ height: '4px', marginBottom: '20px' }} />}

      <MetricsGrid signals={signals} />

      <div className="grid mt-4">
        <div className="col-12 lg:col-8">
          <SignalTable signals={signals} loading={loading} />
        </div>
        <div className="col-12 lg:col-4">
          <div className="flex flex-column gap-4">
            <DigestSidebar signals={signals} />
            
            {isStaff && (
              <NotificationSidebar notifications={notifications} />
            )}

            {auth?.role === "CITIZEN" && (
              <Card title="Citizen Action" subTitle="Report new issues in your sector">
                <Button 
                  label="Report New Issue" 
                  icon="pi pi-plus" 
                  className="w-full" 
                  onClick={() => navigate("/report")} 
                />
              </Card>
            )}
          </div>
        </div>
      </div>

      {showLogin && (
        <LoginModal 
          onLogin={handleLogin} 
          onClose={() => setShowLogin(false)} 
        />
      )}
    </Layout>
  );
}
