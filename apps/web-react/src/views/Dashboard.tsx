import { useEffect, useState, useCallback } from "react";
import { toast } from "react-hot-toast";
import { Signal, Notification, SignalMeta } from "../types";
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
  const [meta, setMeta] = useState<SignalMeta | null>(null);
  const [duplicateClusters, setDuplicateClusters] = useState(0);
  const [totalRecords, setTotalRecords] = useState(0);
  const [lazyState, setLazyState] = useState({
    first: 0,
    rows: 10,
    page: 0
  });

  const loadData = useCallback(async (signal?: AbortSignal) => {
    try {
      setLoading(true);
      const [signalsRes, metaRes, notificationsRes, duplicatesRes] = await Promise.all([
        apiClient.get(`signals/prioritized?page=${lazyState.page}&size=${lazyState.rows}`, { signal }),
        apiClient.get("signals/meta", { signal }),
        (activeRole === "PUBLIC_SERVANT" || activeRole === "SUPER_ADMIN")
          ? apiClient.get("notifications/recent", { signal })
          : Promise.resolve(null),
        (activeRole === "PUBLIC_SERVANT" || activeRole === "SUPER_ADMIN")
          ? apiClient.get("signals/duplicates", { signal })
          : Promise.resolve(null)
      ]);
      
      if (signalsRes.status === 200) {
        setSignals(signalsRes.data.content || []);
        setTotalRecords(signalsRes.data.totalElements || 0);
      }

      if (metaRes.status === 200) {
        setMeta(metaRes.data);
      }
      
      if (notificationsRes && notificationsRes.status === 200) {
        setNotifications(notificationsRes.data);
      }

      if (duplicatesRes && duplicatesRes.status === 200) {
        const clusterCount = Object.keys(duplicatesRes.data || {}).length;
        setDuplicateClusters(clusterCount);
      }
      
    } catch (err: any) {
      if (err.name === 'CanceledError' || err.name === 'AbortError') return;
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('dashboard.sync_error'));
    } finally {
      setLoading(false);
    }
  }, [activeRole, t, lazyState]);

  useEffect(() => {
    const controller = new AbortController();
    loadData(controller.signal);
    return () => controller.abort();
  }, [loadData]);

  const onPage = (event: any) => {
    setLazyState(event);
  };

  const handleRelay = async () => {
    try {
      const res = await apiClient.post("notifications/relay/top-10");
      if (res.status === 200) {
        toast.success(t('dashboard.broadcast_success'));
        loadData();
      }
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('dashboard.relay_rejected'));
    }
  };

  const isStaff = activeRole === "PUBLIC_SERVANT" || activeRole === "SUPER_ADMIN";

  const formatLastUpdated = (isoDate: string | null) => {
    if (!isoDate) {
      return t('dashboard.freshness_pending');
    }
    const date = new Date(isoDate);
    if (Number.isNaN(date.getTime())) {
      return t('dashboard.freshness_pending');
    }
    const diffMinutes = Math.max(0, Math.floor((Date.now() - date.getTime()) / 60000));
    if (diffMinutes < 1) {
      return t('dashboard.freshness_now');
    }
    if (diffMinutes < 60) {
      return t('dashboard.freshness_minutes', { count: diffMinutes });
    }
    const diffHours = Math.floor(diffMinutes / 60);
    if (diffHours < 24) {
      return t('dashboard.freshness_hours', { count: diffHours });
    }
    const diffDays = Math.floor(diffHours / 24);
    return t('dashboard.freshness_days', { count: diffDays });
  };

  return (
    <Layout>
      <section className="mb-6 flex flex-column lg:flex-row justify-content-between lg:align-items-center gap-4 animate-fade-in" data-testid="dashboard-hero">
        <div>
          <div className="flex align-items-center gap-2 mb-2">
            <span className="bg-cyan-900 text-cyan-400 text-xs font-bold px-2 py-1 border-round uppercase tracking-tighter">{t('dashboard.live_intel')}</span>
            <span className="text-muted text-xs">•</span>
            <span className="text-muted text-xs font-medium" data-testid="welcome-message">{t('dashboard.welcome')}, {userName}</span>
            {meta && (
              <>
                <span className="text-muted text-xs">•</span>
                <span className="text-muted text-xs font-medium" data-testid="dashboard-freshness-badge">
                  {t('dashboard.freshness_label')}: {formatLastUpdated(meta.lastUpdatedAt)}
                </span>
              </>
            )}
          </div>
          {/* P1-D: Cleaned title logic, allowing translation to define highlight via key or just direct text */}
          <h1 className="text-5xl font-black mb-2 tracking-tight line-height-1 text-main">
            {t('dashboard.title')}
          </h1>
          <p className="text-muted text-lg max-w-30rem m-0">
            {t('dashboard.subtitle')}
          </p>
          {meta && (
            <div className="mt-3 flex flex-wrap gap-2" data-testid="dashboard-meta-summary">
              <span className="bg-white-alpha-5 text-main text-xs font-semibold px-2 py-1 border-round border-1 border-white-alpha-10">
                {t('metrics.total')}: {meta.totalSignals}
              </span>
              <span className="bg-white-alpha-5 text-main text-xs font-semibold px-2 py-1 border-round border-1 border-white-alpha-10">
                {t('dashboard.open_signals')}: {meta.unresolvedSignals}
              </span>
            </div>
          )}
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
          <SignalTable 
            signals={signals} 
            loading={loading} 
            totalRecords={totalRecords}
            rows={lazyState.rows}
            first={lazyState.first}
            onPage={onPage}
          />
        </div>
        <div className="col-12 xl:col-4">
          <div className="flex flex-column gap-4">
            {!loading && signals.length > 0 && <CategoryChart signals={signals} />}
            <DigestSidebar signals={signals} />
            {isStaff && (
              <NotificationSidebar notifications={notifications} />
            )}
            {isStaff && (
              <Card className="bg-surface border-1 border-white-alpha-10 shadow-4" data-testid="duplicates-insight-card">
                <div className="flex align-items-start justify-content-between gap-3">
                  <div>
                    <h3 className="text-sm font-black text-main uppercase tracking-widest mt-0 mb-2">
                      {t('dashboard.duplicates_title')}
                    </h3>
                    <p className="text-sm text-muted m-0">
                      {t('dashboard.duplicates_desc', { count: duplicateClusters })}
                    </p>
                  </div>
                  <Button
                    label={t('dashboard.review_queue')}
                    icon="pi pi-eye"
                    outlined
                    severity="info"
                    onClick={() => navigate('/moderation')}
                    data-testid="duplicates-review-button"
                  />
                </div>
              </Card>
            )}
            {activeRole === "CITIZEN" && (
              <Card title={t('dashboard.citizen_support_title')} className="bg-surface border-1 border-white-alpha-10 shadow-4" data-testid="citizen-support-card">
                <p className="text-muted line-height-3 mb-4 text-sm">
                  {t('dashboard.citizen_support_desc')}
                </p>
                <div className="flex align-items-center gap-2 text-cyan-500">
                  <i className="pi pi-shield"></i>
                  <span className="text-xs font-bold uppercase tracking-wider text-cyan-500">{t('dashboard.citizen_verification')}</span>
                </div>
              </Card>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}
