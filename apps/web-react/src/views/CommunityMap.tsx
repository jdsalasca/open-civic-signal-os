import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { useTranslation } from "react-i18next";
import {
  CommunitySignalHeatCell,
  CommunitySignalMap,
  CommunitySignalMapPoint,
  CommunitySignalsHeatMap,
} from "../types";
import { Layout } from "../components/Layout";
import { useCommunityStore } from "../store/useCommunityStore";
import apiClient from "../api/axios";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";
import { CivicActionBar } from "../components/ui/CivicActionBar";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";
import { CivicSelect } from "../components/ui/CivicSelect";
import { CivicBadge } from "../components/ui/CivicBadge";

type ApiError = Error & { friendlyMessage?: string };

type GeoFiltersState = {
  category: string;
  status: string;
  fromDate: string;
  toDate: string;
};

const defaultFilters: GeoFiltersState = {
  category: "",
  status: "",
  fromDate: "",
  toDate: "",
};

function normalizeCoordinate(
  value: number,
  min: number,
  max: number,
  paddingPercent = 8
) {
  if (max === min) {
    return 50;
  }
  const ratio = (value - min) / (max - min);
  return paddingPercent + ratio * (100 - paddingPercent * 2);
}

function buildQuery(filters: GeoFiltersState, communityId?: string | null) {
  const params = new URLSearchParams();
  if (communityId) {
    params.set("communityId", communityId);
  }
  if (filters.category) {
    params.set("category", filters.category);
  }
  if (filters.status) {
    params.set("status", filters.status);
  }
  if (filters.fromDate) {
    params.set("fromDate", filters.fromDate);
  }
  if (filters.toDate) {
    params.set("toDate", filters.toDate);
  }
  return params.toString();
}

function CommunityGeoCanvas({
  points,
  onSelect,
}: {
  points: CommunitySignalMapPoint[];
  onSelect: (point: CommunitySignalMapPoint) => void;
}) {
  const bounds = useMemo(() => {
    const latitudes = points.map((point) => point.latitude);
    const longitudes = points.map((point) => point.longitude);
    return {
      minLat: Math.min(...latitudes),
      maxLat: Math.max(...latitudes),
      minLng: Math.min(...longitudes),
      maxLng: Math.max(...longitudes),
    };
  }, [points]);

  return (
    <div className="community-map-canvas" data-testid="community-map-canvas">
      <div className="community-map-grid" aria-hidden="true" />
      {points.map((point) => {
        const top = 100 - normalizeCoordinate(point.latitude, bounds.minLat, bounds.maxLat);
        const left = normalizeCoordinate(point.longitude, bounds.minLng, bounds.maxLng);
        return (
          <button
            key={point.signalId}
            type="button"
            className="community-map-point"
            style={{
              top: `${top}%`,
              left: `${left}%`,
              ["--point-size" as string]: `${Math.min(26, Math.max(14, point.heatWeight / 18))}px`,
            }}
            aria-label={`${point.title} ${point.locationLabel ?? ""}`}
            onClick={() => onSelect(point)}
            data-testid={`community-map-point-${point.signalId}`}
          >
            <span className="community-map-point-core" />
          </button>
        );
      })}
    </div>
  );
}

