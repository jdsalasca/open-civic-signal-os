import { useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { Signal, Notification } from "../types";
import { MetricsGrid } from "../components/MetricsGrid";
import { SignalTable } from "../components/SignalTable";
import { DigestSidebar } from "../components/DigestSidebar";
import { NotificationSidebar } from "../components/NotificationSidebar";
import { CategoryChart } from "../components/CategoryChart";
import { ProgressBar } from "primereact/progressbar";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../store/useAuthStore";
import { Layout } from "../components/Layout";
import apiClient from "../api/axios";

export function Dashboard() {
  const navigate = useNavigate();
  const { role, user } = useAuthStore();
  
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
      // P2-17: Removed artificial delay. Feedback is now immediate.
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
        toast.success("Intelligence broadcast successful!");
        loadData();
      }
    } catch (err) {
      toast.error("Unauthorized: Operation rejected.");
    }
  };

  const isStaff = role === "PUBLIC_SERVANT" || role === "SUPER_ADMIN";

  return (
    <Layout>
      <section className="mb-6 flex flex-column lg:flex-row justify-content-between lg:align-items-center gap-4 animate-fade-in">
        <div>
          <div className="flex align-items-center gap-2 mb-2">
            <span className="bg-cyan-900 text-cyan-400 text-xs font-bold px-2 py-1 border-round uppercase tracking-tighter">Live Intelligence</span>
            <span className="text-gray-600 text-xs">•</span>
            <span className="text-gray-500 text-xs font-medium">Welcome back, {user}</span>
          </div>
          <h1 className="text-5xl font-black mb-2 tracking-tight line-height-1">
            Governance <span className="text-cyan-500">Insights</span>
          </h1>
          <p className="text-gray-500 text-lg max-w-30rem m-0">
            Real-time community signal prioritization and resolution lifecycle tracking.
          </p>
        </div>
        <div className="flex gap-3">
          {isStaff && (
            <Button 
              label="Weekly Broadcast" 
              icon="pi pi-bolt" 
              severity="danger"
              className="px-4 py-3 shadow-6 border-none font-bold"
              onClick={handleRelay} 
            />
          )}
          <Button 
            label="Report New Issue" 
            icon="pi pi-plus" 
            className="px-4 py-3 bg-cyan-600 border-none shadow-6 font-bold" 
            onClick={() => navigate("/report")} 
          />
        </div>
      </section>

      {loading && <ProgressBar mode="indeterminate" style={{ height: '2px', marginBottom: '24px' }} className="border-round" />}

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
            {role === "CITIZEN" && (
              <Card title="Civic Support" className="bg-blue-900 border-none shadow-4">
                <p className="text-blue-100 line-height-3 mb-4 text-sm">
                  Your votes and reports directly impact the algorithmic priority of issues in your community. Keep up the good work!
                </p>
                <div className="flex align-items-center gap-2 text-blue-200">
                  <i className="pi pi-shield"></i>
                  <span className="text-xs font-bold uppercase tracking-wider">Citizen Verification: Active</span>
                </div>
              </Card>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}
