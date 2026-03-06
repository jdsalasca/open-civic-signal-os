import { useCallback, useEffect, useMemo, useState } from "react";
import { Controller, useFieldArray, useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
import { classNames } from "primereact/utils";
import apiClient from "../api/axios";
import { Layout } from "../components/Layout";
import { CivicActionBar } from "../components/ui/CivicActionBar";
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
import type { CommunityProposal, PageResponse, Signal } from "../types";
import { useTranslation } from "react-i18next";

type ApiError = Error & { friendlyMessage?: string };

type ProposalLink = {
  url: string;
};

type ProposalForm = {
  relatedSignalId: string | null;
  title: string;
  problemStatement: string;
  proposedSolution: string;
  estimatedCost: string;
  beneficiariesSummary: string;
  supportingLinks: ProposalLink[];
};

const defaultValues: ProposalForm = {
  relatedSignalId: null,
  title: "",
  problemStatement: "",
  proposedSolution: "",
  estimatedCost: "",
  beneficiariesSummary: "",
  supportingLinks: [{ url: "" }],
};

export function CommunityProposals() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { activeCommunityId, memberships } = useCommunityStore();
  const activeMembership = memberships.find((membership) => membership.communityId === activeCommunityId) ?? null;
  const [proposals, setProposals] = useState<CommunityProposal[]>([]);
  const [selectedProposalId, setSelectedProposalId] = useState<string | null>(null);
  const [signalOptions, setSignalOptions] = useState<Array<{ label: string; value: string }>>([]);
  const [loading, setLoading] = useState(false);

  const {
    control,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<ProposalForm>({
    mode: "onChange",
    defaultValues,
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: "supportingLinks",
  });

  const watchedTitle = watch("title") ?? "";
  const watchedProblem = watch("problemStatement") ?? "";
  const watchedSolution = watch("proposedSolution") ?? "";
  const watchedCost = watch("estimatedCost") ?? "";
  const watchedBeneficiaries = watch("beneficiariesSummary") ?? "";

  const selectedProposal = useMemo(
    () => proposals.find((proposal) => proposal.id === selectedProposalId) ?? proposals[0] ?? null,
    [proposals, selectedProposalId]
  );
  const getStatusLabel = useCallback(
    (status: string) => t(`community_proposals.status.${status}`),
    [t]
  );

  const loadProposals = useCallback(async () => {
    if (!activeCommunityId) {
      setProposals([]);
      setSelectedProposalId(null);
      return;
    }
    setLoading(true);
    try {
      const response = await apiClient.get<CommunityProposal[]>(`community/proposals?communityId=${activeCommunityId}`);
      setProposals(response.data ?? []);
      setSelectedProposalId((current) => {
        const next = response.data ?? [];
        if (next.length === 0) {
          return null;
        }
        return next.some((proposal) => proposal.id === current) ? current : next[0].id;
      });
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_proposals.load_error"));
    } finally {
      setLoading(false);
    }
  }, [activeCommunityId, t]);

  const loadSignals = useCallback(async () => {
    if (!activeCommunityId) {
      setSignalOptions([]);
      return;
    }
    try {
      const response = await apiClient.get<PageResponse<Signal>>("signals/prioritized", {
        params: { page: 0, size: 20 },
        headers: { "X-Community-Id": activeCommunityId },
      });
      setSignalOptions(
        (response.data?.content ?? []).map((signal) => ({
          label: signal.title,
          value: signal.id,
        }))
      );
    } catch {
      setSignalOptions([]);
    }
  }, [activeCommunityId]);

  useEffect(() => {
    loadProposals();
    loadSignals();
  }, [loadProposals, loadSignals]);

  const onSubmit = async (data: ProposalForm) => {
    if (!activeCommunityId) {
      toast.error(t("community_proposals.community_required"));
      return;
    }
    try {
      const response = await apiClient.post<CommunityProposal>("community/proposals", {
        communityId: activeCommunityId,
        relatedSignalId: data.relatedSignalId || null,
        title: data.title,
        problemStatement: data.problemStatement,
        proposedSolution: data.proposedSolution,
        estimatedCost: data.estimatedCost,
        beneficiariesSummary: data.beneficiariesSummary,
        supportingLinks: data.supportingLinks.map((item) => item.url.trim()).filter(Boolean),
      });
      toast.success(t("community_proposals.create_success"));
      reset(defaultValues);
      await loadProposals();
      setSelectedProposalId(response.data.id);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_proposals.create_error"));
    }
  };

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <div className="flex flex-column xl:flex-row justify-content-between align-items-start gap-4 mb-8">
          <CivicPageHeader
            title={t("community_proposals.title")}
            description={t("community_proposals.desc", {
              community: activeMembership?.communityName ?? t("dashboard.community_default"),
            })}
            className="mb-0"
          />
          <CivicActionBar className="w-full xl:w-auto">
            <div className="community-home-action-copy">
              <div className="u-eyebrow">{t("community_proposals.template_badge")}</div>
              <p className="u-section-copy text-sm m-0">{t("community_proposals.template_desc")}</p>
            </div>
            <div className="dashboard-action-cluster">
              <CivicButton
                type="button"
                icon="pi pi-comments"
                label={t("nav.dialogues")}
                variant="ghost"
                onClick={() => navigate("/communities/threads")}
              />
              <CivicButton
                type="button"
                icon="pi pi-map"
                label={t("community_map.open_map")}
                variant="secondary"
                onClick={() => navigate("/communities/map")}
              />
            </div>
          </CivicActionBar>
        </div>

        {!activeCommunityId ? (
          <CivicCard>
            <CivicEmptyState
              icon="pi-users"
              title={t("community_proposals.no_context_title")}
              description={t("community_proposals.no_context_desc")}
              actionLabel={t("nav.communities")}
              onAction={() => navigate("/communities")}
            />
          </CivicCard>
        ) : (
          <div className="grid">
            <div className="col-12 xl:col-5">
              <CivicCard title={t("community_proposals.create_title")} className="mb-6" data-testid="community-proposals-create-card">
                <form className="flex flex-column gap-3" onSubmit={handleSubmit(onSubmit)}>
                  <div className="civic-stat-grid civic-stat-grid-comfortable mb-4" data-testid="community-proposals-structure-grid">
                    <CivicStatCard compact label={t("community_proposals.stats.problem")} value={t("community_proposals.stats.required")} />
                    <CivicStatCard compact label={t("community_proposals.stats.solution")} value={t("community_proposals.stats.required")} />
                    <CivicStatCard compact label={t("community_proposals.stats.cost")} value={t("community_proposals.stats.required")} />
                    <CivicStatCard compact label={t("community_proposals.stats.people")} value={t("community_proposals.stats.required")} />
                  </div>

                  <CivicField label={t("community_proposals.related_signal_label")} helpText={t("community_proposals.related_signal_help")}>
                    <Controller
                      name="relatedSignalId"
                      control={control}
                      render={({ field }) => (
                        <CivicSelect
                          value={field.value}
                          onChange={(e) => field.onChange(e.value)}
                          options={signalOptions}
                          placeholder={t("community_proposals.related_signal_placeholder")}
                          showClear
                          className="w-full"
                          data-testid="proposal-related-signal-select"
                        />
                      )}
                    />
                  </CivicField>

                  <CivicField label={t("community_proposals.title_label")} error={errors.title?.message} helpText={t("community_proposals.title_help")}>
                    <Controller
                      name="title"
                      control={control}
                      rules={{
                        required: t("common.required"),
                        minLength: { value: FORM_LIMITS.proposals.titleMin, message: t("community_proposals.title_too_short") },
                        maxLength: { value: FORM_LIMITS.proposals.titleMax, message: t("community_proposals.title_too_long") },
                      }}
                      render={({ field, fieldState }) => (
                        <div className="flex flex-column gap-2">
                          <InputText
                            {...field}
                            onChange={(e) => field.onChange(e.target.value)}
                            className={classNames("w-full", { "p-invalid": fieldState.error })}
                            maxLength={FORM_LIMITS.proposals.titleMax}
                            data-testid="proposal-title-input"
                            placeholder={t("community_proposals.title_placeholder")}
                          />
                          <CivicCharacterCount current={watchedTitle.length} max={FORM_LIMITS.proposals.titleMax} min={FORM_LIMITS.proposals.titleMin} />
                        </div>
                      )}
                    />
                  </CivicField>

                  <CivicField label={t("community_proposals.problem_label")} error={errors.problemStatement?.message} helpText={t("community_proposals.problem_help")}>
                    <Controller
                      name="problemStatement"
                      control={control}
                      rules={{
                        required: t("common.required"),
                        minLength: { value: FORM_LIMITS.proposals.sectionMin, message: t("community_proposals.section_too_short") },
                        maxLength: { value: FORM_LIMITS.proposals.sectionMax, message: t("community_proposals.section_too_long") },
                      }}
                      render={({ field, fieldState }) => (
                        <div className="flex flex-column gap-2">
                          <InputTextarea
                            {...field}
                            rows={5}
                            onChange={(e) => field.onChange(e.target.value)}
                            className={classNames("w-full", { "p-invalid": fieldState.error })}
                            maxLength={FORM_LIMITS.proposals.sectionMax}
                            data-testid="proposal-problem-input"
                            placeholder={t("community_proposals.problem_placeholder")}
                          />
                          <CivicCharacterCount current={watchedProblem.length} max={FORM_LIMITS.proposals.sectionMax} min={FORM_LIMITS.proposals.sectionMin} />
                        </div>
                      )}
                    />
                  </CivicField>

                  <CivicField label={t("community_proposals.solution_label")} error={errors.proposedSolution?.message} helpText={t("community_proposals.solution_help")}>
                    <Controller
                      name="proposedSolution"
                      control={control}
                      rules={{
                        required: t("common.required"),
                        minLength: { value: FORM_LIMITS.proposals.sectionMin, message: t("community_proposals.section_too_short") },
                        maxLength: { value: FORM_LIMITS.proposals.sectionMax, message: t("community_proposals.section_too_long") },
                      }}
                      render={({ field, fieldState }) => (
                        <div className="flex flex-column gap-2">
                          <InputTextarea
                            {...field}
                            rows={5}
                            onChange={(e) => field.onChange(e.target.value)}
                            className={classNames("w-full", { "p-invalid": fieldState.error })}
                            maxLength={FORM_LIMITS.proposals.sectionMax}
                            data-testid="proposal-solution-input"
                            placeholder={t("community_proposals.solution_placeholder")}
                          />
                          <CivicCharacterCount current={watchedSolution.length} max={FORM_LIMITS.proposals.sectionMax} min={FORM_LIMITS.proposals.sectionMin} />
                        </div>
                      )}
                    />
                  </CivicField>

                  <CivicField label={t("community_proposals.cost_label")} error={errors.estimatedCost?.message} helpText={t("community_proposals.cost_help")}>
                    <Controller
                      name="estimatedCost"
                      control={control}
                      rules={{
                        required: t("common.required"),
                        minLength: { value: FORM_LIMITS.proposals.costMin, message: t("community_proposals.cost_too_short") },
                        maxLength: { value: FORM_LIMITS.proposals.costMax, message: t("community_proposals.cost_too_long") },
                      }}
                      render={({ field, fieldState }) => (
                        <div className="flex flex-column gap-2">
                          <InputTextarea
                            {...field}
                            rows={4}
                            onChange={(e) => field.onChange(e.target.value)}
                            className={classNames("w-full", { "p-invalid": fieldState.error })}
                            maxLength={FORM_LIMITS.proposals.costMax}
                            data-testid="proposal-cost-input"
                            placeholder={t("community_proposals.cost_placeholder")}
                          />
                          <CivicCharacterCount current={watchedCost.length} max={FORM_LIMITS.proposals.costMax} min={FORM_LIMITS.proposals.costMin} />
                        </div>
                      )}
                    />
                  </CivicField>

                  <CivicField label={t("community_proposals.beneficiaries_label")} error={errors.beneficiariesSummary?.message} helpText={t("community_proposals.beneficiaries_help")}>
                    <Controller
                      name="beneficiariesSummary"
                      control={control}
                      rules={{
                        required: t("common.required"),
                        minLength: { value: FORM_LIMITS.proposals.beneficiariesMin, message: t("community_proposals.beneficiaries_too_short") },
                        maxLength: { value: FORM_LIMITS.proposals.beneficiariesMax, message: t("community_proposals.beneficiaries_too_long") },
                      }}
                      render={({ field, fieldState }) => (
                        <div className="flex flex-column gap-2">
                          <InputTextarea
                            {...field}
                            rows={4}
                            onChange={(e) => field.onChange(e.target.value)}
                            className={classNames("w-full", { "p-invalid": fieldState.error })}
                            maxLength={FORM_LIMITS.proposals.beneficiariesMax}
                            data-testid="proposal-beneficiaries-input"
                            placeholder={t("community_proposals.beneficiaries_placeholder")}
                          />
                          <CivicCharacterCount current={watchedBeneficiaries.length} max={FORM_LIMITS.proposals.beneficiariesMax} min={FORM_LIMITS.proposals.beneficiariesMin} />
                        </div>
                      )}
                    />
                  </CivicField>

                  <CivicCard title={t("community_proposals.links_title")} className="mb-0">
                    <div className="flex flex-column gap-4">
                      <p className="text-sm text-secondary m-0">{t("community_proposals.links_help")}</p>
                      {fields.map((item, index) => (
                        <CivicField
                          key={item.id}
                          label={t("community_proposals.link_item_label", { index: index + 1 })}
                          error={errors.supportingLinks?.[index]?.url?.message}
                        >
                          <div className="flex gap-2 align-items-start">
                            <Controller
                              name={`supportingLinks.${index}.url`}
                              control={control}
                              rules={{
                                validate: (value) => {
                                  const trimmed = (value ?? "").trim();
                                  if (!trimmed) {
                                    return true;
                                  }
                                  if (trimmed.length > FORM_LIMITS.proposals.linkMax) {
                                    return t("community_proposals.link_too_long");
                                  }
                                  return /^https?:\/\//.test(trimmed) || t("community_proposals.link_invalid");
                                },
                              }}
                              render={({ field, fieldState }) => (
                                <InputText
                                  {...field}
                                  onChange={(e) => field.onChange(e.target.value)}
                                  className={classNames("w-full", { "p-invalid": fieldState.error })}
                                  maxLength={FORM_LIMITS.proposals.linkMax}
                                  data-testid={`proposal-link-input-${index}`}
                                  placeholder={t("community_proposals.link_placeholder")}
                                />
                              )}
                            />
                            {fields.length > 1 && (
                              <CivicButton
                                type="button"
                                icon="pi pi-trash"
                                variant="ghost"
                                onClick={() => remove(index)}
                                data-testid={`proposal-remove-link-${index}`}
                              />
                            )}
                          </div>
                        </CivicField>
                      ))}
                      <div className="flex justify-content-between align-items-center flex-wrap gap-3">
                        <small className="text-muted text-xs">{t("community_proposals.links_limit", { count: FORM_LIMITS.proposals.linksMax })}</small>
                        <CivicButton
                          type="button"
                          icon="pi pi-plus"
                          variant="ghost"
                          label={t("community_proposals.add_link")}
                          onClick={() => append({ url: "" })}
                          disabled={fields.length >= FORM_LIMITS.proposals.linksMax}
                          data-testid="proposal-add-link-button"
                        />
                      </div>
                    </div>
                  </CivicCard>

                  <div className="flex justify-content-end">
                    <CivicButton
                      type="submit"
                      icon="pi pi-megaphone"
                      label={t("community_proposals.create_action")}
                      loading={isSubmitting}
                      data-testid="proposal-submit-button"
                    />
                  </div>
                </form>
              </CivicCard>
            </div>

            <div className="col-12 xl:col-7">
              <CivicCard title={t("community_proposals.list_title")} data-testid="community-proposals-list-card">
                {loading ? (
                  <p className="text-secondary m-0">{t("common.loading")}</p>
                ) : proposals.length === 0 ? (
                  <CivicEmptyState
                    icon="pi-file-edit"
                    title={t("community_proposals.empty_title")}
                    description={t("community_proposals.empty_desc")}
                    actionLabel={t("community_proposals.empty_action")}
                    onAction={() => window.scrollTo({ top: 0, behavior: "smooth" })}
                  />
                ) : (
                  <div className="grid">
                    <div className="col-12 lg:col-5">
                      <div className="flex flex-column gap-3">
                        {proposals.map((proposal) => (
                          <button
                            key={proposal.id}
                            type="button"
                            className={classNames("community-feed-list-card text-left border-none bg-transparent p-0 cursor-pointer", {
                              "border-brand-primary": proposal.id === selectedProposal?.id,
                            })}
                            onClick={() => setSelectedProposalId(proposal.id)}
                            data-testid={`community-proposal-row-${proposal.id}`}
                          >
                            <div className="flex flex-column gap-3">
                              <div className="flex justify-content-between gap-3 align-items-start">
                                <div className="min-w-0 flex-1">
                                  <div className="u-eyebrow">{t("community_proposals.template_badge")}</div>
                                  <h3 className="text-lg font-black text-main m-0 mt-2">{proposal.title}</h3>
                                </div>
                                <span className="u-pill">{getStatusLabel(proposal.status)}</span>
                              </div>
                              <p className="text-sm text-secondary m-0 line-height-3">{proposal.problemStatement}</p>
                              <div className="u-meta-row">
                                <span>{proposal.authorUsername}</span>
                                <span>{new Date(proposal.updatedAt).toLocaleDateString()}</span>
                              </div>
                            </div>
                          </button>
                        ))}
                      </div>
                    </div>

                    <div className="col-12 lg:col-7">
                      {selectedProposal ? (
                        <div className="flex flex-column gap-4" data-testid="community-proposal-detail-card">
                          <CivicCard title={selectedProposal.title}>
                            <div className="civic-stat-grid civic-stat-grid-comfortable mb-4">
                              <CivicStatCard compact label={t("community_proposals.detail_author")} value={selectedProposal.authorUsername} />
                              <CivicStatCard compact label={t("community_proposals.detail_status")} value={getStatusLabel(selectedProposal.status)} />
                              <CivicStatCard compact label={t("community_proposals.detail_template")} value={t("community_proposals.template_short")} />
                              <CivicStatCard compact label={t("community_proposals.detail_links")} value={selectedProposal.supportingLinks.length} />
                            </div>
                            <div className="flex flex-column gap-4">
                              <div className="u-surface-note">
                                <div className="u-eyebrow mb-2">{t("community_proposals.problem_label")}</div>
                                <p className="text-secondary m-0 line-height-3">{selectedProposal.problemStatement}</p>
                              </div>
                              <div className="u-surface-note">
                                <div className="u-eyebrow mb-2">{t("community_proposals.solution_label")}</div>
                                <p className="text-secondary m-0 line-height-3">{selectedProposal.proposedSolution}</p>
                              </div>
                              <div className="u-surface-note">
                                <div className="u-eyebrow mb-2">{t("community_proposals.cost_label")}</div>
                                <p className="text-secondary m-0 line-height-3">{selectedProposal.estimatedCost}</p>
                              </div>
                              <div className="u-surface-note">
                                <div className="u-eyebrow mb-2">{t("community_proposals.beneficiaries_label")}</div>
                                <p className="text-secondary m-0 line-height-3">{selectedProposal.beneficiariesSummary}</p>
                              </div>
                              {selectedProposal.relatedSignalId && (
                                <div className="u-surface-note">
                                  <div className="u-eyebrow mb-2">{t("community_proposals.related_signal_label")}</div>
                                  <button
                                    type="button"
                                    className="border-none bg-transparent p-0 text-left text-brand-primary font-bold cursor-pointer"
                                    onClick={() => navigate(`/signal/${selectedProposal.relatedSignalId}`)}
                                    data-testid="community-proposal-related-signal-link"
                                  >
                                    {selectedProposal.relatedSignalTitle}
                                  </button>
                                </div>
                              )}
                              <div className="u-surface-note">
                                <div className="u-eyebrow mb-2">{t("community_proposals.links_title")}</div>
                                {selectedProposal.supportingLinks.length > 0 ? (
                                  <div className="flex flex-column gap-2">
                                    {selectedProposal.supportingLinks.map((link, index) => (
                                      <a
                                        key={link}
                                        href={link}
                                        target="_blank"
                                        rel="noreferrer"
                                        className="text-brand-primary font-semibold break-all"
                                        data-testid={`community-proposal-link-${index}`}
                                      >
                                        {link}
                                      </a>
                                    ))}
                                  </div>
                                ) : (
                                  <p className="text-secondary m-0">{t("community_proposals.no_links")}</p>
                                )}
                              </div>
                            </div>
                          </CivicCard>
                        </div>
                      ) : null}
                    </div>
                  </div>
                )}
              </CivicCard>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
