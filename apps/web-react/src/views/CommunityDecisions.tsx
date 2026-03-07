import { useCallback, useEffect, useMemo, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
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
import { FORM_LIMITS } from "../constants/formLimits";
import { useCommunityStore } from "../store/useCommunityStore";
import type {
  CommunityDecision,
  CommunityDecisionBasisType,
  CommunityDecisionStatus,
  CommunityDecisionType,
  CommunityPermissionPolicy,
  CommunityProjectBoard,
  CommunityProposal,
  GovernanceDocument,
} from "../types";

type ApiError = Error & { friendlyMessage?: string };

type DecisionForm = {
  linkedProposalId: string | null;
  governanceDocumentId: string | null;
  projectBoardId: string | null;
  executionOwnerUsername: string;
  title: string;
  summary: string;
  decisionType: CommunityDecisionType;
  decisionStatus: CommunityDecisionStatus;
  approvalBasisType: CommunityDecisionBasisType;
  approvalBasisSummary: string;
  decidedAt: string;
  effectiveDate: string;
};

const defaultDecisionValues: DecisionForm = {
  linkedProposalId: null,
  governanceDocumentId: null,
  projectBoardId: null,
  executionOwnerUsername: "",
  title: "",
  summary: "",
  decisionType: "APPROVAL",
  decisionStatus: "RECORDED",
  approvalBasisType: "GOVERNANCE_RECORD",
  approvalBasisSummary: "",
  decidedAt: "",
  effectiveDate: "",
};

const decisionStatusOrder: CommunityDecisionStatus[] = ["RECORDED", "IN_EXECUTION", "COMPLETED", "REJECTED", "SUPERSEDED"];

export function CommunityDecisions() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { activeCommunityId, memberships } = useCommunityStore();
  const activeMembership = memberships.find((membership) => membership.communityId === activeCommunityId) ?? null;
  const [decisions, setDecisions] = useState<CommunityDecision[]>([]);
  const [selectedDecisionId, setSelectedDecisionId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadingPolicies, setLoadingPolicies] = useState(false);
  const [statusFilter, setStatusFilter] = useState<CommunityDecisionStatus | "ALL">("ALL");
  const [typeFilter, setTypeFilter] = useState<CommunityDecisionType | "ALL">("ALL");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [permissionPolicies, setPermissionPolicies] = useState<CommunityPermissionPolicy[]>([]);
  const [proposalOptions, setProposalOptions] = useState<Array<{ label: string; value: string }>>([]);
  const [governanceOptions, setGovernanceOptions] = useState<Array<{ label: string; value: string }>>([]);
  const [projectOptions, setProjectOptions] = useState<Array<{ label: string; value: string }>>([]);

  const {
    control,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<DecisionForm>({
    mode: "onChange",
    defaultValues: defaultDecisionValues,
  });

  const watchedTitle = watch("title") ?? "";
  const watchedSummary = watch("summary") ?? "";
  const watchedApprovalBasisSummary = watch("approvalBasisSummary") ?? "";
  const watchedDecisionStatus = watch("decisionStatus");
  const watchedApprovalBasisType = watch("approvalBasisType");

  const statusOptions = useMemo(
    () => [
      { label: t("community_decisions.filters.status_all"), value: "ALL" },
      ...decisionStatusOrder.map((status) => ({ label: t(`community_decisions.status.${status}`), value: status })),
    ],
    [t]
  );

  const decisionTypeOptions = useMemo(
    () => [
      { label: t("community_decisions.filters.type_all"), value: "ALL" },
      { label: t("community_decisions.types.APPROVAL"), value: "APPROVAL" },
      { label: t("community_decisions.types.REJECTION"), value: "REJECTION" },
      { label: t("community_decisions.types.PRIORITIZATION"), value: "PRIORITIZATION" },
      { label: t("community_decisions.types.DIRECTIVE"), value: "DIRECTIVE" },
      { label: t("community_decisions.types.STATUS_UPDATE"), value: "STATUS_UPDATE" },
    ],
    [t]
  );

  const creationTypeOptions = useMemo(() => decisionTypeOptions.filter((option) => option.value !== "ALL"), [decisionTypeOptions]);
  const creationStatusOptions = useMemo(() => statusOptions.filter((option) => option.value !== "ALL"), [statusOptions]);
  const basisOptions = useMemo(
    () => [
      { label: t("community_decisions.basis.COMMUNITY_VOTE"), value: "COMMUNITY_VOTE" },
      { label: t("community_decisions.basis.COORDINATOR_REVIEW"), value: "COORDINATOR_REVIEW" },
      { label: t("community_decisions.basis.GOVERNANCE_RECORD"), value: "GOVERNANCE_RECORD" },
      { label: t("community_decisions.basis.STAFF_DIRECTIVE"), value: "STAFF_DIRECTIVE" },
      { label: t("community_decisions.basis.MIXED_RECORD"), value: "MIXED_RECORD" },
    ],
    [t]
  );

  const selectedDecision = useMemo(
    () => decisions.find((decision) => decision.id === selectedDecisionId) ?? decisions[0] ?? null,
    [decisions, selectedDecisionId]
  );

  const decisionScopePolicy = useMemo(
    () => permissionPolicies.find((policy) => policy.scope === "MANAGE_DECISION_LEDGER"),
    [permissionPolicies]
  );
  const canManageDecisionLedger = Boolean(
    activeMembership && decisionScopePolicy?.allowedRoles.includes(activeMembership.role)
  );

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
      toast.error(apiErr.friendlyMessage || t("community_decisions.permissions_load_error"));
      setPermissionPolicies([]);
    } finally {
      setLoadingPolicies(false);
    }
  }, [activeCommunityId, t]);

  const loadOptions = useCallback(async () => {
    if (!activeCommunityId) {
      setProposalOptions([]);
      setGovernanceOptions([]);
      setProjectOptions([]);
      return;
    }
    try {
      const [proposalResponse, governanceResponse, projectResponse] = await Promise.all([
        apiClient.get<CommunityProposal[]>(`community/proposals?communityId=${activeCommunityId}`),
        apiClient.get<GovernanceDocument[]>(`community/governance?communityId=${activeCommunityId}`),
        apiClient.get<CommunityProjectBoard[]>(`community/projects?communityId=${activeCommunityId}`),
      ]);
      setProposalOptions((proposalResponse.data ?? []).map((proposal) => ({ label: proposal.title, value: proposal.id })));
      setGovernanceOptions((governanceResponse.data ?? []).map((document) => ({ label: document.title, value: document.id })));
      setProjectOptions((projectResponse.data ?? []).map((project) => ({ label: project.title, value: project.id })));
    } catch {
      setProposalOptions([]);
      setGovernanceOptions([]);
      setProjectOptions([]);
    }
  }, [activeCommunityId]);

  const loadDecisions = useCallback(async () => {
    if (!activeCommunityId) {
      setDecisions([]);
      setSelectedDecisionId(null);
      return;
    }
    setLoading(true);
    try {
      const params = new URLSearchParams({ communityId: activeCommunityId });
      if (statusFilter !== "ALL") params.set("decisionStatus", statusFilter);
      if (typeFilter !== "ALL") params.set("decisionType", typeFilter);
      if (dateFrom) params.set("dateFrom", dateFrom);
      if (dateTo) params.set("dateTo", dateTo);
      const response = await apiClient.get<CommunityDecision[]>(`community/decisions?${params.toString()}`);
      const nextDecisions = response.data ?? [];
      setDecisions(nextDecisions);
      setSelectedDecisionId((current) => {
        if (nextDecisions.length === 0) {
          return null;
        }
        return nextDecisions.some((decision) => decision.id === current) ? current : nextDecisions[0].id;
      });
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_decisions.load_error"));
    } finally {
      setLoading(false);
    }
  }, [activeCommunityId, dateFrom, dateTo, statusFilter, t, typeFilter]);

  useEffect(() => {
    loadPolicies();
    loadOptions();
  }, [loadOptions, loadPolicies]);

  useEffect(() => {
    loadDecisions();
  }, [loadDecisions]);

  const onSubmit = async (data: DecisionForm) => {
    if (!activeCommunityId) {
      toast.error(t("community_decisions.community_required"));
      return;
    }
    try {
      const response = await apiClient.post<CommunityDecision>("community/decisions", {
        communityId: activeCommunityId,
        linkedProposalId: data.linkedProposalId || null,
        governanceDocumentId: data.governanceDocumentId || null,
        projectBoardId: data.projectBoardId || null,
        executionOwnerUsername: data.executionOwnerUsername.trim() || null,
        title: data.title,
        summary: data.summary,
        decisionType: data.decisionType,
        decisionStatus: data.decisionStatus,
        approvalBasisType: data.approvalBasisType,
        approvalBasisSummary: data.approvalBasisSummary,
        decidedAt: data.decidedAt ? new Date(data.decidedAt).toISOString().slice(0, 19) : null,
        effectiveDate: data.effectiveDate || null,
      });
      toast.success(t("community_decisions.create_success"));
      reset(defaultDecisionValues);
      await loadDecisions();
      setSelectedDecisionId(response.data.id);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_decisions.create_error"));
    }
  };

  const formatDate = (value?: string | null) => (value ? new Date(value).toLocaleDateString() : t("community_decisions.not_set"));
  const formatDateTime = (value?: string | null) => (value ? new Date(value).toLocaleString() : t("community_decisions.not_set"));

  const stats = useMemo(
    () => ({
      total: decisions.length,
      inExecution: decisions.filter((decision) => decision.decisionStatus === "IN_EXECUTION").length,
      completed: decisions.filter((decision) => decision.decisionStatus === "COMPLETED").length,
      basisVisible: decisions.filter((decision) => Boolean(decision.approvalBasisSummary)).length,
    }),
    [decisions]
  );

  const relatedLinks = (decision: CommunityDecision) =>
    [
      decision.linkedProposalId
        ? {
            key: "proposal",
            label: decision.linkedProposalTitle ?? t("community_decisions.related.proposal_fallback"),
            actionLabel: t("community_decisions.related.open_proposal"),
            onClick: () => navigate("/communities/proposals"),
          }
        : null,
      decision.governanceDocumentId
        ? {
            key: "governance",
            label: decision.governanceDocumentTitle ?? t("community_decisions.related.governance_fallback"),
            actionLabel: t("community_decisions.related.open_governance"),
            onClick: () => navigate("/communities/governance"),
          }
        : null,
      decision.projectBoardId
        ? {
            key: "project",
            label: decision.projectBoardTitle ?? t("community_decisions.related.project_fallback"),
            actionLabel: t("community_decisions.related.open_project"),
            onClick: () => navigate("/communities/projects"),
          }
        : null,
    ].filter(Boolean) as Array<{ key: string; label: string; actionLabel: string; onClick: () => void }>;

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <div className="flex flex-column xl:flex-row justify-content-between align-items-start gap-4 mb-8">
          <CivicPageHeader
            title={t("community_decisions.title")}
            description={t("community_decisions.desc", {
              community: activeMembership?.communityName ?? t("dashboard.community_default"),
            })}
            className="mb-0"
          />
          <CivicActionBar className="w-full xl:w-auto">
            <div className="community-home-action-copy">
              <div className="u-eyebrow">{t("community_decisions.badge")}</div>
              <p className="u-section-copy text-sm m-0">{t("community_decisions.badge_desc")}</p>
            </div>
            <div className="dashboard-action-cluster">
              <CivicButton type="button" icon="pi pi-file-edit" label={t("community_proposals.open_hub")} variant="ghost" onClick={() => navigate("/communities/proposals")} />
              <CivicButton type="button" icon="pi pi-briefcase" label={t("nav.community_projects")} variant="ghost" onClick={() => navigate("/communities/projects")} />
              <CivicButton type="button" icon="pi pi-book" label={t("nav.community_governance")} variant="ghost" onClick={() => navigate("/communities/governance")} />
            </div>
          </CivicActionBar>
        </div>

        {!activeCommunityId ? (
          <CivicCard>
            <CivicEmptyState
              icon="pi pi-sitemap"
              title={t("community_decisions.no_context_title")}
              description={t("community_decisions.no_context_desc")}
              actionLabel={t("nav.communities")}
              onAction={() => navigate("/communities")}
            />
          </CivicCard>
        ) : (
          <div className="grid">
            <div className="col-12 xl:col-4">
              <CivicCard title={t("community_decisions.filter_title")} className="mb-6" data-testid="community-decision-filter-card">
                <div className="flex flex-column gap-3">
                  <CivicField label={t("community_decisions.filters.status_label")}>
                    <CivicSelect value={statusFilter} onChange={(e) => setStatusFilter(e.value)} options={statusOptions} className="w-full" data-testid="decision-status-filter" />
                  </CivicField>
                  <CivicField label={t("community_decisions.filters.type_label")}>
                    <CivicSelect value={typeFilter} onChange={(e) => setTypeFilter(e.value)} options={decisionTypeOptions} className="w-full" data-testid="decision-type-filter" />
                  </CivicField>
                  <div className="grid">
                    <div className="col-12 md:col-6 xl:col-12">
                      <CivicField label={t("community_decisions.filters.date_from")}>
                        <InputText value={dateFrom} type="date" onChange={(e) => setDateFrom(e.target.value)} className="w-full" data-testid="decision-date-from" />
                      </CivicField>
                    </div>
                    <div className="col-12 md:col-6 xl:col-12">
                      <CivicField label={t("community_decisions.filters.date_to")}>
                        <InputText value={dateTo} type="date" onChange={(e) => setDateTo(e.target.value)} className="w-full" data-testid="decision-date-to" />
                      </CivicField>
                    </div>
                  </div>
                  <div className="flex justify-content-end gap-2 flex-wrap">
                    <CivicButton
                      type="button"
                      variant="ghost"
                      icon="pi pi-refresh"
                      label={t("community_decisions.filters.reset")}
                      onClick={() => {
                        setStatusFilter("ALL");
                        setTypeFilter("ALL");
                        setDateFrom("");
                        setDateTo("");
                      }}
                    />
                    <CivicButton type="button" icon="pi pi-search" label={t("community_decisions.filters.apply")} onClick={loadDecisions} data-testid="decision-apply-filters" />
                  </div>
                </div>
              </CivicCard>

              <CivicCard title={t("community_decisions.list_title")} className="mb-6" data-testid="community-decision-list-card">
                {loading ? (
                  <p className="text-secondary m-0">{t("common.loading")}</p>
                ) : decisions.length === 0 ? (
                  <CivicEmptyState
                    icon="pi pi-sitemap"
                    title={t("community_decisions.empty_title")}
                    description={t("community_decisions.empty_desc")}
                    actionLabel={t("community_decisions.empty_action")}
                    onAction={() => window.scrollTo({ top: 0, behavior: "smooth" })}
                  />
                ) : (
                  <div className="flex flex-column gap-3">
                    {decisions.map((decision) => (
                      <button
                        key={decision.id}
                        type="button"
                        className="community-feed-list-card text-left border-none bg-transparent p-0 cursor-pointer"
                        onClick={() => setSelectedDecisionId(decision.id)}
                        data-testid={`community-decision-row-${decision.id}`}
                      >
                        <div className="flex justify-content-between gap-3 align-items-start flex-wrap">
                          <div className="min-w-0 flex-1">
                            <div className="u-eyebrow">{t(`community_decisions.types.${decision.decisionType}`)}</div>
                            <h3 className="text-lg font-black text-main m-0 mt-2">{decision.title}</h3>
                            <p className="text-sm text-secondary mt-2 mb-0 line-height-3">{decision.summary}</p>
                          </div>
                          <CivicBadge
                            label={t(`community_decisions.status.${decision.decisionStatus}`)}
                            severity={
                              decision.decisionStatus === "COMPLETED"
                                ? "progress"
                                : decision.decisionStatus === "REJECTED"
                                  ? "rejected"
                                  : "neutral"
                            }
                          />
                        </div>
                        <div className="u-meta-row mt-3">
                          <span>{decision.decidedByUsername}</span>
                          <span>{formatDate(decision.decidedAt)}</span>
                        </div>
                      </button>
                    ))}
                  </div>
                )}
              </CivicCard>

              <CivicCard title={t("community_decisions.create_title")} data-testid="community-decision-create-card">
                {loadingPolicies ? (
                  <p className="text-secondary m-0">{t("common.loading")}</p>
                ) : !canManageDecisionLedger ? (
                  <CivicEmptyState
                    icon="pi pi-lock"
                    title={t("community_decisions.locked_title")}
                    description={t("community_decisions.locked_desc", {
                      role: activeMembership ? t(`settings.roles.${activeMembership.role}`) : t("settings.community_none"),
                    })}
                  />
                ) : (
                  <form className="flex flex-column gap-3" onSubmit={handleSubmit(onSubmit)}>
                    <div className="civic-stat-grid civic-stat-grid-comfortable mb-4">
                      <CivicStatCard compact label={t("community_decisions.stats.total")} value={stats.total} supportingText={t("community_decisions.stats.total_support")} />
                      <CivicStatCard compact label={t("community_decisions.stats.in_execution")} value={stats.inExecution} supportingText={t("community_decisions.stats.in_execution_support")} />
                      <CivicStatCard compact label={t("community_decisions.stats.completed")} value={stats.completed} supportingText={t("community_decisions.stats.completed_support")} />
                      <CivicStatCard compact label={t("community_decisions.stats.basis_visible")} value={stats.basisVisible} supportingText={t("community_decisions.stats.basis_visible_support")} />
                    </div>

                    <CivicField label={t("community_decisions.form.title_label")} error={errors.title?.message} helpText={t("community_decisions.form.title_help")}>
                      <Controller
                        name="title"
                        control={control}
                        rules={{
                          required: t("common.required"),
                          minLength: { value: FORM_LIMITS.decisions.titleMin, message: t("community_decisions.form.title_too_short") },
                          maxLength: { value: FORM_LIMITS.decisions.titleMax, message: t("community_decisions.form.title_too_long") },
                        }}
                        render={({ field, fieldState }) => (
                          <div className="flex flex-column gap-2">
                            <InputText
                              {...field}
                              onChange={(e) => field.onChange(e.target.value)}
                              className={classNames("w-full", { "p-invalid": fieldState.error })}
                              maxLength={FORM_LIMITS.decisions.titleMax}
                              data-testid="decision-title-input"
                              placeholder={t("community_decisions.form.title_placeholder")}
                            />
                            <CivicCharacterCount current={watchedTitle.length} max={FORM_LIMITS.decisions.titleMax} min={FORM_LIMITS.decisions.titleMin} />
                          </div>
                        )}
                      />
                    </CivicField>

                    <div className="grid">
                      <div className="col-12 md:col-6">
                        <CivicField label={t("community_decisions.form.type_label")}>
                          <Controller name="decisionType" control={control} render={({ field }) => <CivicSelect value={field.value} onChange={(e) => field.onChange(e.value)} options={creationTypeOptions} className="w-full" data-testid="decision-type-select" />} />
                        </CivicField>
                      </div>
                      <div className="col-12 md:col-6">
                        <CivicField label={t("community_decisions.form.status_label")}>
                          <Controller name="decisionStatus" control={control} render={({ field }) => <CivicSelect value={field.value} onChange={(e) => field.onChange(e.value)} options={creationStatusOptions} className="w-full" data-testid="decision-status-select" />} />
                        </CivicField>
                      </div>
                    </div>

                    <CivicField label={t("community_decisions.form.summary_label")} error={errors.summary?.message} helpText={t("community_decisions.form.summary_help")}>
                      <Controller
                        name="summary"
                        control={control}
                        rules={{
                          required: t("common.required"),
                          minLength: { value: FORM_LIMITS.decisions.summaryMin, message: t("community_decisions.form.summary_too_short") },
                          maxLength: { value: FORM_LIMITS.decisions.summaryMax, message: t("community_decisions.form.summary_too_long") },
                        }}
                        render={({ field, fieldState }) => (
                          <div className="flex flex-column gap-2">
                            <InputTextarea
                              {...field}
                              rows={4}
                              onChange={(e) => field.onChange(e.target.value)}
                              className={classNames("w-full", { "p-invalid": fieldState.error })}
                              maxLength={FORM_LIMITS.decisions.summaryMax}
                              data-testid="decision-summary-input"
                              placeholder={t("community_decisions.form.summary_placeholder")}
                            />
                            <CivicCharacterCount current={watchedSummary.length} max={FORM_LIMITS.decisions.summaryMax} min={FORM_LIMITS.decisions.summaryMin} />
                          </div>
                        )}
                      />
                    </CivicField>

                    <div className="grid">
                      <div className="col-12 md:col-6">
                        <CivicField label={t("community_decisions.form.proposal_label")} helpText={t("community_decisions.form.proposal_help")}>
                          <Controller name="linkedProposalId" control={control} render={({ field }) => <CivicSelect value={field.value} onChange={(e) => field.onChange(e.value)} options={proposalOptions} showClear placeholder={t("community_decisions.form.proposal_placeholder")} className="w-full" data-testid="decision-proposal-select" />} />
                        </CivicField>
                      </div>
                      <div className="col-12 md:col-6">
                        <CivicField label={t("community_decisions.form.governance_label")} helpText={t("community_decisions.form.governance_help")}>
                          <Controller name="governanceDocumentId" control={control} render={({ field }) => <CivicSelect value={field.value} onChange={(e) => field.onChange(e.value)} options={governanceOptions} showClear placeholder={t("community_decisions.form.governance_placeholder")} className="w-full" data-testid="decision-governance-select" />} />
                        </CivicField>
                      </div>
                    </div>

                    <div className="grid">
                      <div className="col-12 md:col-6">
                        <CivicField label={t("community_decisions.form.project_label")} helpText={t("community_decisions.form.project_help")}>
                          <Controller name="projectBoardId" control={control} render={({ field }) => <CivicSelect value={field.value} onChange={(e) => field.onChange(e.value)} options={projectOptions} showClear placeholder={t("community_decisions.form.project_placeholder")} className="w-full" data-testid="decision-project-select" />} />
                        </CivicField>
                      </div>
                      <div className="col-12 md:col-6">
                        <CivicField label={t("community_decisions.form.execution_owner_label")} error={errors.executionOwnerUsername?.message} helpText={t("community_decisions.form.execution_owner_help")}>
                          <Controller
                            name="executionOwnerUsername"
                            control={control}
                            rules={{ maxLength: { value: FORM_LIMITS.decisions.executionOwnerMax, message: t("community_decisions.form.execution_owner_too_long") } }}
                            render={({ field, fieldState }) => (
                              <InputText
                                {...field}
                                onChange={(e) => field.onChange(e.target.value)}
                                className={classNames("w-full", { "p-invalid": fieldState.error })}
                                maxLength={FORM_LIMITS.decisions.executionOwnerMax}
                                data-testid="decision-execution-owner-input"
                                placeholder={t("community_decisions.form.execution_owner_placeholder")}
                              />
                            )}
                          />
                        </CivicField>
                      </div>
                    </div>

                    <div className="grid">
                      <div className="col-12 md:col-6">
                        <CivicField label={t("community_decisions.form.basis_type_label")}>
                          <Controller name="approvalBasisType" control={control} render={({ field }) => <CivicSelect value={field.value} onChange={(e) => field.onChange(e.value)} options={basisOptions} className="w-full" data-testid="decision-basis-type-select" />} />
                        </CivicField>
                      </div>
                      <div className="col-12 md:col-6">
                        <CivicField label={t("community_decisions.form.decided_at_label")} helpText={t("community_decisions.form.decided_at_help")}>
                          <Controller name="decidedAt" control={control} render={({ field }) => <InputText {...field} type="datetime-local" onChange={(e) => field.onChange(e.target.value)} className="w-full" data-testid="decision-decided-at-input" />} />
                        </CivicField>
                      </div>
                    </div>

                    <CivicField
                      label={t("community_decisions.form.basis_summary_label")}
                      error={errors.approvalBasisSummary?.message}
                      helpText={t("community_decisions.form.basis_summary_help", { basis: t(`community_decisions.basis.${watchedApprovalBasisType}`) })}
                    >
                      <Controller
                        name="approvalBasisSummary"
                        control={control}
                        rules={{
                          required: t("common.required"),
                          minLength: { value: FORM_LIMITS.decisions.approvalBasisMin, message: t("community_decisions.form.basis_summary_too_short") },
                          maxLength: { value: FORM_LIMITS.decisions.approvalBasisMax, message: t("community_decisions.form.basis_summary_too_long") },
                        }}
                        render={({ field, fieldState }) => (
                          <div className="flex flex-column gap-2">
                            <InputTextarea
                              {...field}
                              rows={4}
                              onChange={(e) => field.onChange(e.target.value)}
                              className={classNames("w-full", { "p-invalid": fieldState.error })}
                              maxLength={FORM_LIMITS.decisions.approvalBasisMax}
                              data-testid="decision-basis-summary-input"
                              placeholder={t("community_decisions.form.basis_summary_placeholder")}
                            />
                            <CivicCharacterCount current={watchedApprovalBasisSummary.length} max={FORM_LIMITS.decisions.approvalBasisMax} min={FORM_LIMITS.decisions.approvalBasisMin} />
                          </div>
                        )}
                      />
                    </CivicField>

                    <CivicField label={t("community_decisions.form.effective_date_label")} helpText={t("community_decisions.form.effective_date_help")}>
                      <Controller name="effectiveDate" control={control} render={({ field }) => <InputText {...field} type="date" onChange={(e) => field.onChange(e.target.value)} className="w-full" data-testid="decision-effective-date-input" />} />
                    </CivicField>

                    <div className="u-surface-note">
                      <div className="u-meta-row mb-2">
                        <span>{t("community_decisions.preview.status_label")}</span>
                        <span>{t(`community_decisions.status.${watchedDecisionStatus}`)}</span>
                      </div>
                      <p className="text-sm text-secondary m-0 line-height-3">{t("community_decisions.preview.copy")}</p>
                    </div>

                    <div className="flex justify-content-end">
                      <CivicButton type="submit" icon="pi pi-check-square" label={t("community_decisions.create_action")} loading={isSubmitting} data-testid="decision-submit-button" />
                    </div>
                  </form>
                )}
              </CivicCard>
            </div>

            <div className="col-12 xl:col-8">
              {selectedDecision ? (
                <div className="flex flex-column gap-4" data-testid="community-decision-detail-card">
                  <CivicCard title={selectedDecision.title}>
                    <div className="civic-stat-grid civic-stat-grid-comfortable mb-4">
                      <CivicStatCard compact label={t("community_decisions.detail.type")} value={t(`community_decisions.types.${selectedDecision.decisionType}`)} />
                      <CivicStatCard compact label={t("community_decisions.detail.status")} value={t(`community_decisions.status.${selectedDecision.decisionStatus}`)} />
                      <CivicStatCard compact label={t("community_decisions.detail.decided_by")} value={selectedDecision.decidedByUsername} />
                      <CivicStatCard compact label={t("community_decisions.detail.execution_owner")} value={selectedDecision.executionOwnerUsername ?? t("community_decisions.execution_owner_none")} />
                    </div>
                    <div className="u-surface-note mb-4">
                      <div className="u-eyebrow mb-2">{t("community_decisions.form.summary_label")}</div>
                      <p className="text-secondary m-0 line-height-3">{selectedDecision.summary}</p>
                    </div>
                    <div className="u-surface-note mb-4">
                      <div className="flex justify-content-between align-items-start gap-3 flex-wrap mb-2">
                        <div className="u-eyebrow">{t("community_decisions.form.basis_summary_label")}</div>
                        <CivicBadge label={t(`community_decisions.basis.${selectedDecision.approvalBasisType}`)} severity="neutral" />
                      </div>
                      <p className="text-secondary m-0 line-height-3">{selectedDecision.approvalBasisSummary}</p>
                    </div>
                    <div className="civic-stat-grid civic-stat-grid-comfortable">
                      <CivicStatCard compact label={t("community_decisions.detail.decided_at")} value={formatDateTime(selectedDecision.decidedAt)} />
                      <CivicStatCard compact label={t("community_decisions.detail.effective_date")} value={formatDate(selectedDecision.effectiveDate)} />
                      <CivicStatCard compact label={t("community_decisions.detail.created_at")} value={formatDateTime(selectedDecision.createdAt)} />
                      <CivicStatCard compact label={t("community_decisions.detail.updated_at")} value={formatDateTime(selectedDecision.updatedAt)} />
                    </div>
                  </CivicCard>

                  <CivicCard title={t("community_decisions.related.title")} data-testid="community-decision-related-card">
                    {relatedLinks(selectedDecision).length > 0 ? (
                      <div className="flex flex-column gap-3">
                        {relatedLinks(selectedDecision).map((item) => (
                          <div className="u-surface-note" key={`${selectedDecision.id}-${item.key}`}>
                            <div className="flex justify-content-between align-items-start gap-3 flex-wrap">
                              <div className="min-w-0 flex-1">
                                <div className="u-eyebrow mb-2">{item.actionLabel}</div>
                                <p className="text-secondary m-0 line-height-3">{item.label}</p>
                              </div>
                              <CivicButton type="button" variant="ghost" size="small" icon="pi pi-arrow-right" label={item.actionLabel} onClick={item.onClick} />
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <CivicEmptyState icon="pi pi-link" title={t("community_decisions.related.empty_title")} description={t("community_decisions.related.empty_desc")} />
                    )}
                  </CivicCard>
                </div>
              ) : (
                <CivicCard>
                  <CivicEmptyState icon="pi pi-sitemap" title={t("community_decisions.empty_detail_title")} description={t("community_decisions.empty_detail_desc")} />
                </CivicCard>
              )}
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
