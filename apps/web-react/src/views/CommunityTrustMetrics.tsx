import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { Layout } from "../components/Layout";
import { CivicActionBar } from "../components/ui/CivicActionBar";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";
import { CivicSelect } from "../components/ui/CivicSelect";
import { CivicStatCard } from "../components/ui/CivicStatCard";
import { useCommunityStore } from "../store/useCommunityStore";
import { useSettingsStore } from "../store/useSettingsStore";
import type { CommunityTrustMetrics, CommunityTrustMetricsPeriod, TrustMetricBreakdown } from "../types";

type ApiError = Error & { friendlyMessage?: string };

const PERIODS: CommunityTrustMetricsPeriod[] = ["LAST_7_DAYS", "LAST_30_DAYS", "LAST_90_DAYS"];

export function CommunityTrustMetrics() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const interfaceMode = useSettingsStore((state) => state.interfaceMode);
  const { activeCommunityId, memberships } = useCommunityStore();
  const activeMembership = memberships.find((membership) => membership.communityId === activeCommunityId) ?? memberships[0] ?? null;
  const selectedCommunityId = activeCommunityId ?? activeMembership?.communityId ?? null;
  const [period, setPeriod] = useState<CommunityTrustMetricsPeriod>("LAST_30_DAYS");
  const [metrics, setMetrics] = useState<CommunityTrustMetrics | null>(null);
  const [loading, setLoading] = useState(false);

  const periodOptions = useMemo(
    () => PERIODS.map((value) => ({ label: t(`community_trust.periods.${value}`), value })),
    [t]
  );

  const loadMetrics = useCallback(async () => {
    if (!selectedCommunityId) {
      setMetrics(null);
      return;
    }
    setLoading(true);
    try {
      const response = await apiClient.get<CommunityTrustMetrics>(`community/trust-metrics?communityId=${selectedCommunityId}&period=${period}`);
      setMetrics(response.data);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_trust.load_error"));
      setMetrics(null);
    } finally {
      setLoading(false);
    }
  }, [period, selectedCommunityId, t]);

  useEffect(() => {
    loadMetrics();
  }, [loadMetrics]);

  const renderBreakdown = (breakdown: TrustMetricBreakdown) => {
    const topItems = interfaceMode === "advanced" ? breakdown.items : breakdown.items.slice(0, 4);

    return (
      <CivicCard key={breakdown.key} title={breakdown.title} data-testid={`community-trust-breakdown-${breakdown.key}`}>
        <p className="text-sm text-secondary mt-0 mb-4">{breakdown.description}</p>
        {topItems.length === 0 ? (
          <CivicEmptyState
            icon="pi pi-chart-bar"
            title={t("community_trust.breakdown_empty_title")}
            description={t("community_trust.breakdown_empty_desc")}
          />
        ) : (
          <div className="flex flex-column gap-3">
            {topItems.map((item) => (
              <div className="u-surface-note" key={`${breakdown.key}-${item.label}`}>
                <div className="flex justify-content-between gap-3 align-items-center flex-wrap mb-2">
                  <div className="min-w-0 flex-1">
                    <div className="u-eyebrow mb-2">{item.label}</div>
                    <p className="text-sm text-secondary m-0">
                      {t("community_trust.share_copy", { count: item.value, share: item.share.toFixed(1) })}
                    </p>
                  </div>
                  <strong className="text-main text-lg font-black">{item.value}</strong>
                </div>
                <div className="w-full border-round-xl bg-surface-soft overflow-hidden" style={{ height: "10px" }}>
                  <div
                    className="border-round-xl"
                    style={{
                      width: `${Math.min(100, Math.max(item.share, item.value > 0 ? 6 : 0))}%`,
                      height: "10px",
                      background: "linear-gradient(135deg, var(--brand-primary) 0%, var(--brand-secondary) 100%)",
                    }}
                    aria-hidden="true"
                  />
                </div>
              </div>
            ))}
          </div>
        )}
      </CivicCard>
    );
  };

  const formatAbsoluteDate = (value?: string | null) => {
    if (!value) {
      return t("community_trust.not_available");
    }
    return new Date(value).toLocaleString();
  };

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <div className="flex flex-column md:flex-row justify-content-between align-items-start gap-4 mb-8">
          <CivicPageHeader
            title={t("community_trust.title")}
            description={t("community_trust.desc", {
              community: activeMembership?.communityName ?? t("dashboard.community_default"),
            })}
            className="mb-0"
          />
          <CivicActionBar className="community-home-action-bar">
            <div className="community-home-action-copy">
              <div className="u-eyebrow">{t("community_trust.filters.kicker")}</div>
              <p className="u-section-copy text-sm m-0">{t("community_trust.filters.desc")}</p>
            </div>
            <div className="dashboard-action-cluster">
              <div className="min-w-12rem">
                <CivicSelect
                  value={period}
                  onChange={(e) => setPeriod(e.value)}
                  options={periodOptions}
                  className="w-full"
                  data-testid="community-trust-period-filter"
                />
              </div>
              <CivicButton type="button" icon="pi pi-refresh" label={t("community_trust.filters.refresh")} variant="secondary" onClick={loadMetrics} />
              <CivicButton type="button" icon="pi pi-sitemap" label={t("nav.community_decisions")} variant="ghost" onClick={() => navigate("/communities/decisions")} />
              <CivicButton type="button" icon="pi pi-briefcase" label={t("nav.community_projects")} variant="ghost" onClick={() => navigate("/communities/projects")} />
            </div>
          </CivicActionBar>
        </div>

        {!selectedCommunityId ? (
          <CivicCard>
            <CivicEmptyState
              icon="pi pi-globe"
              title={t("community_trust.no_context_title")}
              description={t("community_trust.no_context_desc")}
              actionLabel={t("nav.communities")}
              onAction={() => navigate("/communities")}
            />
          </CivicCard>
        ) : loading && !metrics ? (
          <CivicCard>
            <p className="text-secondary m-0">{t("common.loading")}</p>
          </CivicCard>
        ) : metrics ? (
          <div className="flex flex-column gap-6">
            <CivicCard data-testid="community-trust-overview">
              <div className="flex flex-column lg:flex-row justify-content-between align-items-start gap-4">
                <div className="min-w-0 flex-1">
                  <div className="u-eyebrow mb-2">{t("community_trust.kicker")}</div>
                  <h2 className="text-3xl font-black text-main mt-0 mb-3">{metrics.communityName}</h2>
                  <p className="text-secondary m-0 line-height-3">
                    {t("community_trust.overview_copy", {
                      start: metrics.startDate,
                      end: metrics.endDate,
                    })}
                  </p>
                </div>
                <div className="flex flex-column gap-2 min-w-0">
                  <div className="u-pill" data-testid="community-trust-freshness">
                    <i className="pi pi-clock text-brand-primary"></i>
                    {metrics.freshness}
                  </div>
                  <div className="u-surface-note">
                    <div className="u-eyebrow mb-2">{t("community_trust.last_updated_label")}</div>
                    <p className="text-sm text-secondary m-0">{formatAbsoluteDate(metrics.lastUpdatedAt)}</p>
                  </div>
                </div>
              </div>
              {metrics.lowData && (
                <div className="u-surface-note mt-4" data-testid="community-trust-low-data">
                  <div className="u-eyebrow mb-2">{t("community_trust.low_data_title")}</div>
                  <p className="text-sm text-secondary m-0 line-height-3">{metrics.lowDataReason ?? t("community_trust.low_data_fallback")}</p>
                </div>
              )}
            </CivicCard>

            <div className="civic-stat-grid civic-stat-grid-comfortable" data-testid="community-trust-cards">
              {metrics.cards.map((card) => (
                <CivicCard key={card.key} data-testid={`community-trust-card-${card.key}`}>
                  <CivicStatCard label={card.label} value={card.value} supportingText={card.definition} compact />
                  <div className="u-surface-note mt-3">
                    <div className="u-eyebrow mb-2">{t("community_trust.formula_label")}</div>
                    <p className="text-sm text-secondary m-0 line-height-3">{interfaceMode === "advanced" ? card.formula : card.supportingText}</p>
                  </div>
                </CivicCard>
              ))}
            </div>

            <div className="grid">
              {metrics.breakdowns.map((breakdown) => (
                <div className="col-12 xl:col-6" key={breakdown.key}>
                  {renderBreakdown(breakdown)}
                </div>
              ))}
            </div>
          </div>
        ) : (
          <CivicCard>
            <CivicEmptyState
              icon="pi pi-chart-line"
              title={t("community_trust.empty_title")}
              description={t("community_trust.empty_desc")}
              actionLabel={t("community_trust.filters.refresh")}
              onAction={loadMetrics}
            />
          </CivicCard>
        )}
      </div>
    </Layout>
  );
}
