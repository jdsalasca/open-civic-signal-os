import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { useTranslation } from "react-i18next";
import { CommunityHome } from "../types";
import { Layout } from "../components/Layout";
import { useCommunityStore } from "../store/useCommunityStore";
import apiClient from "../api/axios";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";
import { CivicActionBar } from "../components/ui/CivicActionBar";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicStatCard } from "../components/ui/CivicStatCard";

type ApiError = Error & { friendlyMessage?: string };

export function CommunityFeed() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { activeCommunityId, memberships } = useCommunityStore();
  const [home, setHome] = useState<CommunityHome | null>(null);

  const activeCommunityName = memberships.find((m) => m.communityId === activeCommunityId)?.communityName;

  const loadHome = useCallback(async () => {
    if (!activeCommunityId) return;
    try {
      const res = await apiClient.get<CommunityHome>(`community/home?communityId=${activeCommunityId}`);
      setHome(res.data);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_home.load_error"));
    }
  }, [activeCommunityId, t]);

  useEffect(() => {
    loadHome();
  }, [loadHome]);

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <div className="flex flex-column md:flex-row justify-content-between align-items-start md:align-items-center mb-8 gap-4">
          <CivicPageHeader
            title={t("community_home.title", { community: activeCommunityName || t("dashboard.community_default") })}
            description={t("community_home.desc")}
            className="mb-0"
          />

          <CivicActionBar>
            <div className="u-pill" data-testid="community-home-freshness">
              <i className="pi pi-clock text-brand-primary"></i>
              {home?.freshness || t("dashboard.freshness_pending")}
            </div>
            <CivicButton
              type="button"
              icon="pi pi-map"
              label={t("community_map.open_map")}
              variant="secondary"
              onClick={() => navigate("/communities/map")}
            />
            <CivicButton
              type="button"
              icon="pi pi-comments"
              label={t("nav.dialogues")}
              variant="ghost"
              onClick={() => navigate("/communities/threads")}
            />
            <CivicButton
              type="button"
              icon="pi pi-megaphone"
              label={t("nav.public_blog")}
              variant="ghost"
              onClick={() => navigate("/communities/blog")}
            />
          </CivicActionBar>
        </div>

        {!activeCommunityId ? (
          <CivicCard>
            <CivicEmptyState
              icon="pi-map-marker"
              title={t("community_home.no_context_title")}
              description={t("community_home.no_context_desc")}
              actionLabel={t("nav.communities")}
              onAction={() => navigate("/communities")}
            />
          </CivicCard>
        ) : (
          <div className="grid">
            <div className="col-12 xl:col-8">
              <CivicCard className="mb-6" data-testid="community-home-overview">
                <div className="flex flex-column lg:flex-row justify-content-between gap-4">
                  <div className="flex flex-column gap-2">
                    <span className="text-xs font-black uppercase tracking-widest text-muted">
                      {t("community_home.kicker")}
                    </span>
                    <h2 className="text-3xl font-black text-main m-0">{activeCommunityName}</h2>
                    <p className="text-secondary m-0">{t("community_home.overview_desc")}</p>
                  </div>
                </div>
                <div className="civic-stat-grid mt-4" data-testid="community-home-overview-stats">
                  <CivicStatCard
                    label={t("community_home.rooms_count", { count: home?.activeRoomsCount ?? 0 })}
                    value={home?.activeRoomsCount ?? 0}
                    supportingText={t("nav.dialogues")}
                    compact
                  />
                  <CivicStatCard
                    label={t("community_home.official_count", { count: home?.officialUpdates.length ?? 0 })}
                    value={home?.officialUpdates.length ?? 0}
                    supportingText={t("community_blog.channel_badge")}
                    compact
                  />
                  <CivicStatCard
                    label={t("community_home.threads_count", { count: home?.hotThreads.length ?? 0 })}
                    value={home?.hotThreads.length ?? 0}
                    supportingText={t("nav.dialogues")}
                    compact
                  />
                  <CivicStatCard
                    label={t("community_home.signals_count", { count: home?.topSignals.length ?? 0 })}
                    value={home?.topSignals.length ?? 0}
                    supportingText={t("nav.report")}
                    compact
                  />
                </div>
              </CivicCard>

              <div className="grid">
                <div className="col-12 lg:col-6">
                  <CivicCard title={t("community_home.official_title")} className="h-full" data-testid="community-home-official">
                    {home && home.officialUpdates.length > 0 ? (
                      <div className="flex flex-column gap-4">
                        {home.officialUpdates.map((post) => (
                          <button
                            key={post.id}
                            type="button"
                            className="text-left border-none bg-transparent p-0 cursor-pointer"
                            onClick={() => navigate("/communities/blog")}
                          >
                            <div className="border-round-2xl border-1 border-subtle p-4 hover:border-brand-primary transition-colors">
                              <div className="flex justify-content-between gap-3 align-items-start">
                                <div>
                                  <h3 className="text-lg font-black text-main m-0">{post.title}</h3>
                                  <p className="text-sm text-secondary mt-2 mb-0 line-height-3">{post.statusTag}</p>
                                </div>
                                <CivicBadge label={t("community_blog.official_badge")} severity="progress" />
                              </div>
                            </div>
                          </button>
                        ))}
                      </div>
                    ) : (
                      <CivicEmptyState
                        icon="pi-megaphone"
                        title={t("community_home.official_empty_title")}
                        description={t("community_home.official_empty_desc")}
                        actionLabel={t("nav.public_blog")}
                        onAction={() => navigate("/communities/blog")}
                      />
                    )}
                  </CivicCard>
                </div>

                <div className="col-12 lg:col-6">
                  <CivicCard title={t("community_home.threads_title")} className="h-full" data-testid="community-home-threads">
                    {home && home.hotThreads.length > 0 ? (
                      <div className="flex flex-column gap-4">
                        {home.hotThreads.map((thread) => (
                          <button
                            key={thread.id}
                            type="button"
                            className="text-left border-none bg-transparent p-0 cursor-pointer"
                            onClick={() => navigate("/communities/threads")}
                          >
                            <div className="border-round-2xl border-1 border-subtle p-4 hover:border-brand-primary transition-colors">
                              <div className="flex justify-content-between gap-3 align-items-start">
                                <div>
                                  <h3 className="text-lg font-black text-main m-0">{thread.title}</h3>
                                  <p className="text-sm text-secondary mt-2 mb-0 line-height-3">{thread.relevanceSummary}</p>
                                </div>
                                <CivicBadge label={t("community_threads.relevance_score")} severity="neutral" />
                              </div>
                            </div>
                          </button>
                        ))}
                      </div>
                    ) : (
                      <CivicEmptyState
                        icon="pi-comments"
                        title={t("community_home.threads_empty_title")}
                        description={t("community_home.threads_empty_desc")}
                        actionLabel={t("nav.dialogues")}
                        onAction={() => navigate("/communities/threads")}
                      />
                    )}
                  </CivicCard>
                </div>
              </div>
            </div>

            <div className="col-12 xl:col-4">
              <CivicCard title={t("community_home.signals_title")} data-testid="community-home-signals">
                {home && home.topSignals.length > 0 ? (
                  <div className="flex flex-column gap-4">
                    {home.topSignals.map((signal) => (
                      <button
                        key={signal.id}
                        type="button"
                        className="text-left border-none bg-transparent p-0 cursor-pointer"
                        onClick={() => navigate(`/signal/${signal.id}`)}
                      >
                        <div className="border-round-2xl border-1 border-subtle p-4 hover:border-brand-primary transition-colors">
                          <div className="flex justify-content-between align-items-start gap-3">
                            <div>
                              <h3 className="text-base font-black text-main m-0">{signal.title}</h3>
                              <p className="text-sm text-secondary mt-2 mb-0">
                                {t("common.status")}: {signal.status}
                              </p>
                            </div>
                            <span className="u-pill">{Math.round(signal.priorityScore ?? 0)}</span>
                          </div>
                        </div>
                      </button>
                    ))}
                    <CivicButton
                      type="button"
                      label={t("community_home.signals_action")}
                      icon="pi pi-arrow-right"
                      variant="ghost"
                      onClick={() => navigate("/")}
                    />
                  </div>
                ) : (
                  <CivicEmptyState
                    icon="pi-bolt"
                    title={t("community_home.signals_empty_title")}
                    description={t("community_home.signals_empty_desc")}
                    actionLabel={t("nav.report")}
                    onAction={() => navigate("/report")}
                  />
                )}
              </CivicCard>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
