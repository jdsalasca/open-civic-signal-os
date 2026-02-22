import { useEffect, useState, useCallback } from "react";
import { toast } from "react-hot-toast";
import { Signal, Notification, SignalMeta } from "../types";
import { MetricsGrid } from "../components/MetricsGrid";
import { SignalTable } from "../components/SignalTable";
import { DigestSidebar } from "../components/DigestSidebar";
import { NotificationSidebar } from "../components/NotificationSidebar";
import { CategoryChart } from "../components/CategoryChart";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../store/useAuthStore";
import { Layout } from "../components/Layout";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicSkeleton } from "../components/ui/CivicSkeleton";

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
  
  const [activeFilter, setActiveFilter] = useState<string>("ALL");
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
    if (!isoDate) return t('dashboard.freshness_pending');
    const date = new Date(isoDate);
    const diffMinutes = Math.max(0, Math.floor((Date.now() - date.getTime()) / 60000));
    if (diffMinutes < 1) return t('dashboard.freshness_now');
    if (diffMinutes < 60) return t('dashboard.freshness_minutes', { count: diffMinutes });
    return t('dashboard.freshness_hours', { count: Math.floor(diffMinutes / 60) });
  };

  const quickFilters = [
    { label: t('signals.filter_all'), value: "ALL", icon: "pi-list" },
    { label: t('signals.filter_critical'), value: "URGENT", icon: "pi-exclamation-triangle" },
    { label: t('signals.filter_pending'), value: "NEW", icon: "pi-clock" },
    { label: t('signals.filter_in_progress'), value: "IN_PROGRESS", icon: "pi-sync" },
    { label: t('signals.filter_resolved'), value: "RESOLVED", icon: "pi-check-circle" },
  ];

  return (
    <Layout>
      <div className="animate-fade-up">
        <section className="mb-8 flex flex-column lg:flex-row justify-content-between align-items-end gap-6" data-testid="dashboard-hero">
          <div>
            <div className="flex align-items-center gap-3 mb-4">
              <CivicBadge label="Operations Live" severity="new" />
              <div className="flex align-items-center gap-2 px-3 py-1 bg-white-alpha-5 border-round-lg border-1 border-white-alpha-10 text-xs font-bold text-secondary" data-testid="welcome-message">
                <i className="pi pi-user text-brand-primary"></i>
                {userName}
              </div>
              <div className="flex align-items-center gap-2 px-3 py-1 bg-white-alpha-5 border-round-lg border-1 border-white-alpha-10 text-xs font-bold text-secondary" data-testid="dashboard-freshness-badge">
                <i className="pi pi-clock text-brand-primary"></i>
                {formatLastUpdated(meta?.lastUpdatedAt ?? null)}
              </div>
            </div>
            <h1 className="text-6xl font-black m-0 tracking-tighter text-main line-height-1">
              {t('dashboard.title')}
            </h1>
            <p className="text-secondary text-xl mt-3 mb-0 font-medium max-w-30rem opacity-80">
              {t('dashboard.subtitle')}
            </p>
          </div>

          <div className="flex gap-3">
            {isStaff && (
              <CivicButton 
                label={t('dashboard.broadcast')}
                icon="pi pi-bolt" 
                variant="danger"
                className="py-4 px-6 shadow-xl"
                onClick={handleRelay}
              />
            )}
            <CivicButton 
              label={t('dashboard.new_issue')}
              icon="pi pi-plus" 
              glow
              className="py-4 px-6 shadow-xl"
              onClick={() => navigate("/report")}
            />
          </div>
        </section>

        <div className="mb-8">
          {loading ? (
            <CivicSkeleton type="metric" count={4} />
          ) : (
            <MetricsGrid signals={signals} />
          )}
        </div>

        <div className="grid">
          <div className="col-12 xl:col-9">
            <div className="flex align-items-center justify-content-between mb-4 px-2">
              <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-hidden">
                {quickFilters.map(f => (
                  <button
                    key={f.value}
                    type="button"
                    onClick={() => setActiveFilter(f.value)}
                    className={`flex align-items-center gap-2 px-4 py-2 border-round-xl border-1 transition-all cursor-pointer whitespace-nowrap font-bold text-xs uppercase tracking-widest
                      ${activeFilter === f.value 
                        ? 'bg-brand-primary border-brand-primary text-white shadow-lg' 
                        : 'bg-white-alpha-5 border-white-alpha-10 text-secondary hover:border-white-alpha-30'
                      }`}
                  >
                    <i className={`pi ${f.icon}`}></i>
                    {f.label}
                  </button>
                ))}
              </div>
            </div>

            {loading ? (
              <CivicSkeleton type="table-row" count={8} />
            ) : (
              <CivicCard padding="none" className="border-round-3xl">
                <SignalTable 
                  signals={signals} 
                  loading={loading} 
                  totalRecords={totalRecords}
                  rows={lazyState.rows}
                  first={lazyState.first}
                  onPage={onPage}
                />
              </CivicCard>
            )}
          </div>

          <div className="col-12 xl:col-3">
            <div className="flex flex-column gap-6">
              {loading ? (
                <CivicSkeleton type="text" count={3} />
              ) : (
                <>
                  {signals.length > 0 && (
                    <CategoryChart signals={signals} />
                  )}
                  {!isStaff && (
                    <CivicCard title={t('dashboard.quickstart_title')} variant="brand">
                      <p className="text-secondary text-sm m-0 mb-4">{t('dashboard.quickstart_desc')}</p>
                      <ul className="m-0 p-0 list-none flex flex-column gap-2 text-sm text-secondary">
                        <li className="line-height-3">1. {t('dashboard.quickstart_step_1')}</li>
                        <li className="line-height-3">2. {t('dashboard.quickstart_step_2')}</li>
                        <li className="line-height-3">3. {t('dashboard.quickstart_step_3')}</li>
                      </ul>
                      <div className="mt-4 flex flex-column gap-2">
                        <CivicButton label={t('dashboard.quickstart_report')} onClick={() => navigate('/report')} />
                        <CivicButton label={t('dashboard.quickstart_forums')} variant="secondary" onClick={() => navigate('/communities/threads')} />
                        <CivicButton label={t('dashboard.quickstart_contributions')} variant="ghost" onClick={() => navigate('/mine')} />
                      </div>
                    </CivicCard>
                  )}
                  <DigestSidebar signals={signals} />
                  {isStaff && (
                    <NotificationSidebar notifications={notifications} />
                  )}
                  {isStaff && (
                    <CivicCard title="Integrity Alerts" variant="danger">
                      <div className="flex flex-column gap-4">
                        <div className="flex align-items-center justify-content-between bg-status-rejected-alpha-10 p-3 border-round-xl">
                          <span className="text-sm font-bold text-main">Duplicates</span>
                          <span className="bg-status-rejected text-white px-2 py-1 border-round font-black text-xs">{duplicateClusters}</span>
                        </div>
                        <CivicButton 
                          label="Review Queue" 
                          variant="ghost" 
                          className="w-full text-xs" 
                          onClick={() => navigate('/moderation')}
                        />
                      </div>
                    </CivicCard>
                  )}
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
}
