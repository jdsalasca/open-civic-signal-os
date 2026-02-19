import { useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { Signal, Notification } from "../types";
import { MetricsGrid } from "../components/MetricsGrid";
import { SignalTable } from "../components/SignalTable";
import { DigestSidebar } from "../components/DigestSidebar";
import { NotificationSidebar } from "../components/NotificationSidebar";
import { CategoryChart } from "../components/CategoryChart";
import { Button } from "primereact/button";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../store/useAuthStore";
import apiClient from "../api/axios";

export function Dashboard() {
  const navigate = useNavigate();
  const { isLoggedIn, role } = useAuthStore();
  
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
      toast.error("Network synchronization failed.");
    } finally {
      // Simulate slightly longer load for UX feel of computation
      setTimeout(() => setLoading(false), 600);
    }
  };

  useEffect(() => {
    loadData();
  }, [role]);

  const handleRelay = async () => {
    try {
      const res = await apiClient.post("/api/notifications/relay/top-10");
      if (res.status === 200) {
        toast.success("Intelligence broadcast successful!");
        loadData();
      }
    } catch (err) {
      toast.error("Permission denied.");
    }
  };

  const isStaff = role === "PUBLIC_SERVANT" || role === "SUPER_ADMIN";

  return (
    <div className="page-container">
      <section className="mb-6 flex flex-column lg:flex-row justify-content-between lg:align-items-center gap-4 animate-fade-in">
        <div>
          <h1 className="text-5xl font-black mb-2 tracking-tight line-height-1">
            Governance <span className="text-cyan-500">Intelligence</span>
          </h1>
          <p className="text-gray-500 text-lg max-w-30rem m-0">
            Automated community signals prioritization and transparent resolution tracking.
          </p>
        </div>
        <div className="flex gap-3">
          {isStaff && (
            <Button 
              label="Weekly Broadcast" 
              icon="pi pi-bolt" 
              severity="danger"
              className="px-4 py-3 shadow-6 border-none"
              onClick={handleRelay} 
            />
          )}
          {role === "CITIZEN" && (
            <Button 
              label="Report Issue" 
              icon="pi pi-plus" 
              className="px-4 py-3 bg-cyan-600 border-none shadow-6" 
              onClick={() => navigate("/report")} 
            />
          )}
          {!isLoggedIn && (
            <Button 
              label="Get Involved" 
              icon="pi pi-user-plus" 
              outlined
              className="px-4 py-3 border-cyan-500 text-cyan-400" 
              onClick={() => navigate("/register")} 
            />
          )}
        </div>
      </section>

      <MetricsGrid signals={loading ? [] : signals} />

      <div className="grid mt-2">
        <div className="col-12 xl:col-8">
          <SignalTable signals={signals} loading={loading} />
        </div>
        <div className="col-12 xl:col-4">
          <div className="flex flex-column gap-4">
            {!loading && <CategoryChart signals={signals} />}
            <DigestSidebar signals={signals} />
            {isStaff && (
              <NotificationSidebar notifications={notifications} />
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
