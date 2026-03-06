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
  GovernanceDocument,
  GovernanceDocumentType,
  GovernanceDocumentVersion,
  GovernanceDocumentVisibility,
} from "../types";

type ApiError = Error & { friendlyMessage?: string };

type GovernanceDocumentForm = {
  title: string;
  summary: string;
  documentType: GovernanceDocumentType;
  visibility: GovernanceDocumentVisibility;
  tagsInput: string;
  content: string;
  changeSummary: string;
  sourceUrl: string;
  effectiveDate: string;
  meetingDate: string;
};

type GovernanceVersionForm = {
  content: string;
  changeSummary: string;
  sourceUrl: string;
  effectiveDate: string;
  meetingDate: string;
};

const defaultDocumentValues: GovernanceDocumentForm = {
  title: "",
  summary: "",
  documentType: "AGREEMENT",
  visibility: "COMMUNITY",
  tagsInput: "",
  content: "",
  changeSummary: "",
  sourceUrl: "",
  effectiveDate: "",
  meetingDate: "",
};

const defaultVersionValues: GovernanceVersionForm = {
  content: "",
  changeSummary: "",
  sourceUrl: "",
  effectiveDate: "",
  meetingDate: "",
};

export function CommunityGovernance() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { activeCommunityId, memberships } = useCommunityStore();
  const activeMembership = memberships.find((membership) => membership.communityId === activeCommunityId) ?? null;
  const [documents, setDocuments] = useState<GovernanceDocument[]>([]);
  const [selectedDocumentId, setSelectedDocumentId] = useState<string | null>(null);
  const [loadingDocuments, setLoadingDocuments] = useState(false);
  const [query, setQuery] = useState("");
  const [documentTypeFilter, setDocumentTypeFilter] = useState<GovernanceDocumentType | "ALL">("ALL");
  const [visibilityFilter, setVisibilityFilter] = useState<GovernanceDocumentVisibility | "ALL">("ALL");

  const {
    control,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<GovernanceDocumentForm>({
    mode: "onChange",
    defaultValues: defaultDocumentValues,
  });

  const {
    control: versionControl,
    handleSubmit: handleVersionSubmit,
    reset: resetVersion,
    watch: watchVersion,
    formState: { errors: versionErrors, isSubmitting: isSubmittingVersion },
  } = useForm<GovernanceVersionForm>({
    mode: "onChange",
    defaultValues: defaultVersionValues,
  });

  const selectedDocument = useMemo(
    () => documents.find((document) => document.id === selectedDocumentId) ?? documents[0] ?? null,
    [documents, selectedDocumentId]
  );

  const canManageGovernance =
    activeMembership?.role === "COORDINATOR" || activeMembership?.role === "PUBLIC_SERVANT_LIAISON";

  const watchedTitle = watch("title") ?? "";
  const watchedSummary = watch("summary") ?? "";
  const watchedContent = watch("content") ?? "";
  const watchedChangeSummary = watch("changeSummary") ?? "";
  const watchedVersionContent = watchVersion("content") ?? "";
  const watchedVersionChangeSummary = watchVersion("changeSummary") ?? "";

  const documentTypeOptions = useMemo(
    () => [
      { label: t("community_governance.filters.type_all"), value: "ALL" },
      { label: t("community_governance.types.STATUTE"), value: "STATUTE" },
      { label: t("community_governance.types.REGULATION"), value: "REGULATION" },
      { label: t("community_governance.types.MINUTES"), value: "MINUTES" },
      { label: t("community_governance.types.AGREEMENT"), value: "AGREEMENT" },
      { label: t("community_governance.types.BUDGET"), value: "BUDGET" },
      { label: t("community_governance.types.REPORT"), value: "REPORT" },
    ],
    [t]
  );

  const visibilityOptions = useMemo(
    () => [
      { label: t("community_governance.filters.visibility_all"), value: "ALL" },
      { label: t("community_governance.visibility.PUBLIC"), value: "PUBLIC" },
      { label: t("community_governance.visibility.COMMUNITY"), value: "COMMUNITY" },
      { label: t("community_governance.visibility.ADMINS"), value: "ADMINS" },
    ],
    [t]
  );

  const creationTypeOptions = documentTypeOptions.filter((option) => option.value !== "ALL");
  const creationVisibilityOptions = visibilityOptions.filter((option) => option.value !== "ALL");

  const loadDocuments = useCallback(async () => {
    if (!activeCommunityId) {
      setDocuments([]);
      setSelectedDocumentId(null);
      return;
    }
    setLoadingDocuments(true);
    try {
      const params = new URLSearchParams({ communityId: activeCommunityId });
      if (query.trim()) params.set("query", query.trim());
      if (documentTypeFilter !== "ALL") params.set("documentType", documentTypeFilter);
      if (visibilityFilter !== "ALL") params.set("visibility", visibilityFilter);
      const response = await apiClient.get<GovernanceDocument[]>(`community/governance?${params.toString()}`);
      const nextDocuments = response.data ?? [];
      setDocuments(nextDocuments);
      setSelectedDocumentId((current) => {
        if (nextDocuments.length === 0) return null;
        return nextDocuments.some((document) => document.id === current) ? current : nextDocuments[0].id;
      });
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_governance.load_error"));
    } finally {
      setLoadingDocuments(false);
    }
  }, [activeCommunityId, documentTypeFilter, query, t, visibilityFilter]);

  useEffect(() => {
    loadDocuments();
  }, [loadDocuments]);

  const normalizeTags = (rawValue: string) =>
    rawValue.split(",").map((tag) => tag.trim()).filter(Boolean).slice(0, FORM_LIMITS.governance.tagsMax);

  const onSubmitDocument = async (data: GovernanceDocumentForm) => {
    if (!activeCommunityId) {
      toast.error(t("community_governance.community_required"));
      return;
    }
    try {
      const response = await apiClient.post<GovernanceDocument>("community/governance", {
        communityId: activeCommunityId,
        title: data.title,
        summary: data.summary,
        documentType: data.documentType,
        visibility: data.visibility,
        tags: normalizeTags(data.tagsInput),
        content: data.content,
        changeSummary: data.changeSummary,
        sourceUrl: data.sourceUrl.trim() || null,
        effectiveDate: data.effectiveDate || null,
        meetingDate: data.meetingDate || null,
      });
      toast.success(t("community_governance.create_success"));
      reset(defaultDocumentValues);
      await loadDocuments();
      setSelectedDocumentId(response.data.id);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_governance.create_error"));
    }
  };

  const onSubmitVersion = async (data: GovernanceVersionForm) => {
    if (!selectedDocument) return;
    try {
      const response = await apiClient.post<GovernanceDocument>(`community/governance/${selectedDocument.id}/versions`, {
        content: data.content,
        changeSummary: data.changeSummary,
        sourceUrl: data.sourceUrl.trim() || null,
        effectiveDate: data.effectiveDate || null,
        meetingDate: data.meetingDate || null,
      });
      setDocuments((current) => current.map((document) => (document.id === response.data.id ? response.data : document)));
      setSelectedDocumentId(response.data.id);
      resetVersion(defaultVersionValues);
      toast.success(t("community_governance.version_success"));
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_governance.version_error"));
    }
  };

  const currentTagPreview = normalizeTags(watch("tagsInput") ?? "");
  const versionMeta = (version: GovernanceDocumentVersion) =>
    [version.effectiveDate, version.meetingDate, new Date(version.createdAt).toLocaleDateString()].filter(Boolean).join(" · ");

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <div className="flex flex-column xl:flex-row justify-content-between align-items-start gap-4 mb-8">
          <CivicPageHeader
            title={t("community_governance.title")}
            description={t("community_governance.desc", {
              community: activeMembership?.communityName ?? t("dashboard.community_default"),
            })}
            className="mb-0"
          />
          <CivicActionBar className="w-full xl:w-auto">
            <div className="community-home-action-copy">
              <div className="u-eyebrow">{t("community_governance.badge")}</div>
              <p className="u-section-copy text-sm m-0">{t("community_governance.badge_desc")}</p>
            </div>
            <div className="dashboard-action-cluster">
              <CivicButton type="button" icon="pi pi-briefcase" label={t("nav.community_projects")} variant="ghost" onClick={() => navigate("/communities/projects")} />
              <CivicButton type="button" icon="pi pi-file-edit" label={t("community_proposals.open_hub")} variant="ghost" onClick={() => navigate("/communities/proposals")} />
            </div>
          </CivicActionBar>
        </div>

        {!activeCommunityId ? (
          <CivicCard>
            <CivicEmptyState
              icon="pi-book"
              title={t("community_governance.no_context_title")}
              description={t("community_governance.no_context_desc")}
              actionLabel={t("nav.communities")}
              onAction={() => navigate("/communities")}
            />
          </CivicCard>
        ) : (
          <div className="grid">
            <div className="col-12 xl:col-4">
              <CivicCard title={t("community_governance.search_title")} className="mb-6" data-testid="governance-filter-card">
                <div className="flex flex-column gap-3">
                  <CivicField label={t("community_governance.filters.query_label")}>
                    <InputText value={query} onChange={(e) => setQuery(e.target.value)} className="w-full" data-testid="governance-query-input" placeholder={t("community_governance.filters.query_placeholder")} />
                  </CivicField>
                  <CivicField label={t("community_governance.filters.type_label")}>
                    <CivicSelect value={documentTypeFilter} onChange={(e) => setDocumentTypeFilter(e.value)} options={documentTypeOptions} className="w-full" data-testid="governance-type-filter" />
                  </CivicField>
                  <CivicField label={t("community_governance.filters.visibility_label")}>
                    <CivicSelect value={visibilityFilter} onChange={(e) => setVisibilityFilter(e.value)} options={visibilityOptions} className="w-full" data-testid="governance-visibility-filter" />
                  </CivicField>
                  <div className="flex justify-content-end">
                    <CivicButton type="button" icon="pi pi-search" label={t("community_governance.filters.apply")} onClick={loadDocuments} data-testid="governance-apply-filters" />
                  </div>
                </div>
              </CivicCard>

              <CivicCard title={t("community_governance.library_title")} className="mb-6" data-testid="governance-list-card">
                {loadingDocuments ? (
                  <p className="text-secondary m-0">{t("common.loading")}</p>
                ) : documents.length === 0 ? (
                  <CivicEmptyState icon="pi-book" title={t("community_governance.empty_title")} description={t("community_governance.empty_desc")} />
                ) : (
                  <div className="flex flex-column gap-3">
                    {documents.map((document) => (
                      <button key={document.id} type="button" className="community-feed-list-card text-left border-none bg-transparent p-0 cursor-pointer" onClick={() => setSelectedDocumentId(document.id)} data-testid={`governance-document-row-${document.id}`}>
                        <div className="flex justify-content-between gap-3 align-items-start flex-wrap">
                          <div className="min-w-0 flex-1">
                            <div className="u-eyebrow">{t(`community_governance.types.${document.documentType}`)}</div>
                            <h3 className="text-lg font-black text-main m-0 mt-2">{document.title}</h3>
                            <p className="text-sm text-secondary mt-2 mb-0 line-height-3">{document.summary}</p>
                          </div>
                          <CivicBadge label={t(`community_governance.visibility.${document.visibility}`)} severity="neutral" />
                        </div>
                      </button>
                    ))}
                  </div>
                )}
              </CivicCard>

              <CivicCard title={t("community_governance.create_title")} data-testid="governance-create-card">
                {!canManageGovernance ? (
                  <CivicEmptyState icon="pi-lock" title={t("community_governance.manage_locked_title")} description={t("community_governance.manage_locked_desc")} />
                ) : (
                  <form className="flex flex-column gap-3" onSubmit={handleSubmit(onSubmitDocument)}>
                    <CivicField label={t("community_governance.form.title_label")} error={errors.title?.message}>
                      <Controller
                        name="title"
                        control={control}
                        rules={{ required: t("common.required"), minLength: { value: FORM_LIMITS.governance.titleMin, message: t("community_governance.form.title_too_short") }, maxLength: { value: FORM_LIMITS.governance.titleMax, message: t("community_governance.form.title_too_long") } }}
                        render={({ field, fieldState }) => (
                          <div className="flex flex-column gap-2">
                            <InputText {...field} onChange={(e) => field.onChange(e.target.value)} className={classNames("w-full", { "p-invalid": fieldState.error })} maxLength={FORM_LIMITS.governance.titleMax} data-testid="governance-title-input" />
                            <CivicCharacterCount current={watchedTitle.length} max={FORM_LIMITS.governance.titleMax} min={FORM_LIMITS.governance.titleMin} />
                          </div>
                        )}
                      />
                    </CivicField>

                    <CivicField label={t("community_governance.form.summary_label")} error={errors.summary?.message}>
                      <Controller
                        name="summary"
                        control={control}
                        rules={{ required: t("common.required"), minLength: { value: FORM_LIMITS.governance.summaryMin, message: t("community_governance.form.summary_too_short") }, maxLength: { value: FORM_LIMITS.governance.summaryMax, message: t("community_governance.form.summary_too_long") } }}
                        render={({ field, fieldState }) => (
                          <div className="flex flex-column gap-2">
                            <InputTextarea {...field} rows={3} onChange={(e) => field.onChange(e.target.value)} className={classNames("w-full", { "p-invalid": fieldState.error })} maxLength={FORM_LIMITS.governance.summaryMax} data-testid="governance-summary-input" />
                            <CivicCharacterCount current={watchedSummary.length} max={FORM_LIMITS.governance.summaryMax} min={FORM_LIMITS.governance.summaryMin} />
                          </div>
                        )}
                      />
                    </CivicField>

                    <div className="grid">
                      <div className="col-12 md:col-6">
                        <CivicField label={t("community_governance.form.type_label")}>
                          <Controller name="documentType" control={control} render={({ field }) => <CivicSelect value={field.value} onChange={(e) => field.onChange(e.value)} options={creationTypeOptions} className="w-full" data-testid="governance-document-type-select" />} />
                        </CivicField>
                      </div>
                      <div className="col-12 md:col-6">
                        <CivicField label={t("community_governance.form.visibility_label")}>
                          <Controller name="visibility" control={control} render={({ field }) => <CivicSelect value={field.value} onChange={(e) => field.onChange(e.value)} options={creationVisibilityOptions} className="w-full" data-testid="governance-visibility-select" />} />
                        </CivicField>
                      </div>
                    </div>
                    <CivicField label={t("community_governance.form.tags_label")} helpText={t("community_governance.form.tags_help")}>
                      <Controller
                        name="tagsInput"
                        control={control}
                        render={({ field }) => (
                          <div className="flex flex-column gap-2">
                            <InputText {...field} onChange={(e) => field.onChange(e.target.value)} className="w-full" data-testid="governance-tags-input" placeholder={t("community_governance.form.tags_placeholder")} />
                            {currentTagPreview.length > 0 && (
                              <div className="flex flex-wrap gap-2">
                                {currentTagPreview.map((tag) => <span className="u-pill" key={tag}>{tag}</span>)}
                              </div>
                            )}
                          </div>
                        )}
                      />
                    </CivicField>

                    <CivicField label={t("community_governance.form.content_label")} error={errors.content?.message}>
                      <Controller
                        name="content"
                        control={control}
                        rules={{ required: t("common.required"), minLength: { value: FORM_LIMITS.governance.contentMin, message: t("community_governance.form.content_too_short") }, maxLength: { value: FORM_LIMITS.governance.contentMax, message: t("community_governance.form.content_too_long") } }}
                        render={({ field, fieldState }) => (
                          <div className="flex flex-column gap-2">
                            <InputTextarea {...field} rows={7} onChange={(e) => field.onChange(e.target.value)} className={classNames("w-full", { "p-invalid": fieldState.error })} maxLength={FORM_LIMITS.governance.contentMax} data-testid="governance-content-input" />
                            <CivicCharacterCount current={watchedContent.length} max={FORM_LIMITS.governance.contentMax} min={FORM_LIMITS.governance.contentMin} />
                          </div>
                        )}
                      />
                    </CivicField>

                    <CivicField label={t("community_governance.form.change_summary_label")} error={errors.changeSummary?.message}>
                      <Controller
                        name="changeSummary"
                        control={control}
                        rules={{ required: t("common.required"), minLength: { value: FORM_LIMITS.governance.changeSummaryMin, message: t("community_governance.form.change_summary_too_short") }, maxLength: { value: FORM_LIMITS.governance.changeSummaryMax, message: t("community_governance.form.change_summary_too_long") } }}
                        render={({ field, fieldState }) => (
                          <div className="flex flex-column gap-2">
                            <InputText {...field} onChange={(e) => field.onChange(e.target.value)} className={classNames("w-full", { "p-invalid": fieldState.error })} maxLength={FORM_LIMITS.governance.changeSummaryMax} data-testid="governance-change-summary-input" />
                            <CivicCharacterCount current={watchedChangeSummary.length} max={FORM_LIMITS.governance.changeSummaryMax} min={FORM_LIMITS.governance.changeSummaryMin} />
                          </div>
                        )}
                      />
                    </CivicField>

                    <div className="grid">
                      <div className="col-12">
                        <CivicField label={t("community_governance.form.source_url_label")}>
                          <Controller name="sourceUrl" control={control} render={({ field }) => <InputText {...field} onChange={(e) => field.onChange(e.target.value)} className="w-full" data-testid="governance-source-url-input" />} />
                        </CivicField>
                      </div>
                      <div className="col-12 md:col-6">
                        <CivicField label={t("community_governance.form.effective_date_label")}>
                          <Controller name="effectiveDate" control={control} render={({ field }) => <InputText {...field} type="date" onChange={(e) => field.onChange(e.target.value)} className="w-full" data-testid="governance-effective-date-input" />} />
                        </CivicField>
                      </div>
                      <div className="col-12 md:col-6">
                        <CivicField label={t("community_governance.form.meeting_date_label")}>
                          <Controller name="meetingDate" control={control} render={({ field }) => <InputText {...field} type="date" onChange={(e) => field.onChange(e.target.value)} className="w-full" data-testid="governance-meeting-date-input" />} />
                        </CivicField>
                      </div>
                    </div>

                    <div className="flex justify-content-end">
                      <CivicButton type="submit" icon="pi pi-book" label={t("community_governance.create_action")} loading={isSubmitting} data-testid="governance-submit-button" />
                    </div>
                  </form>
                )}
              </CivicCard>
            </div>

            <div className="col-12 xl:col-8">
              {selectedDocument ? (
                <div className="flex flex-column gap-4" data-testid="governance-detail-card">
                  <CivicCard title={selectedDocument.title}>
                    <div className="civic-stat-grid civic-stat-grid-comfortable mb-4">
                      <CivicStatCard compact label={t("community_governance.stats.type")} value={t(`community_governance.types.${selectedDocument.documentType}`)} />
                      <CivicStatCard compact label={t("community_governance.stats.visibility")} value={t(`community_governance.visibility.${selectedDocument.visibility}`)} />
                      <CivicStatCard compact label={t("community_governance.stats.version")} value={`v${selectedDocument.currentVersionNumber}`} />
                      <CivicStatCard compact label={t("community_governance.stats.author")} value={selectedDocument.authorUsername} />
                    </div>
                    <div className="u-surface-note mb-4">
                      <p className="text-secondary m-0 line-height-3">{selectedDocument.summary}</p>
                    </div>
                    <div className="flex flex-wrap gap-2 mb-4">
                      {selectedDocument.tags.map((tag) => <span key={tag} className="u-pill">{tag}</span>)}
                    </div>
                    {selectedDocument.currentVersion && (
                      <div className="u-surface-note">
                        <div className="u-meta-row mb-2">
                          <span>{t("community_governance.current_version_title", { version: selectedDocument.currentVersion.versionNumber })}</span>
                          <span>{versionMeta(selectedDocument.currentVersion)}</span>
                        </div>
                        <p className="text-secondary m-0 line-height-3 white-space-pre-wrap">{selectedDocument.currentVersion.content}</p>
                        <p className="text-sm text-muted mt-3 mb-0 line-height-3">{selectedDocument.currentVersion.changeSummary}</p>
                        {selectedDocument.currentVersion.sourceUrl && (
                          <a href={selectedDocument.currentVersion.sourceUrl} target="_blank" rel="noreferrer" className="text-brand-primary font-semibold break-all inline-block mt-3" data-testid="governance-current-version-link">
                            {selectedDocument.currentVersion.sourceUrl}
                          </a>
                        )}
                      </div>
                    )}
                  </CivicCard>

                  <CivicCard title={t("community_governance.versions_title")} data-testid="governance-versions-card">
                    <div className="flex flex-column gap-3">
                      {selectedDocument.versions.map((version) => (
                        <div key={version.id} className="u-surface-note" data-testid={`governance-version-${version.versionNumber}`}>
                          <div className="flex justify-content-between gap-3 align-items-start flex-wrap">
                            <div className="min-w-0 flex-1">
                              <div className="u-eyebrow">{t("community_governance.version_badge", { version: version.versionNumber })}</div>
                              <p className="text-sm text-secondary mt-2 mb-0 line-height-3 white-space-pre-wrap">{version.content}</p>
                            </div>
                            <CivicBadge label={version.authorUsername} severity="neutral" />
                          </div>
                          <div className="u-meta-row mt-3">
                            <span>{versionMeta(version)}</span>
                            <span>{version.changeSummary}</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  </CivicCard>
                  <CivicCard title={t("community_governance.version_create_title")} data-testid="governance-version-create-card">
                    {!canManageGovernance ? (
                      <CivicEmptyState icon="pi-lock" title={t("community_governance.version_locked_title")} description={t("community_governance.version_locked_desc")} />
                    ) : (
                      <form className="flex flex-column gap-3" onSubmit={handleVersionSubmit(onSubmitVersion)}>
                        <CivicField label={t("community_governance.form.version_content_label")} error={versionErrors.content?.message}>
                          <Controller
                            name="content"
                            control={versionControl}
                            rules={{ required: t("common.required"), minLength: { value: FORM_LIMITS.governance.contentMin, message: t("community_governance.form.content_too_short") }, maxLength: { value: FORM_LIMITS.governance.contentMax, message: t("community_governance.form.content_too_long") } }}
                            render={({ field, fieldState }) => (
                              <div className="flex flex-column gap-2">
                                <InputTextarea {...field} rows={6} onChange={(e) => field.onChange(e.target.value)} className={classNames("w-full", { "p-invalid": fieldState.error })} maxLength={FORM_LIMITS.governance.contentMax} data-testid="governance-version-content-input" />
                                <CivicCharacterCount current={watchedVersionContent.length} max={FORM_LIMITS.governance.contentMax} min={FORM_LIMITS.governance.contentMin} />
                              </div>
                            )}
                          />
                        </CivicField>

                        <CivicField label={t("community_governance.form.version_change_summary_label")} error={versionErrors.changeSummary?.message}>
                          <Controller
                            name="changeSummary"
                            control={versionControl}
                            rules={{ required: t("common.required"), minLength: { value: FORM_LIMITS.governance.changeSummaryMin, message: t("community_governance.form.change_summary_too_short") }, maxLength: { value: FORM_LIMITS.governance.changeSummaryMax, message: t("community_governance.form.change_summary_too_long") } }}
                            render={({ field, fieldState }) => (
                              <div className="flex flex-column gap-2">
                                <InputText {...field} onChange={(e) => field.onChange(e.target.value)} className={classNames("w-full", { "p-invalid": fieldState.error })} maxLength={FORM_LIMITS.governance.changeSummaryMax} data-testid="governance-version-change-summary-input" />
                                <CivicCharacterCount current={watchedVersionChangeSummary.length} max={FORM_LIMITS.governance.changeSummaryMax} min={FORM_LIMITS.governance.changeSummaryMin} />
                              </div>
                            )}
                          />
                        </CivicField>

                        <div className="grid">
                          <div className="col-12">
                            <CivicField label={t("community_governance.form.source_url_label")}>
                              <Controller name="sourceUrl" control={versionControl} render={({ field }) => <InputText {...field} onChange={(e) => field.onChange(e.target.value)} className="w-full" data-testid="governance-version-source-url-input" />} />
                            </CivicField>
                          </div>
                          <div className="col-12 md:col-6">
                            <CivicField label={t("community_governance.form.effective_date_label")}>
                              <Controller name="effectiveDate" control={versionControl} render={({ field }) => <InputText {...field} type="date" onChange={(e) => field.onChange(e.target.value)} className="w-full" data-testid="governance-version-effective-date-input" />} />
                            </CivicField>
                          </div>
                          <div className="col-12 md:col-6">
                            <CivicField label={t("community_governance.form.meeting_date_label")}>
                              <Controller name="meetingDate" control={versionControl} render={({ field }) => <InputText {...field} type="date" onChange={(e) => field.onChange(e.target.value)} className="w-full" data-testid="governance-version-meeting-date-input" />} />
                            </CivicField>
                          </div>
                        </div>

                        <div className="flex justify-content-end">
                          <CivicButton type="submit" icon="pi pi-history" label={t("community_governance.version_create_action")} loading={isSubmittingVersion} data-testid="governance-version-submit-button" />
                        </div>
                      </form>
                    )}
                  </CivicCard>
                </div>
              ) : (
                <CivicCard>
                  <CivicEmptyState icon="pi-book" title={t("community_governance.empty_detail_title")} description={t("community_governance.empty_detail_desc")} />
                </CivicCard>
              )}
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
