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
import { CivicStatCard } from "../components/ui/CivicStatCard";
import { useCommunityStore } from "../store/useCommunityStore";
import { useSettingsStore } from "../store/useSettingsStore";
import { toRoleLabel } from "../constants/roleLabels";

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
  const interfaceMode = useSettingsStore((state) => state.interfaceMode);
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
  const formatLastUpdated = (isoDate: string | null) => {
    if (!isoDate) return t('dashboard.freshness_pending');
    const date = new Date(isoDate);
    const diffMinutes = Math.max(0, Math.floor((Date.now() - date.getTime()) / 60000));
    if (diffMinutes < 1) return t('dashboard.freshness_now');
    if (diffMinutes < 60) return t('dashboard.freshness_minutes', { count: diffMinutes });
    return t('dashboard.freshness_hours', { count: Math.floor(diffMinutes / 60) });
  };
  const heroStats = useMemo(
    () => [
      {
        label: t("dashboard.priority_strip_critical", { count: criticalCount }),
        value: criticalCount.toString(),
      },
      {
        label: t("dashboard.priority_strip_new", { count: newCount }),
        value: newCount.toString(),
      },
      {
        label: t("dashboard.freshness_label"),
        value: formatLastUpdated(meta?.lastUpdatedAt ?? null),
      },
    ],
    [criticalCount, meta?.lastUpdatedAt, newCount, t]
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
  const activeBreadcrumb = activeMembership?.breadcrumb ?? [];
  const isCommunityModerator =
    activeMembership?.role === "MODERATOR" || activeMembership?.role === "COORDINATOR";
  const activeCommunityName = activeMembership?.communityName ?? t("dashboard.community_default");
  const activeCommunityRoleLabel = activeMembership ? toRoleLabel(activeMembership.role, t) : null;
  const activeCommunityPath =
    activeBreadcrumb.length > 0
      ? activeBreadcrumb.map((item) => item.name).join(" / ")
      : t("dashboard.community_path_empty");

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

  const communityActionCards = useMemo(
    () => [
      {
        title: t("dashboard.community_surface.official_title"),
        description: t("dashboard.community_surface.official_desc", { community: activeCommunityName }),
        icon: "pi pi-megaphone",
        actionLabel: t("dashboard.community_surface.official_action"),
        action: () => navigate("/communities/blog"),
        testId: "dashboard-community-action-blog",
      },
      {
        title: t("dashboard.community_surface.talks_title"),
        description: t("dashboard.community_surface.talks_desc", { community: activeCommunityName }),
        icon: "pi pi-comments",
        actionLabel: t("dashboard.community_surface.talks_action"),
        action: () => navigate("/communities/threads"),
        testId: "dashboard-community-action-threads",
      },
      {
        title: t("dashboard.community_surface.membership_title"),
        description: t("dashboard.community_surface.membership_desc", {
          count: memberships.length,
          role: activeCommunityRoleLabel ?? t("dashboard.community_surface.membership_no_role"),
        }),
        icon: "pi pi-users",
        actionLabel: t("dashboard.community_surface.membership_action"),
        action: () => navigate("/communities"),
        testId: "dashboard-community-action-hub",
      },
    ],
    [activeCommunityName, activeCommunityRoleLabel, memberships.length, navigate, t]
  );

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <section className="dashboard-hero-shell mb-8" data-testid="dashboard-hero">
          <CivicCard className="dashboard-story-card" padding="lg">
            <div className="flex align-items-center flex-wrap gap-3 mb-4">
              <div className="u-pill" data-testid="welcome-message">
                <i className="pi pi-user text-brand-primary"></i>
                {userName}
              </div>
              <div className="u-pill" data-testid="dashboard-guided-persona">
                <i className="pi pi-compass text-brand-primary"></i>
                {guidedHome.personaLabel}
              </div>
              <div className="u-pill" data-testid="dashboard-active-community-pill">
                <i className="pi pi-globe text-brand-primary"></i>
                {activeCommunityName}
              </div>
              <div className="u-pill" data-testid="dashboard-freshness-badge">
                <i className="pi pi-clock text-brand-primary"></i>
                {formatLastUpdated(meta?.lastUpdatedAt ?? null)}
              </div>
            </div>
            <h1 className="u-page-title text-4xl md:text-5xl font-black m-0 line-height-2">
              {guidedHome.heroTitle}
            </h1>
            <p className="text-secondary text-lg mt-3 mb-0 font-medium max-w-30rem">
              {guidedHome.heroSubtitle}
            </p>
            <p className="text-sm text-muted mt-3 mb-0 max-w-28rem" data-testid="dashboard-primary-guidance">
              {guidedHome.heroGuidance}
            </p>
            <div className="dashboard-story-grid mt-5">
              {heroStats.map((item) => (
                <CivicStatCard
                  key={item.label}
                  label={item.label}
                  value={item.value}
                  compact
                  tone="brand"
                />
              ))}
            </div>
            <CivicActionBar className="dashboard-hero-actions mt-5">
              <CivicButton
                type="button"
                label={guidedHome.primaryActionLabel}
                icon={guidedHome.primaryActionIcon}
                onClick={guidedHome.primaryAction}
                className="shadow-xl"
                data-testid="dashboard-action-report"
              />
            </CivicActionBar>
          </CivicCard>
          <CivicCard variant="brand" padding="lg" data-testid="dashboard-community-hub-card">
            <span className="u-section-title text-xs">{t("dashboard.community_hub.title")}</span>
            <h2 className="text-xl font-black text-main mt-3 mb-0">{activeCommunityName}</h2>
            <p className="text-secondary text-sm mt-3 mb-0" data-testid="dashboard-guided-home-description">
              {t("dashboard.community_hub.desc", {
                community: activeCommunityName,
                role: activeCommunityRoleLabel ?? t("dashboard.community_hub.role_fallback"),
              })}
            </p>
            <div className="dashboard-community-summary-grid mt-4" data-testid="dashboard-community-summary">
              <CivicStatCard
                label={t("dashboard.community_hub.path_label")}
                value={activeCommunityName}
                supportingText={activeCommunityPath}
                compact
                data-testid="dashboard-community-summary-path"
              />
              <CivicStatCard
                label={t("dashboard.community_hub.role_label")}
                value={activeCommunityRoleLabel ?? t("dashboard.community_hub.role_fallback")}
                supportingText={guidedHome.personaLabel}
                compact
                data-testid="dashboard-community-summary-role"
              />
              <CivicStatCard
                label={t("dashboard.community_hub.memberships_label")}
                value={memberships.length}
                supportingText={t("settings.community_membership_title")}
                compact
                data-testid="dashboard-community-summary-memberships"
              />
            </div>
            <div className="mt-5 pt-4 border-top-1 border-white-alpha-10">
              <span className="text-xs font-black uppercase tracking-widest text-muted">
                {t("dashboard.community_hub.next_actions_label")}
              </span>
              <div className="dashboard-action-cluster mt-3">
                <CivicButton
                  type="button"
                  label={t("dashboard.community_hub.action_hub")}
                  icon="pi pi-globe"
                  variant="ghost"
                  onClick={() => navigate("/communities")}
                  data-testid="dashboard-community-open-hub"
                />
                <CivicButton
                  type="button"
                  label={t("dashboard.track_contributions")}
                  icon="pi pi-user"
                  variant="ghost"
                  onClick={() => navigate("/mine")}
                  data-testid="dashboard-hero-action-mine"
                />
              </div>
            </div>
          </CivicCard>
        </section>

        <CivicCard className="mb-6" data-testid="dashboard-community-focus">
          <div className="flex flex-column lg:flex-row lg:align-items-end justify-content-between gap-3 mb-4">
            <div className="flex flex-column gap-1">
              <span className="text-xs font-black uppercase tracking-widest text-muted">
                {t("dashboard.community_surface.kicker")}
              </span>
              <h2 className="text-2xl font-black text-main m-0">{t("dashboard.community_surface.title", { community: activeCommunityName })}</h2>
              <p className="text-sm text-secondary m-0">{t("dashboard.community_surface.desc")}</p>
            </div>
            {interfaceMode === "simple" && (
              <div className="u-pill" data-testid="dashboard-community-mode-hint">
                <i className="pi pi-sparkles text-brand-primary"></i>
                {t("dashboard.community_surface.simple_mode_hint")}
              </div>
            )}
          </div>
          <div className="dashboard-community-surface-grid">
            {communityActionCards.map((card) => (
              <div key={card.title} className="dashboard-community-surface-card" data-testid={card.testId}>
                <div className="flex align-items-start justify-content-between gap-3">
                  <div className="flex-1">
                    <div className="dashboard-story-metric-label">{card.title}</div>
                    <p className="text-sm text-secondary mt-3 mb-0 line-height-3">{card.description}</p>
                  </div>
                  <div className="u-pill">
                    <i className={`${card.icon} text-brand-primary`}></i>
                  </div>
                </div>
                <div className="mt-4">
                  <CivicButton
                    type="button"
                    label={card.actionLabel}
                    icon="pi pi-arrow-right"
                    variant="ghost"
                    onClick={card.action}
                  />
                </div>
              </div>
            ))}
          </div>
        </CivicCard>

        {interfaceMode === "advanced" && (
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
              <CivicActionBar className="dashboard-secondary-action-bar">
                <div className="dashboard-action-cluster">
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
                    label={t('nav.dialogues')}
                    icon="pi pi-comments"
                    variant="ghost"
                    onClick={() => navigate("/communities/threads")}
                    data-testid="dashboard-action-threads"
                  />
                  <CivicButton
                    type="button"
                    label={t('dashboard.track_contributions')}
                    icon="pi pi-user"
                    variant="ghost"
                    onClick={() => navigate("/mine")}
                    data-testid="dashboard-action-mine"
                  />
                </div>
                {isStaff && (
                  <CivicButton
                    type="button"
                    label={t('dashboard.broadcast')}
                    icon="pi pi-bolt"
                    variant="secondary"
                    onClick={handleRelay}
                    data-testid="dashboard-action-broadcast"
                  />
                )}
              </CivicActionBar>
            </div>
          </CivicCard>
        )}

        <CivicCard className="mb-6" variant="brand">
          <div className="flex flex-column lg:flex-row lg:align-items-center justify-content-between gap-3">
            <div className="flex flex-column gap-1">
                <span className="text-xs font-black uppercase tracking-widest text-muted">{t("dashboard.priority_strip_title")}</span>
                <span className="text-sm text-secondary">{t("dashboard.priority_strip_desc")}</span>
              </div>
            <div className="dashboard-action-cluster">
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
                  {interfaceMode === "advanced" && displayedSignals.length > 0 && (
                    <CategoryChart signals={displayedSignals} />
                  )}
                  {!isStaff && (
                    <CivicCard title={t('dashboard.quickstart_title')} variant="brand">
                      <p className="text-secondary text-sm m-0 mb-4">{t('dashboard.quickstart_desc')}</p>
                      <ul className="m-0 p-0 list-none flex flex-column gap-2 text-sm text-secondary">
                        <li className="line-height-3">1. {t('dashboard.quickstart_step_1')}</li>
                        <li className="line-height-3">2. {t('dashboard.quickstart_step_2')}</li>
                        <li className="line-height-3">3. {t('dashboard.quickstart_step_3')}</li>
                      </ul>
                      <CivicActionBar className="dashboard-quickstart-actions mt-4">
                        <CivicButton label={t('dashboard.quickstart_report')} onClick={() => navigate('/report')} />
                        <CivicButton label={t('dashboard.quickstart_forums')} variant="secondary" onClick={() => navigate('/communities/threads')} />
                        <CivicButton label={t('dashboard.quickstart_contributions')} variant="ghost" onClick={() => navigate('/mine')} />
                      </CivicActionBar>
                    </CivicCard>
                  )}
                  <DigestSidebar signals={displayedSignals} />
                  {isStaff && notifications.length > 0 && (
                    <NotificationSidebar notifications={notifications} />
                  )}
                  {isStaff && interfaceMode === "advanced" && (
                    <CivicCard title={t("dashboard.integrity_title")} variant="danger">
                      <div className="flex flex-column gap-4">
                        <CivicStatCard
                          label={t("dashboard.integrity_duplicates")}
                          value={duplicateClusters}
                          supportingText={t("dashboard.duplicates_desc", { count: duplicateClusters })}
                          compact
                        />
                        <CivicButton 
                          label={t("dashboard.integrity_review")} 
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
