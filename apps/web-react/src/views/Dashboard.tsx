import { useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { Signal, Notification } from "../types";
import { MetricsGrid } from "../components/MetricsGrid";
import { SignalTable } from "../components/SignalTable";
import { DigestSidebar } from "../components/DigestSidebar";
import { NotificationSidebar } from "../components/NotificationSidebar";
import { ProgressBar } from "primereact/progressbar";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../store/useAuthStore";
import apiClient from "../api/axios";

export function Dashboard() {
  const navigate = useNavigate();
  const { isLoggedIn, role, user } = useAuthStore();
  
  const [signals, setSignals] = useState<Signal[]>([]);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    try {
      setLoading(true);
      const [signalsRes, notificationsRes] = await Promise.all([
        apiClient.get("/api/signals/prioritized?size=50"),
        (role === "PUBLIC_SERVANT" || role === "SUPER_ADMIN")
          ? apiClient.get("/api/notifications/recent")
          : Promise.resolve(null)
      ]);
      
      if (signalsRes.status === 200) {
        setSignals(signalsRes.data.content || []);
      }
      
      if (notificationsRes && notificationsRes.status === 200) {
        setNotifications(notificationsRes.data);
      }
      
    } catch (err) {
      toast.error("Network synchronization error.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [role]);

  const handleRelay = async () => {
    try {
      const res = await apiClient.post("/api/notifications/relay/top-10");
      if (res.status === 200) {
        toast.success("Broadcast successful!");
        loadData();
      }
    } catch (err) {
      toast.error("Unauthorized: Operation rejected.");
    }
  };

  const isStaff = role === "PUBLIC_SERVANT" || role === "SUPER_ADMIN";

  return (
    <div className="page-container">
      <section className="mb-6 flex flex-column md:flex-row justify-content-between align-items-end gap-4 animate-fade-in">
        <div>
          <h1 className="text-5xl font-black mb-2 tracking-tight">
            Signal <span className="text-cyan-500">Intelligence</span>
          </h1>
          <p className="text-gray-500 text-lg max-w-30rem line-height-3">
            Real-time algorithmic prioritization of community reported needs and infrastructure signals.
          </p>
        </div>
        <div className="flex gap-3">
          {isStaff && (
            <Button 
              label="Run Weekly Relay" 
              icon="pi pi-bolt" 
              severity="danger"
              className="px-4 py-3 shadow-4 hover:shadow-6"
              onClick={handleRelay} 
            />
          )}
          {role === "CITIZEN" && (
            <Button 
              label="Report Civic Issue" 
              icon="pi pi-plus" 
              className="px-4 py-3 bg-cyan-600 border-none shadow-4" 
              onClick={() => navigate("/report")} 
            />
          )}
        </div>
      </section>

      {loading && <ProgressBar mode="indeterminate" style={{ height: '2px', marginBottom: '24px' }} className="border-round" />}

      <MetricsGrid signals={signals} />

      <div className="grid mt-2">
        <div className="col-12 xl:col-8">
          <SignalTable signals={signals} loading={loading} />
        </div>
        <div className="col-12 xl:col-4">
          <div className="flex flex-column gap-4">
            <DigestSidebar signals={signals} />
            
            {isStaff && (
              <NotificationSidebar notifications={notifications} />
            )}

            {!isLoggedIn && (
              <Card title="Civic Participation" className="bg-cyan-900 border-none shadow-4">
                <p className="text-cyan-100 line-height-3 mb-4">
                  Join the platform to report issues, support community initiatives and track resolution status in real-time.
                </p>
                <Button label="Get Started" severity="info" className="w-full bg-cyan-500 border-none text-gray-900 font-bold" onClick={() => navigate('/register')} />
              </Card>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
