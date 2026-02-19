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
      toast.error("Error connecting to services.");
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
        toast.success("Relay sent!");
        loadData();
      }
    } catch (err) {
      toast.error("Failed to send relay.");
    }
  };

  const isStaff = role === "PUBLIC_SERVANT" || role === "SUPER_ADMIN";

  return (
    <>
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

            {role === "CITIZEN" && (
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
    </>
  );
}
