import { useEffect, useState, useCallback, useMemo } from "react";
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
import { CivicSkeleton } from "../components/ui/CivicSkeleton";
import { CivicToolbar } from "../components/ui/CivicToolbar";
import { CivicActionBar } from "../components/ui/CivicActionBar";
import { useCommunityStore } from "../store/useCommunityStore";

interface ApiError extends Error {
  friendlyMessage?: string;
}

const CRITICAL_SCORE_THRESHOLD = 220;
const STATUS_FILTERS = new Set(["NEW", "IN_PROGRESS", "RESOLVED"]);
const DASHBOARD_CACHE_TTL_MS = 60 * 1000;

type DashboardCachePayload = {
  signals: Signal[];
  totalRecords: number;
  meta: SignalMeta | null;
  notifications: Notification[];
  duplicateClusters: number;
};

const dashboardCache = new Map<string, { timestamp: number; data: DashboardCachePayload }>();

export function Dashboard() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { activeRole, userName } = useAuthStore();
  const { activeCommunityId, memberships } = useCommunityStore();
  
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

  const loadData = useCallback(async (signal?: AbortSignal, force = false) => {
    const cacheKey = [
      activeCommunityId || "global",
      activeRole,
      activeFilter,
      lazyState.page,
      lazyState.rows,
    ].join(":");

    if (!force) {
      const cached = dashboardCache.get(cacheKey);
      const isFresh = cached && Date.now() - cached.timestamp < DASHBOARD_CACHE_TTL_MS;
      if (isFresh && cached) {
        setSignals(cached.data.signals);
        setTotalRecords(cached.data.totalRecords);
        setMeta(cached.data.meta);
        setNotifications(cached.data.notifications);
        setDuplicateClusters(cached.data.duplicateClusters);
        setLoading(false);
        return;
      }
    }

    try {
      setLoading(true);
      const prioritizedQuery = STATUS_FILTERS.has(activeFilter)
        ? `signals/prioritized?page=${lazyState.page}&size=${lazyState.rows}&status=${activeFilter}`
        : `signals/prioritized?page=${lazyState.page}&size=${lazyState.rows}`;
      const [signalsRes, metaRes, notificationsRes, duplicatesRes] = await Promise.all([
        apiClient.get(prioritizedQuery, { signal }),
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

      dashboardCache.set(cacheKey, {
        timestamp: Date.now(),
        data: {
          signals: signalsRes.data.content || [],
          totalRecords: signalsRes.data.totalElements || 0,
          meta: metaRes.status === 200 ? metaRes.data : null,
          notifications: notificationsRes && notificationsRes.status === 200 ? notificationsRes.data : [],
          duplicateClusters:
            duplicatesRes && duplicatesRes.status === 200
              ? Object.keys(duplicatesRes.data || {}).length
              : 0,
        },
      });
      
    } catch (err: any) {
      if (err.name === 'CanceledError' || err.name === 'AbortError') return;
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('dashboard.sync_error'));
    } finally {
      setLoading(false);
    }
  }, [activeRole, t, lazyState, activeFilter, activeCommunityId]);

  useEffect(() => {
    const controller = new AbortController();
    loadData(controller.signal);
    return () => controller.abort();
  }, [loadData]);

  const onPage = (event: any) => {
    setLazyState(event);
  };

  const handleFilterChange = (value: string) => {
    setActiveFilter(value);
    setLazyState((prev) => ({ ...prev, first: 0, page: 0 }));
  };

  const displayedSignals = useMemo(() => {
    if (activeFilter === "CRITICAL") {
      return signals.filter((s) => (s.priorityScore ?? 0) >= CRITICAL_SCORE_THRESHOLD);
    }
    return signals;
  }, [signals, activeFilter]);

  const visibleRecords = activeFilter === "CRITICAL" ? displayedSignals.length : totalRecords;
  const criticalCount = useMemo(
    () => signals.filter((s) => (s.priorityScore ?? 0) >= CRITICAL_SCORE_THRESHOLD).length,
    [signals]
  );
  const newCount = useMemo(
    () => signals.filter((s) => s.status === "NEW").length,
    [signals]
  );

  const handleRelay = async () => {
    try {
      const res = await apiClient.post("notifications/relay/top-10");
      if (res.status === 200) {
        toast.success(t('dashboard.broadcast_success'));
        loadData(undefined, true);
      }
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('dashboard.relay_rejected'));
    }
  };

  const isStaff = activeRole === "PUBLIC_SERVANT" || activeRole === "SUPER_ADMIN";
  const activeMembership = useMemo(
    () =>
      memberships.find((membership) => membership.communityId === activeCommunityId) ??
      memberships[0] ??
      null,
    [activeCommunityId, memberships]
  );
  const isCommunityModerator =
    activeMembership?.role === "MODERATOR" || activeMembership?.role === "COORDINATOR";

  const guidedHome = useMemo(() => {
    if (isStaff) {
      return {
        personaLabel: t("dashboard.guided_home.public_servant.persona"),
        heroTitle: t("dashboard.guided_home.public_servant.hero_title"),
        heroSubtitle: t("dashboard.guided_home.public_servant.hero_subtitle"),
        heroGuidance: t("dashboard.guided_home.public_servant.hero_guidance"),
        primaryActionLabel: t("dashboard.guided_home.public_servant.primary_action"),
        primaryActionIcon: "pi pi-megaphone",
        primaryAction: () => navigate("/communities/blog"),
        cardTitle: t("dashboard.guided_home.public_servant.card_title"),
        cardDescription: t("dashboard.guided_home.public_servant.card_description"),
        steps: [
          t("dashboard.guided_home.public_servant.step_1"),
          t("dashboard.guided_home.public_servant.step_2"),
          t("dashboard.guided_home.public_servant.step_3"),
        ],
      };
    }

    if (isCommunityModerator) {
      return {
        personaLabel: t("dashboard.guided_home.moderator.persona"),
        heroTitle: t("dashboard.guided_home.moderator.hero_title"),
        heroSubtitle: t("dashboard.guided_home.moderator.hero_subtitle"),
        heroGuidance: t("dashboard.guided_home.moderator.hero_guidance", {
          community: activeMembership?.communityName ?? t("dashboard.community_default"),
        }),
        primaryActionLabel: t("dashboard.guided_home.moderator.primary_action"),
        primaryActionIcon: "pi pi-comments",
        primaryAction: () => navigate("/communities/threads"),
        cardTitle: t("dashboard.guided_home.moderator.card_title"),
        cardDescription: t("dashboard.guided_home.moderator.card_description", {
          community: activeMembership?.communityName ?? t("dashboard.community_default"),
        }),
        steps: [
          t("dashboard.guided_home.moderator.step_1"),
          t("dashboard.guided_home.moderator.step_2"),
          t("dashboard.guided_home.moderator.step_3"),
        ],
      };
    }

    return {
      personaLabel: t("dashboard.guided_home.citizen.persona"),
      heroTitle: t("dashboard.guided_home.citizen.hero_title"),
      heroSubtitle: t("dashboard.guided_home.citizen.hero_subtitle"),
      heroGuidance: t("dashboard.guided_home.citizen.hero_guidance"),
      primaryActionLabel: t("dashboard.guided_home.citizen.primary_action"),
      primaryActionIcon: "pi pi-plus",
      primaryAction: () => navigate("/report"),
      cardTitle: t("dashboard.guided_home.citizen.card_title"),
      cardDescription: t("dashboard.guided_home.citizen.card_description"),
      steps: [
        t("dashboard.guided_home.citizen.step_1"),
        t("dashboard.guided_home.citizen.step_2"),
        t("dashboard.guided_home.citizen.step_3"),
      ],
    };
  }, [activeMembership?.communityName, isCommunityModerator, isStaff, navigate, t]);

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
    { label: t('signals.filter_critical'), value: "CRITICAL", icon: "pi-exclamation-triangle" },
    { label: t('signals.filter_pending'), value: "NEW", icon: "pi-clock" },
    { label: t('signals.filter_in_progress'), value: "IN_PROGRESS", icon: "pi-sync" },
    { label: t('signals.filter_resolved'), value: "RESOLVED", icon: "pi-check-circle" },
  ];

  const handleMetricSelect = (metricId: "total" | "new" | "analysis" | "avg") => {
    const filterByMetric: Record<typeof metricId, string> = {
      total: "ALL",
      new: "NEW",
      analysis: "IN_PROGRESS",
      avg: "ALL",
    };
    handleFilterChange(filterByMetric[metricId]);
  };

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <section className="mb-8 flex flex-column lg:flex-row justify-content-between align-items-end gap-6" data-testid="dashboard-hero">
          <div>
            <div className="flex align-items-center flex-wrap gap-3 mb-4">
              <div className="u-pill" data-testid="welcome-message">
                <i className="pi pi-user text-brand-primary"></i>
                {userName}
              </div>
              <div className="u-pill" data-testid="dashboard-guided-persona">
                <i className="pi pi-compass text-brand-primary"></i>
                {guidedHome.personaLabel}
              </div>
              <div className="u-pill" data-testid="dashboard-freshness-badge">
                <i className="pi pi-clock text-brand-primary"></i>
                {formatLastUpdated(meta?.lastUpdatedAt ?? null)}
              </div>
            </div>
            <h1 className="text-4xl md:text-5xl font-black m-0 tracking-tight text-main line-height-2">
              {guidedHome.heroTitle}
            </h1>
            <p className="text-secondary text-lg mt-3 mb-0 font-medium max-w-30rem">
              {guidedHome.heroSubtitle}
            </p>
            <p className="text-sm text-muted mt-3 mb-0 max-w-28rem" data-testid="dashboard-primary-guidance">
              {guidedHome.heroGuidance}
            </p>
          </div>

          <CivicActionBar className="dashboard-hero-actions">
            <CivicButton
              type="button"
              label={guidedHome.primaryActionLabel}
              icon={guidedHome.primaryActionIcon}
              onClick={guidedHome.primaryAction}
              className="shadow-xl"
              data-testid="dashboard-action-report"
            />
          </CivicActionBar>
        </section>

        <CivicCard className="mb-6" data-testid="dashboard-secondary-actions">
          <div className="flex flex-column lg:flex-row lg:align-items-center justify-content-between gap-3">
            <div className="flex flex-column gap-1">
              <span className="text-xs font-black uppercase tracking-widest text-muted">
                {t("dashboard.secondary_actions_title")}
              </span>
              <span className="text-sm text-secondary">
                {t("dashboard.secondary_actions_desc")}
              </span>
            </div>
            <div className="flex flex-wrap gap-2">
              <CivicButton
                type="button"
                label={t('nav.dialogues')}
                icon="pi pi-comments"
                variant="ghost"
                onClick={() => navigate("/communities/threads")}
                data-testid="dashboard-action-threads"
              />
              <CivicButton
                type="button"
                label={t('nav.public_blog')}
                icon="pi pi-megaphone"
                variant="ghost"
                onClick={() => navigate("/communities/blog")}
                data-testid="dashboard-action-blog"
              />
              <CivicButton
                type="button"
                label={t('dashboard.track_contributions')}
                icon="pi pi-user"
                variant="ghost"
                onClick={() => navigate("/mine")}
                data-testid="dashboard-action-mine"
              />
              {isStaff && (
                <CivicButton
                  type="button"
                  label={t('dashboard.broadcast')}
                  icon="pi pi-bolt"
                  variant="ghost"
                  onClick={handleRelay}
                  data-testid="dashboard-action-broadcast"
                />
              )}
            </div>
          </div>
        </CivicCard>

        <CivicCard className="mb-6" variant="brand">
          <div className="flex flex-column lg:flex-row lg:align-items-center justify-content-between gap-3">
            <div className="flex flex-column gap-1">
              <span className="text-xs font-black uppercase tracking-widest text-muted">{t("dashboard.priority_strip_title")}</span>
              <span className="text-sm text-secondary">{t("dashboard.priority_strip_desc")}</span>
            </div>
            <div className="flex flex-wrap gap-2">
              <CivicButton
                type="button"
                size="small"
                variant={activeFilter === "CRITICAL" ? "primary" : "ghost"}
                label={t("dashboard.priority_strip_critical", { count: criticalCount })}
                icon="pi pi-exclamation-triangle"
                onClick={() => handleFilterChange("CRITICAL")}
                data-testid="dashboard-priority-critical"
              />
              <CivicButton
                type="button"
                size="small"
                variant={activeFilter === "NEW" ? "primary" : "ghost"}
                label={t("dashboard.priority_strip_new", { count: newCount })}
                icon="pi pi-clock"
                onClick={() => handleFilterChange("NEW")}
                data-testid="dashboard-priority-new"
              />
              {isStaff && (
                <CivicButton
                  type="button"
                  size="small"
                  variant="ghost"
                  label={t("dashboard.priority_strip_review_queue")}
                  icon="pi pi-shield"
                  onClick={() => navigate("/moderation")}
                  data-testid="dashboard-priority-moderation"
                />
              )}
            </div>
          </div>
        </CivicCard>

        <div className="mb-8">
          {loading ? (
            <CivicSkeleton type="metric" count={4} />
          ) : (
            <MetricsGrid signals={displayedSignals} onMetricSelect={handleMetricSelect} />
          )}
        </div>

        <div className="grid">
          <div className="col-12 xl:col-9">
            <div className="flex align-items-center justify-content-between mb-4 px-2">
              <CivicToolbar className="overflow-x-auto pb-2 scrollbar-hidden">
                {quickFilters.map(f => (
                  <button
                    key={f.value}
                    type="button"
                    onClick={() => handleFilterChange(f.value)}
                    data-testid={`dashboard-filter-${f.value.toLowerCase()}`}
                    className={`u-filter-chip cursor-pointer whitespace-nowrap ${activeFilter === f.value ? 'is-active' : ''}`}
                  >
                    <i className={`pi ${f.icon}`}></i>
                    {f.label}
                  </button>
                ))}
              </CivicToolbar>
            </div>

            {loading ? (
              <CivicSkeleton type="table-row" count={8} />
            ) : (
              <CivicCard padding="none" className="border-round-3xl">
                <SignalTable 
                  signals={displayedSignals} 
                  loading={loading} 
                  totalRecords={visibleRecords}
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
                  {displayedSignals.length > 0 && (
                    <CategoryChart signals={displayedSignals} />
                  )}
                  <CivicCard title={guidedHome.cardTitle} variant="brand" data-testid="dashboard-guided-home-card">
                    <p className="text-secondary text-sm m-0 mb-4" data-testid="dashboard-guided-home-description">
                      {guidedHome.cardDescription}
                    </p>
                    <ul className="m-0 p-0 list-none flex flex-column gap-2 text-sm text-secondary">
                      {guidedHome.steps.map((step, index) => (
                        <li
                          key={step}
                          className="line-height-3"
                          data-testid={`dashboard-guided-step-${index + 1}`}
                        >
                          {index + 1}. {step}
                        </li>
                      ))}
                    </ul>
                  </CivicCard>
                  {!isStaff && (
                    <CivicCard title={t('dashboard.quickstart_title')} variant="brand">
                      <p className="text-secondary text-sm m-0 mb-4">{t('dashboard.quickstart_desc')}</p>
                      <ul className="m-0 p-0 list-none flex flex-column gap-2 text-sm text-secondary">
                        <li className="line-height-3">1. {t('dashboard.quickstart_step_1')}</li>
                        <li className="line-height-3">2. {t('dashboard.quickstart_step_2')}</li>
                        <li className="line-height-3">3. {t('dashboard.quickstart_step_3')}</li>
                      </ul>
                      <div className="mt-4 flex flex-column md:flex-row gap-2">
                        <CivicButton label={t('dashboard.quickstart_report')} onClick={() => navigate('/report')} />
                        <CivicButton label={t('dashboard.quickstart_forums')} variant="secondary" onClick={() => navigate('/communities/threads')} />
                        <CivicButton label={t('dashboard.quickstart_contributions')} variant="ghost" onClick={() => navigate('/mine')} className="md:ml-auto" />
                      </div>
                    </CivicCard>
                  )}
                  <DigestSidebar signals={displayedSignals} />
                  {isStaff && notifications.length > 0 && (
                    <NotificationSidebar notifications={notifications} />
                  )}
                  {isStaff && (
                    <CivicCard title="Integrity Alerts" variant="danger">
                      <div className="flex flex-column gap-4">
                        <div className="flex align-items-center justify-content-between bg-status-rejected-alpha-10 p-3 border-round-xl">
                          <span className="text-sm font-bold text-main">Duplicates</span>
                          <span className="bg-status-rejected text-on-brand px-2 py-1 border-round font-black text-xs">{duplicateClusters}</span>
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
