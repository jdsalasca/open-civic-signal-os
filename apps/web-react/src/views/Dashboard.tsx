import { useEffect, useState, useCallback } from "react";
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
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";

interface ApiError extends Error {
  friendlyMessage?: string;
}

export function Dashboard() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { activeRole, userName } = useAuthStore();
  
  const [signals, setSignals] = useState<Signal[]>([]);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [signalsRes, notificationsRes] = await Promise.all([
        apiClient.get("signals/prioritized?size=50"),
        (activeRole === "PUBLIC_SERVANT" || activeRole === "SUPER_ADMIN")
          ? apiClient.get("notifications/recent")
          : Promise.resolve(null)
      ]);
      
      if (signalsRes.status === 200) {
        setSignals(signalsRes.data.content || []);
      }
      
      if (notificationsRes && notificationsRes.status === 200) {
        setNotifications(notificationsRes.data);
      }
      
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || "Synchronization failed.");
    } finally {
      setLoading(false);
    }
  }, [activeRole]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleRelay = async () => {
    try {
      const res = await apiClient.post("notifications/relay/top-10");
      if (res.status === 200) {
        toast.success("Intelligence broadcast successful!");
        loadData();
      }
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || "Relay rejected.");
    }
  };

  const isStaff = activeRole === "PUBLIC_SERVANT" || activeRole === "SUPER_ADMIN";

  return (
    <Layout>
      <section className="mb-6 flex flex-column lg:flex-row justify-content-between lg:align-items-center gap-4 animate-fade-in" data-testid="dashboard-hero">
        <div>
          <div className="flex align-items-center gap-2 mb-2">
            <span className="bg-cyan-900 text-cyan-400 text-xs font-bold px-2 py-1 border-round uppercase tracking-tighter">{t('dashboard.live_intel')}</span>
            <span className="text-gray-600 text-xs">•</span>
            <span className="text-gray-500 text-xs font-medium" data-testid="welcome-message">{t('dashboard.welcome')}, {userName}</span>
          </div>
          {/* P1-D: Cleaned title logic, allowing translation to define highlight via key or just direct text */}
          <h1 className="text-5xl font-black mb-2 tracking-tight line-height-1">
            {t('dashboard.title')}
          </h1>
          <p className="text-gray-500 text-lg max-w-30rem m-0">
            {t('dashboard.subtitle')}
          </p>
        </div>
        <div className="flex gap-3">
          {isStaff && (
            <Button 
              label={t('dashboard.broadcast')} 
              icon="pi pi-bolt" 
              severity="danger"
              className="p-button-primary px-4 py-3 shadow-6 font-bold"
              onClick={handleRelay} 
              data-testid="broadcast-button"
            />
          )}
          <Button 
            label={t('dashboard.new_issue')} 
            icon="pi pi-plus" 
            className="p-button-primary px-4 py-3 shadow-6 font-bold" 
            onClick={() => navigate("/report")} 
            data-testid="report-issue-button"
          />
        </div>
      </section>

      {loading && <ProgressBar mode="indeterminate" style={{ height: '2px', marginBottom: '24px' }} className="border-round" />}

      <MetricsGrid signals={loading ? [] : signals} />

      <div className="grid mt-2">
        <div className="col-12 xl:col-8" data-testid="main-signal-table">
          <SignalTable signals={signals} loading={loading} />
        </div>
        <div className="col-12 xl:col-4">
          <div className="flex flex-column gap-4">
            {!loading && signals.length > 0 && <CategoryChart signals={signals} />}
            <DigestSidebar signals={signals} />
            {isStaff && (
              <NotificationSidebar notifications={notifications} />
            )}
            {activeRole === "CITIZEN" && (
              <Card title={t('dashboard.citizen_support_title')} className="bg-blue-900 border-none shadow-4" data-testid="citizen-support-card">
                <p className="text-blue-100 line-height-3 mb-4 text-sm">
                  {t('dashboard.citizen_support_desc')}
                </p>
                <div className="flex align-items-center gap-2 text-blue-200">
                  <i className="pi pi-shield"></i>
                  <span className="text-xs font-bold uppercase tracking-wider">{t('dashboard.citizen_verification')}</span>
                </div>
              </Card>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}
