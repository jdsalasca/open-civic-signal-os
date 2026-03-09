import { useCallback, useEffect, useMemo, useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { Checkbox, CheckboxChangeEvent } from "primereact/checkbox";
import { InputTextarea } from "primereact/inputtextarea";
import { classNames } from "primereact/utils";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { Layout } from "../components/Layout";
import { CivicActionBar } from "../components/ui/CivicActionBar";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicCharacterCount } from "../components/ui/CivicCharacterCount";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";
import { CivicField } from "../components/ui/CivicField";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";
import { CivicSelect } from "../components/ui/CivicSelect";
import { CivicStatCard } from "../components/ui/CivicStatCard";
import { useCommunityStore } from "../store/useCommunityStore";
import { useAuthStore } from "../store/useAuthStore";
import type {
  CommunityModerationQueue,
  CommunityModerationReportStatus,
  CommunityModerationTargetType,
  CommunityPermissionPolicy,
  CommunitySanctionType,
} from "../types";

type ApiError = Error & { friendlyMessage?: string };

const RESOLUTION_MIN = 8;
const RESOLUTION_MAX = 2000;

export function Moderation() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const activeRole = useAuthStore((state) => state.activeRole);
  const { activeCommunityId, memberships } = useCommunityStore();
  const activeMembership = memberships.find((membership) => membership.communityId === activeCommunityId) ?? null;
  const [queue, setQueue] = useState<CommunityModerationQueue | null>(null);
  const [permissionPolicies, setPermissionPolicies] = useState<CommunityPermissionPolicy[]>([]);
  const [loadingQueue, setLoadingQueue] = useState(false);
  const [loadingPolicies, setLoadingPolicies] = useState(false);
  const [submittingAction, setSubmittingAction] = useState(false);
  const [statusFilter, setStatusFilter] = useState<CommunityModerationReportStatus | "ALL">("OPEN");
  const [targetTypeFilter, setTargetTypeFilter] = useState<CommunityModerationTargetType | "ALL">("ALL");
  const [selectedReportId, setSelectedReportId] = useState<string | null>(null);
  const [action, setAction] = useState<"DISMISS" | "ENFORCE">("ENFORCE");
  const [hideContent, setHideContent] = useState(true);
  const [sanctionType, setSanctionType] = useState<CommunitySanctionType | "NONE">("WARN");
  const [resolutionReason, setResolutionReason] = useState("");

  const moderationPolicy = useMemo(
    () => permissionPolicies.find((policy) => policy.scope === "MANAGE_MODERATION_QUEUE"),
    [permissionPolicies]
  );
  const canManageModeration = Boolean(
    activeMembership &&
      moderationPolicy?.allowedRoles.includes(activeMembership.role)
  );

  const selectedReport = useMemo(
    () => queue?.reports.find((report) => report.id === selectedReportId) ?? queue?.reports[0] ?? null,
    [queue?.reports, selectedReportId]
  );

  const statusOptions = [
    { label: t("moderation.filters.status_all"), value: "ALL" },
    { label: t("moderation.status.OPEN"), value: "OPEN" },
    { label: t("moderation.status.ACTIONED"), value: "ACTIONED" },
    { label: t("moderation.status.DISMISSED"), value: "DISMISSED" },
  ] as Array<{ label: string; value: CommunityModerationReportStatus | "ALL" }>;

  const targetTypeOptions = [
    { label: t("moderation.filters.target_all"), value: "ALL" },
    { label: t("moderation.target_types.THREAD_MESSAGE"), value: "THREAD_MESSAGE" },
    { label: t("moderation.target_types.PROPOSAL_DELIBERATION"), value: "PROPOSAL_DELIBERATION" },
  ] as Array<{ label: string; value: CommunityModerationTargetType | "ALL" }>;

  const sanctionOptions = [
    { label: t("moderation.sanctions.NONE"), value: "NONE" },
    { label: t("moderation.sanctions.WARN"), value: "WARN" },
    { label: t("moderation.sanctions.LIMIT_POSTING_7_DAYS"), value: "LIMIT_POSTING_7_DAYS" },
    { label: t("moderation.sanctions.SUSPEND_7_DAYS"), value: "SUSPEND_7_DAYS" },
    { label: t("moderation.sanctions.SUSPEND_30_DAYS"), value: "SUSPEND_30_DAYS" },
  ] as Array<{ label: string; value: CommunitySanctionType | "NONE" }>;

  const resetActionForm = useCallback(() => {
    setAction("ENFORCE");
    setHideContent(true);
    setSanctionType("WARN");
    setResolutionReason("");
  }, []);

  const loadPolicies = useCallback(async () => {
    if (!activeCommunityId) {
      setPermissionPolicies([]);
      return;
    }
    setLoadingPolicies(true);
    try {
      const response = await apiClient.get<CommunityPermissionPolicy[]>(`communities/${activeCommunityId}/permissions`);
      setPermissionPolicies(response.data ?? []);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("moderation.permissions_load_error"));
      setPermissionPolicies([]);
    } finally {
      setLoadingPolicies(false);
    }
  }, [activeCommunityId, t]);

  const loadQueue = useCallback(async () => {
    if (!activeCommunityId) {
      setQueue(null);
      return;
    }
    setLoadingQueue(true);
    try {
      const params = new URLSearchParams({ communityId: activeCommunityId });
      if (statusFilter !== "ALL") {
        params.set("status", statusFilter);
      }
      if (targetTypeFilter !== "ALL") {
        params.set("targetType", targetTypeFilter);
      }
      const response = await apiClient.get<CommunityModerationQueue>(`community/moderation/queue?${params.toString()}`);
      const nextQueue = response.data;
      setQueue(nextQueue);
      setSelectedReportId((current) =>
        nextQueue.reports.some((report) => report.id === current) ? current : nextQueue.reports[0]?.id ?? null
      );
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("moderation.load_error"));
      setQueue(null);
    } finally {
      setLoadingQueue(false);
    }
  }, [activeCommunityId, statusFilter, targetTypeFilter, t]);

  useEffect(() => {
    loadPolicies();
  }, [loadPolicies]);

  useEffect(() => {
    loadQueue();
  }, [loadQueue]);

  useEffect(() => {
    resetActionForm();
  }, [resetActionForm, selectedReportId]);

  if (!activeCommunityId || !activeMembership) {
    return (
      <Layout>
        <CivicCard>
          <CivicEmptyState
            icon="pi pi-users"
            title={t("moderation.no_context_title")}
            description={t("moderation.no_context_desc")}
            actionLabel={t("nav.communities")}
            onAction={() => navigate("/communities")}
          />
        </CivicCard>
      </Layout>
    );
  }

  if (!loadingPolicies && moderationPolicy && !canManageModeration && activeRole !== "SUPER_ADMIN") {
    return <Navigate to="/unauthorized" replace />;
  }

  const handleResolve = async () => {
    if (!selectedReport) {
      return;
    }
    if (resolutionReason.trim().length < RESOLUTION_MIN) {
      toast.error(t("moderation.resolution_too_short"));
      return;
    }
    setSubmittingAction(true);
    try {
      await apiClient.patch(`community/moderation/reports/${selectedReport.id}`, {
        action,
        hideContent: action === "ENFORCE" ? hideContent : false,
        sanctionType: action === "ENFORCE" && sanctionType !== "NONE" ? sanctionType : null,
        resolutionReason: resolutionReason.trim(),
      });
      toast.success(t("moderation.action_success"));
      resetActionForm();
      await loadQueue();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("moderation.action_error"));
    } finally {
      setSubmittingAction(false);
    }
  };

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <div className="flex flex-column xl:flex-row justify-content-between align-items-start gap-4 mb-8">
          <CivicPageHeader
            title={t("moderation.title")}
            description={t("moderation.desc", { community: activeMembership.communityName })}
            className="mb-0"
          />
          <CivicActionBar className="w-full xl:w-auto">
            <div className="community-home-action-copy">
              <div className="u-eyebrow">{t("moderation.kicker")}</div>
              <p className="u-section-copy text-sm m-0">{t("moderation.kicker_desc")}</p>
            </div>
            <div className="dashboard-action-cluster">
              <div className="min-w-12rem">
                <CivicSelect
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.value)}
                  options={statusOptions}
                  className="w-full"
                  data-testid="moderation-status-filter"
                />
              </div>
              <div className="min-w-14rem">
                <CivicSelect
                  value={targetTypeFilter}
                  onChange={(e) => setTargetTypeFilter(e.value)}
                  options={targetTypeOptions}
                  className="w-full"
                  data-testid="moderation-target-filter"
                />
              </div>
              <CivicButton type="button" icon="pi pi-refresh" label={t("moderation.refresh")} variant="secondary" onClick={loadQueue} />
            </div>
          </CivicActionBar>
        </div>

        {loadingPolicies ? (
          <CivicCard>
            <p className="text-secondary m-0">{t("common.loading")}</p>
          </CivicCard>
        ) : (
          <div className="grid">
            <div className="col-12 xl:col-4">
              <div className="civic-stat-grid civic-stat-grid-comfortable mb-6" data-testid="moderation-stats-grid">
                <CivicStatCard compact label={t("moderation.stats.open")} value={queue?.openReports ?? 0} supportingText={t("moderation.stats.open_support")} />
                <CivicStatCard compact label={t("moderation.stats.actioned")} value={queue?.actionedReports ?? 0} supportingText={t("moderation.stats.actioned_support")} />
                <CivicStatCard compact label={t("moderation.stats.dismissed")} value={queue?.dismissedReports ?? 0} supportingText={t("moderation.stats.dismissed_support")} />
                <CivicStatCard compact label={t("moderation.stats.active_sanctions")} value={queue?.activeSanctions ?? 0} supportingText={t("moderation.stats.active_sanctions_support")} />
              </div>

              <CivicCard title={t("moderation.queue_title")} data-testid="moderation-queue-card">
                {loadingQueue ? (
                  <p className="text-secondary m-0">{t("common.loading")}</p>
                ) : (queue?.reports.length ?? 0) === 0 ? (
                  <CivicEmptyState
                    icon="pi pi-shield"
                    title={t("moderation.empty_title")}
                    description={t("moderation.empty_desc")}
                  />
                ) : (
                  <div className="flex flex-column gap-3">
                    {queue?.reports.map((report) => (
                      <button
                        key={report.id}
                        type="button"
                        className="community-feed-list-card text-left border-none bg-transparent p-0 cursor-pointer"
                        onClick={() => setSelectedReportId(report.id)}
                        data-testid={`moderation-report-row-${report.id}`}
                      >
                        <div className="flex justify-content-between gap-3 align-items-start flex-wrap">
                          <div className="min-w-0 flex-1">
                            <div className="u-eyebrow">{t(`moderation.target_types.${report.targetType}`)}</div>
                            <h3 className="text-lg font-black text-main m-0 mt-2">{t(`moderation.reason_codes.${report.reasonCode}`)}</h3>
                            <p className="text-sm text-secondary mt-2 mb-0 line-height-3">{report.targetPreview}</p>
                          </div>
                          <CivicBadge
                            label={t(`moderation.status.${report.status}`)}
                            severity={report.status === "ACTIONED" ? "progress" : report.status === "DISMISSED" ? "neutral" : "rejected"}
                          />
                        </div>
                        <div className="u-meta-row mt-3">
                          <span>{report.reporterUsername}</span>
                          <span>{new Date(report.createdAt).toLocaleString()}</span>
                        </div>
                      </button>
                    ))}
                  </div>
                )}
              </CivicCard>
            </div>

            <div className="col-12 xl:col-8">
              {selectedReport ? (
                <div className="flex flex-column gap-4" data-testid="moderation-detail-card">
                  <CivicCard title={t("moderation.detail_title")}>
                    <div className="civic-stat-grid civic-stat-grid-comfortable mb-4">
                      <CivicStatCard compact label={t("moderation.detail.target")} value={t(`moderation.target_types.${selectedReport.targetType}`)} />
                      <CivicStatCard compact label={t("moderation.detail.reporter")} value={selectedReport.reporterUsername} />
                      <CivicStatCard compact label={t("moderation.detail.reported")} value={selectedReport.reportedUsername} />
                      <CivicStatCard compact label={t("moderation.detail.status")} value={t(`moderation.status.${selectedReport.status}`)} />
                    </div>

                    <div className="u-surface-note mb-4">
                      <div className="u-eyebrow mb-2">{t("moderation.detail.preview_label")}</div>
                      <p className="text-secondary m-0 line-height-3">{selectedReport.targetPreview}</p>
                    </div>

                    <div className="u-surface-note mb-4">
                      <div className="u-eyebrow mb-2">{t("moderation.detail.report_note_label")}</div>
                      <p className="text-secondary m-0 line-height-3">{selectedReport.details}</p>
                    </div>

                    <div className="flex gap-2 flex-wrap mb-4">
                      <CivicBadge label={t(`moderation.reason_codes.${selectedReport.reasonCode}`)} severity="neutral" />
                      {selectedReport.contentHidden && <CivicBadge label={t("moderation.content_hidden")} severity="rejected" />}
                      {selectedReport.falsePositiveReviewRecommended && <CivicBadge label={t("moderation.false_positive_review")} severity="neutral" />}
                    </div>

                    {selectedReport.sanction && (
                      <div className="u-surface-note mb-4" data-testid="moderation-sanction-summary">
                        <div className="flex justify-content-between align-items-start gap-3 flex-wrap mb-2">
                          <div className="u-eyebrow">{t("moderation.sanction_summary_title")}</div>
                          <CivicBadge label={t(`moderation.sanctions.${selectedReport.sanction.sanctionType}`)} severity="rejected" />
                        </div>
                        <p className="text-secondary m-0 line-height-3">
                          {selectedReport.sanction.reason}
                        </p>
                        <div className="u-meta-row mt-3">
                          <span>{selectedReport.sanction.issuedByUsername}</span>
                          <span>{new Date(selectedReport.sanction.startsAt).toLocaleString()}</span>
                          <span>{selectedReport.sanction.endsAt ? new Date(selectedReport.sanction.endsAt).toLocaleDateString() : t("moderation.no_end_date")}</span>
                        </div>
                      </div>
                    )}
                  </CivicCard>

                  <CivicCard title={t("moderation.action_title")} data-testid="moderation-action-card">
                    {selectedReport.status !== "OPEN" ? (
                      <CivicEmptyState
                        icon="pi pi-check-circle"
                        title={t("moderation.already_resolved_title")}
                        description={selectedReport.resolutionReason ?? t("moderation.already_resolved_desc")}
                      />
                    ) : (
                      <div className="flex flex-column gap-3">
                        <div className="grid">
                          <div className="col-12 md:col-6">
                            <CivicField label={t("moderation.action_label")}>
                              <CivicSelect
                                value={action}
                                onChange={(e) => setAction(e.value)}
                                options={[
                                  { label: t("moderation.actions.ENFORCE"), value: "ENFORCE" },
                                  { label: t("moderation.actions.DISMISS"), value: "DISMISS" },
                                ]}
                                className="w-full"
                                data-testid="moderation-action-select"
                              />
                            </CivicField>
                          </div>
                          <div className="col-12 md:col-6">
                            <CivicField label={t("moderation.sanction_label")}>
                              <CivicSelect
                                value={sanctionType}
                                onChange={(e) => setSanctionType(e.value)}
                                options={sanctionOptions}
                                className="w-full"
                                disabled={action !== "ENFORCE"}
                                data-testid="moderation-sanction-select"
                              />
                            </CivicField>
                          </div>
                        </div>

                        <div className="flex align-items-center gap-2">
                          <Checkbox
                            inputId="moderation-hide-content"
                            checked={hideContent}
                            onChange={(e: CheckboxChangeEvent) => setHideContent(Boolean(e.checked))}
                            disabled={action !== "ENFORCE"}
                          />
                          <label htmlFor="moderation-hide-content" className="text-sm text-main font-semibold">
                            {t("moderation.hide_content_label")}
                          </label>
                        </div>

                        <CivicField
                          label={t("moderation.resolution_reason_label")}
                          error={resolutionReason.trim().length > 0 && resolutionReason.trim().length < RESOLUTION_MIN ? t("moderation.resolution_too_short") : undefined}
                          helpText={t("moderation.resolution_reason_help")}
                        >
                          <div className="flex flex-column gap-2">
                            <InputTextarea
                              value={resolutionReason}
                              rows={5}
                              onChange={(e) => setResolutionReason(e.target.value)}
                              className={classNames("w-full", { "p-invalid": resolutionReason.trim().length > 0 && resolutionReason.trim().length < RESOLUTION_MIN })}
                              maxLength={RESOLUTION_MAX}
                              data-testid="moderation-resolution-input"
                              placeholder={t("moderation.resolution_reason_placeholder")}
                            />
                            <CivicCharacterCount current={resolutionReason.length} max={RESOLUTION_MAX} min={RESOLUTION_MIN} />
                          </div>
                        </CivicField>

                        <div className="flex justify-content-end">
                          <CivicButton type="button" icon="pi pi-check" label={t("moderation.submit_action")} loading={submittingAction} onClick={handleResolve} data-testid="moderation-submit-action" />
                        </div>
                      </div>
                    )}
                  </CivicCard>

                  <CivicCard title={t("moderation.history_title")} data-testid="moderation-history-card">
                    {selectedReport.actionHistory.length === 0 ? (
                      <CivicEmptyState icon="pi pi-history" title={t("moderation.history_empty_title")} description={t("moderation.history_empty_desc")} />
                    ) : (
                      <div className="flex flex-column gap-3">
                        {selectedReport.actionHistory.map((event, index) => (
                          <div className="u-surface-note" key={`${selectedReport.id}-${event.actionType}-${index}`}>
                            <div className="flex justify-content-between align-items-start gap-3 flex-wrap mb-2">
                              <div className="u-eyebrow">{t(`moderation.history.${event.actionType}`)}</div>
                              <span className="text-xs text-muted">{new Date(event.happenedAt).toLocaleString()}</span>
                            </div>
                            <p className="text-secondary m-0 line-height-3">{event.note}</p>
                            <div className="u-meta-row mt-3">
                              <span>{event.actorUsername}</span>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </CivicCard>
                </div>
              ) : (
                <CivicCard>
                  <CivicEmptyState icon="pi pi-shield" title={t("moderation.empty_detail_title")} description={t("moderation.empty_detail_desc")} />
                </CivicCard>
              )}
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