function CommunityHeatCanvas({
  communities,
  activeCommunityId,
  onSelect,
}: {
  communities: CommunitySignalHeatCell[];
  activeCommunityId: string | null;
  onSelect: (communityId: string) => void;
}) {
  const bounds = useMemo(() => {
    const latitudes = communities.map((community) => community.latitude);
    const longitudes = communities.map((community) => community.longitude);
    return {
      minLat: Math.min(...latitudes),
      maxLat: Math.max(...latitudes),
      minLng: Math.min(...longitudes),
      maxLng: Math.max(...longitudes),
    };
  }, [communities]);

  return (
    <div className="community-heat-canvas" data-testid="community-heat-canvas">
      <div className="community-map-grid" aria-hidden="true" />
      {communities.map((community) => {
        const top = 100 - normalizeCoordinate(community.latitude, bounds.minLat, bounds.maxLat);
        const left = normalizeCoordinate(community.longitude, bounds.minLng, bounds.maxLng);
        const intensity = Math.min(42, Math.max(20, community.cumulativeHeatScore / 22));
        return (
          <button
            key={community.communityId}
            type="button"
            className={`community-heat-node ${community.communityId === activeCommunityId ? "is-active" : ""}`}
            style={{
              top: `${top}%`,
              left: `${left}%`,
              ["--heat-size" as string]: `${intensity}px`,
            }}
            onClick={() => onSelect(community.communityId)}
            aria-label={community.communityName}
            data-testid={`community-heat-node-${community.communityId}`}
          >
            <span className="community-heat-node-core" />
            <span className="community-heat-node-label">{community.communityName}</span>
          </button>
        );
      })}
    </div>
  );
}

export function CommunityMap() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { activeCommunityId, memberships, setActiveCommunityId } = useCommunityStore();
  const [filters, setFilters] = useState<GeoFiltersState>(defaultFilters);
  const [appliedFilters, setAppliedFilters] = useState<GeoFiltersState>(defaultFilters);
  const [communityMap, setCommunityMap] = useState<CommunitySignalMap | null>(null);
  const [heatMap, setHeatMap] = useState<CommunitySignalsHeatMap | null>(null);
  const [loading, setLoading] = useState(false);
  const [selectedPoint, setSelectedPoint] = useState<CommunitySignalMapPoint | null>(null);
  const [selectedHeatCommunity, setSelectedHeatCommunity] = useState<CommunitySignalHeatCell | null>(null);

  const activeMembership = memberships.find((membership) => membership.communityId === activeCommunityId) ?? null;

  const loadGeo = useCallback(async () => {
    if (!activeCommunityId) {
      setCommunityMap(null);
      setHeatMap(null);
      return;
    }
    setLoading(true);
    try {
      const query = buildQuery(appliedFilters, activeCommunityId);
      const heatQuery = buildQuery(appliedFilters);
      const [communityRes, heatRes] = await Promise.all([
        apiClient.get<CommunitySignalMap>(`signals/map?${query}`),
        apiClient.get<CommunitySignalsHeatMap>(`signals/map/heat?${heatQuery}`),
      ]);
      setCommunityMap(communityRes.data);
      setHeatMap(heatRes.data);
      setSelectedPoint(communityRes.data.points[0] ?? null);
      setSelectedHeatCommunity(heatRes.data.communities[0] ?? null);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_map.load_error"));
    } finally {
      setLoading(false);
    }
  }, [activeCommunityId, appliedFilters, t]);

  useEffect(() => {
    loadGeo();
  }, [loadGeo]);

  const categoryOptions = useMemo(() => [
    { label: t("community_map.filters.all_categories"), value: "" },
    ...((communityMap?.availableCategories ?? heatMap?.availableCategories ?? []).map((category) => ({
      label: category,
      value: category,
    }))),
  ], [communityMap?.availableCategories, heatMap?.availableCategories, t]);

  const statusOptions = useMemo(() => [
    { label: t("community_map.filters.all_statuses"), value: "" },
    ...((communityMap?.availableStatuses ?? heatMap?.availableStatuses ?? []).map((status) => ({
      label: status,
      value: status,
    }))),
  ], [communityMap?.availableStatuses, heatMap?.availableStatuses, t]);

  const hotspotClusters = useMemo(
    () => (communityMap?.clusters ?? []).slice(0, 4),
    [communityMap?.clusters]
  );
  const selectedPointDetails = selectedPoint ?? communityMap?.points[0] ?? null;
  const selectedCommunityHeat = selectedHeatCommunity ?? heatMap?.communities[0] ?? null;

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <div className="flex flex-column lg:flex-row justify-content-between align-items-start gap-4 mb-8">
          <CivicPageHeader
            title={t("community_map.title", { community: activeMembership?.communityName ?? t("dashboard.community_default") })}
            description={t("community_map.desc")}
            className="mb-0"
          />
          <CivicActionBar className="w-full lg:w-auto" data-testid="community-map-actionbar">
            <div className="u-pill" data-testid="community-map-freshness">
              <i className="pi pi-map text-brand-primary" />
              {communityMap?.freshness ?? heatMap?.freshness ?? t("dashboard.freshness_pending")}
            </div>
            <CivicButton
              type="button"
              icon="pi pi-users"
              label={t("nav.communities")}
              variant="ghost"
              onClick={() => navigate("/communities")}
            />
          </CivicActionBar>
        </div>

        {!activeCommunityId ? (
          <CivicCard>
            <CivicEmptyState
              icon="pi-map"
              title={t("community_map.no_context_title")}
              description={t("community_map.no_context_desc")}
              actionLabel={t("nav.communities")}
              onAction={() => navigate("/communities")}
            />
          </CivicCard>
        ) : (
          <>
            <CivicCard className="mb-6" data-testid="community-map-filter-card">
              <div className="community-map-filter-grid">
                <div>
                  <label className="u-eyebrow mb-2 block">
                    {t("community_map.filters.category")}
                  </label>
                  <CivicSelect
                    value={filters.category}
                    options={categoryOptions}
                    onChange={(event) => setFilters((current) => ({ ...current, category: event.value }))}
                    className="w-full"
                    data-testid="community-map-category-filter"
                  />
                </div>
                <div>
                  <label className="u-eyebrow mb-2 block">
                    {t("community_map.filters.status")}
                  </label>
                  <CivicSelect
                    value={filters.status}
                    options={statusOptions}
                    onChange={(event) => setFilters((current) => ({ ...current, status: event.value }))}
                    className="w-full"
                    data-testid="community-map-status-filter"
                  />
                </div>
                <div>
                  <label htmlFor="community-map-from-date" className="u-eyebrow mb-2 block">
                    {t("community_map.filters.from_date")}
                  </label>
                  <input
                    id="community-map-from-date"
                    type="date"
                    className="community-map-date-input"
                    value={filters.fromDate}
                    onChange={(event) => setFilters((current) => ({ ...current, fromDate: event.target.value }))}
                    data-testid="community-map-from-date-filter"
                  />
                </div>
                <div>
                  <label htmlFor="community-map-to-date" className="u-eyebrow mb-2 block">
                    {t("community_map.filters.to_date")}
                  </label>
                  <input
                    id="community-map-to-date"
                    type="date"
                    className="community-map-date-input"
                    value={filters.toDate}
                    onChange={(event) => setFilters((current) => ({ ...current, toDate: event.target.value }))}
                    data-testid="community-map-to-date-filter"
                  />
                </div>
                <div className="community-map-filter-actions">
                  <CivicButton
                    type="button"
                    label={t("community_map.filters.reset")}
                    icon="pi pi-refresh"
                    variant="secondary"
                    onClick={() => {
                      setFilters(defaultFilters);
                      setAppliedFilters(defaultFilters);
                    }}
                    data-testid="community-map-reset-filters"
                  />
                  <CivicButton
                    type="button"
                    label={t("community_map.filters.apply")}
                    icon="pi pi-filter"
                    variant="ghost"
                    onClick={() => setAppliedFilters(filters)}
                    data-testid="community-map-apply-filters"
                  />
                </div>
              </div>
            </CivicCard>

            <div className="grid">
              <div className="col-12 xl:col-8">
                <CivicCard
                  title={t("community_map.community_surface_title")}
                  className="mb-6"
                  data-testid="community-map-community-card"
                >
                  {loading ? (
                    <p className="text-secondary text-sm m-0">{t("common.loading")}</p>
                  ) : communityMap && communityMap.points.length > 0 ? (
                    <div className="flex flex-column gap-5">
                      <div className="community-map-summary-strip">
                        <span className="u-pill">{t("community_map.summary.mapped", { count: communityMap.mappedSignalsCount })}</span>
                        <span className="u-pill">{t("community_map.summary.unmapped", { count: communityMap.unmappedSignalsCount })}</span>
                        <span className="u-pill">{t("community_map.summary.heat", { count: Math.round(communityMap.cumulativeHeatScore) })}</span>
                      </div>
                      <CommunityGeoCanvas
                        points={communityMap.points}
                        onSelect={(point) => setSelectedPoint(point)}
                      />
                      {selectedPointDetails && (
                        <div className="community-map-inspector" data-testid="community-map-inspector">
                          <div className="community-map-inspector-copy">
                            <div className="u-eyebrow">
                              {selectedPointDetails.category}
                            </div>
                            <h3 className="u-section-title-lg m-0 mt-2">{selectedPointDetails.title}</h3>
                            <p className="u-section-copy text-sm mt-2 mb-0 line-height-3">
                              {selectedPointDetails.locationLabel || t("community_map.location_missing")}
                            </p>
                          </div>
                          <div className="community-map-inspector-meta">
                            <CivicBadge label={selectedPointDetails.status} severity="neutral" />
                            <span className="u-pill">{t("community_map.summary.heat", { count: Math.round(selectedPointDetails.heatWeight) })}</span>
                            <CivicButton
                              type="button"
                              icon="pi pi-arrow-right"
                              label={t("signals.view_details")}
                              variant="secondary"
                              onClick={() => navigate(`/signal/${selectedPointDetails.signalId}`)}
                            />
                          </div>
                        </div>
                      )}
                      <div className="grid">
                        {communityMap.points.slice(0, 4).map((point) => (
                          <div key={point.signalId} className="col-12 md:col-6">
                            <button
                              type="button"
                              className={`community-map-listing ${selectedPointDetails?.signalId === point.signalId ? "is-selected" : ""}`}
                              onClick={() => setSelectedPoint(point)}
                            >
                              <div className="flex justify-content-between gap-3 align-items-start">
                                <div>
                                  <div className="u-eyebrow">
                                    {point.category}
                                  </div>
                                  <h3 className="u-list-item-title text-base m-0 mt-2">{point.title}</h3>
                                  <p className="u-list-item-copy text-sm mt-2 mb-0">
                                    {point.locationLabel || t("community_map.location_missing")}
                                  </p>
                                </div>
                                <CivicBadge label={point.status} severity="neutral" />
                              </div>
                            </button>
                          </div>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <CivicEmptyState
                      icon="pi-map-marker"
                      title={t("community_map.empty_points_title")}
                      description={t("community_map.empty_points_desc", { community: activeMembership?.communityName ?? "" })}
                      actionLabel={t("nav.report")}
                      onAction={() => navigate("/report")}
                    />
                  )}
                </CivicCard>
              </div>

              <div className="col-12 xl:col-4">
                <CivicCard title={t("community_map.hotspots_title")} className="mb-6" data-testid="community-map-hotspots-card">
                  {hotspotClusters.length > 0 ? (
                    <div className="flex flex-column gap-3">
                      {hotspotClusters.map((cluster) => (
                        <div key={cluster.clusterKey} className="community-map-hotspot">
                          <div className="flex justify-content-between gap-3">
                            <div>
                              <div className="u-eyebrow">
                                {cluster.primaryCategory}
                              </div>
                              <h3 className="u-list-item-title text-base m-0 mt-2">
                                {cluster.topSignalTitle || t("community_map.hotspot_fallback")}
                              </h3>
                              <p className="u-list-item-copy text-sm mt-2 mb-0">
                                {t("community_map.hotspot_summary", {
                                  count: cluster.signalCount,
                                  score: Math.round(cluster.cumulativePriorityScore),
                                })}
                              </p>
                            </div>
                            <CivicBadge label={t("community_map.cluster_badge")} severity="progress" />
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <CivicEmptyState
                      icon="pi-compass"
                      title={t("community_map.hotspots_empty_title")}
                      description={t("community_map.hotspots_empty_desc")}
                    />
                  )}
                </CivicCard>

                <CivicCard title={t("community_map.cross_community_title")} data-testid="community-map-heat-card">
                  {heatMap && heatMap.communities.length > 0 ? (
                    <div className="flex flex-column gap-4">
                      <div className="community-map-summary-strip">
                        <span className="u-pill">{t("community_map.summary.communities", { count: heatMap.visibleCommunitiesCount })}</span>
                        <span className="u-pill">{t("community_map.summary.total_points", { count: heatMap.totalMappedSignalsCount })}</span>
                      </div>
                      <CommunityHeatCanvas
                        communities={heatMap.communities}
                        activeCommunityId={activeCommunityId}
                        onSelect={(communityId) => {
                          setActiveCommunityId(communityId);
                          const nextCommunity = heatMap.communities.find((community) => community.communityId === communityId) ?? null;
                          setSelectedHeatCommunity(nextCommunity);
                        }}
                      />
                      {selectedCommunityHeat && (
                        <div className="community-map-inspector" data-testid="community-heat-inspector">
                          <div className="community-map-inspector-copy">
                            <div className="u-eyebrow">
                              {selectedCommunityHeat.topCategory}
                            </div>
                            <h3 className="u-section-title-lg m-0 mt-2">{selectedCommunityHeat.communityName}</h3>
                            <p className="u-section-copy text-sm mt-2 mb-0 line-height-3">
                              {t("community_map.community_summary", {
                                mapped: selectedCommunityHeat.mappedSignalsCount,
                                score: Math.round(selectedCommunityHeat.cumulativeHeatScore),
                              })}
                            </p>
                          </div>
                          <div className="community-map-inspector-meta">
                            <span className="u-pill">{t("community_map.summary.total_points", { count: selectedCommunityHeat.mappedSignalsCount })}</span>
                            <CivicButton
                              type="button"
                              icon="pi pi-crosshairs"
                              label={t("community_map.focus_community")}
                              variant="secondary"
                              onClick={() => setActiveCommunityId(selectedCommunityHeat.communityId)}
                            />
                          </div>
                        </div>
                      )}
                      <div className="flex flex-column gap-3">
                        {heatMap.communities.slice(0, 4).map((community) => (
                          <button
                            key={community.communityId}
                            type="button"
                            className={`community-map-listing ${selectedCommunityHeat?.communityId === community.communityId ? "is-selected" : ""}`}
                            onClick={() => {
                              setSelectedHeatCommunity(community);
                              setActiveCommunityId(community.communityId);
                            }}
                            data-testid={`community-map-heat-row-${community.communityId}`}
                          >
                            <div className="flex justify-content-between align-items-start gap-3">
                              <div>
                                <div className="u-eyebrow">
                                  {community.topCategory}
                                </div>
                                <h3 className="u-list-item-title text-base m-0 mt-2">{community.communityName}</h3>
                                <p className="u-list-item-copy text-sm mt-2 mb-0">
                                  {t("community_map.community_summary", {
                                    mapped: community.mappedSignalsCount,
                                    score: Math.round(community.cumulativeHeatScore),
                                  })}
                                </p>
                              </div>
                              {community.communityId === activeCommunityId && (
                                <CivicBadge label={t("communities_hub.context_active")} severity="progress" />
                              )}
                            </div>
                          </button>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <CivicEmptyState
                      icon="pi-globe"
                      title={t("community_map.cross_empty_title")}
                      description={t("community_map.cross_empty_desc")}
                    />
                  )}
                </CivicCard>
              </div>
            </div>
          </>
        )}
      </div>
    </Layout>
  );
}
