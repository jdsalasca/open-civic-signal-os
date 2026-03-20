import { useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import apiClient from "../api/axios";
import { Layout } from "../components/Layout";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";
import { CivicField } from "../components/ui/CivicField";
import { CivicMetaRow } from "../components/ui/CivicMetaRow";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";
import { CivicSelect } from "../components/ui/CivicSelect";
import { CivicStatCard } from "../components/ui/CivicStatCard";
import type {
  HelpCenterResponse,
  HelpCenterStateResponse,
  HelpGuide,
  HelpSurface,
  OnboardingStep,
} from "../types";

const SURFACES: Array<{ value: "ALL" | HelpSurface; icon: string }> = [
  { value: "ALL", icon: "pi pi-compass" },
  { value: "DASHBOARD", icon: "pi pi-th-large" },
  { value: "REPORT", icon: "pi pi-plus-circle" },
  { value: "COMMUNITIES", icon: "pi pi-globe" },
  { value: "PROPOSALS", icon: "pi pi-file-edit" },
  { value: "GOVERNANCE", icon: "pi pi-book" },
  { value: "PROJECTS", icon: "pi pi-briefcase" },
];

export function HelpCenter() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [payload, setPayload] = useState<HelpCenterResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState(searchParams.get("query") ?? "");
  const [surface, setSurface] = useState<"ALL" | HelpSurface>((searchParams.get("surface") as "ALL" | HelpSurface | null) ?? "ALL");
  const [updatingKey, setUpdatingKey] = useState<string | null>(null);
  const highlightedGuideId = searchParams.get("guide");

  useEffect(() => {
    const nextParams: Record<string, string> = {};
    if (query.trim()) {
      nextParams.query = query.trim();
    }
    if (surface !== "ALL") {
      nextParams.surface = surface;
    }
    if (highlightedGuideId) {
      nextParams.guide = highlightedGuideId;
    }
    setSearchParams(nextParams, { replace: true });
  }, [highlightedGuideId, query, setSearchParams, surface]);

  useEffect(() => {
    let mounted = true;

    const load = async () => {
      try {
        setLoading(true);
        const response = await apiClient.get<HelpCenterResponse>("help-center", {
          params: {
            lang: i18n.language,
            surface: surface === "ALL" ? undefined : surface,
            query: query.trim() || undefined,
          }
        });
        if (!mounted) {
          return;
        }
        setPayload(response.data);
      } catch {
        if (mounted) {
          toast.error(t("help_center.load_error"));
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    load();
    return () => {
      mounted = false;
    };
  }, [i18n.language, query, surface, t]);

  const updateState = async (nextCompletedStepKeys: string[], nextDismissedGuideKeys: string[]) => {
    const response = await apiClient.put<HelpCenterStateResponse>("help-center/state", {
      completedStepKeys: nextCompletedStepKeys,
      dismissedGuideKeys: nextDismissedGuideKeys
    });
    setPayload((current) =>
      current
        ? {
            ...current,
            completedStepKeys: response.data.completedStepKeys,
            dismissedGuideKeys: response.data.dismissedGuideKeys,
            onboardingSteps: current.onboardingSteps.map((step) => ({
              ...step,
              completed: response.data.completedStepKeys.includes(step.key),
              dismissed: response.data.dismissedGuideKeys.includes(step.key)
            })),
            guides: current.guides.map((guide) => ({
              ...guide,
              dismissed: response.data.dismissedGuideKeys.includes(guide.id)
            }))
          }
        : current
    );
  };

  const handleToggleStep = async (step: OnboardingStep, complete: boolean) => {
    if (!payload) {
      return;
    }
    try {
      setUpdatingKey(step.key);
      const nextCompleted = complete
        ? Array.from(new Set([...payload.completedStepKeys, step.key])).sort()
        : payload.completedStepKeys.filter((key) => key !== step.key);
      await updateState(nextCompleted, payload.dismissedGuideKeys);
    } catch {
      toast.error(t("help_center.state_error"));
    } finally {
      setUpdatingKey(null);
    }
  };

  const handleDismissToggle = async (key: string, dismissed: boolean) => {
    if (!payload) {
      return;
    }
    try {
      setUpdatingKey(key);
      const nextDismissed = dismissed
        ? Array.from(new Set([...payload.dismissedGuideKeys, key])).sort()
        : payload.dismissedGuideKeys.filter((item) => item !== key);
      await updateState(payload.completedStepKeys, nextDismissed);
    } catch {
      toast.error(t("help_center.state_error"));
    } finally {
      setUpdatingKey(null);
    }
  };

  const activeSteps = useMemo(
    () => (payload?.onboardingSteps ?? []).filter((step) => !step.dismissed),
    [payload]
  );
  const dismissedSteps = useMemo(
    () => (payload?.onboardingSteps ?? []).filter((step) => step.dismissed),
    [payload]
  );
  const visibleGuides = useMemo(
    () => (payload?.guides ?? []).filter((guide) => !guide.dismissed),
    [payload]
  );
  const dismissedGuides = useMemo(
    () => (payload?.guides ?? []).filter((guide) => guide.dismissed),
    [payload]
  );

  const groupedGuides = useMemo(() => {
    const groups = new Map<string, HelpGuide[]>();
    visibleGuides.forEach((guide) => {
      const key = guide.surface;
      groups.set(key, [...(groups.get(key) ?? []), guide]);
    });
    return Array.from(groups.entries());
  }, [visibleGuides]);

  const surfaceOptions = SURFACES.map((item) => ({
    label: t(`help_center.surfaces.${item.value}`),
    value: item.value
  }));

  return (
    <Layout>
      <div className="animate-fade-up motion-page max-w-72rem mx-auto">
        <CivicPageHeader title={t("help_center.title")} description={t("help_center.desc")} />

        <div className="grid mb-6">
          <div className="col-12 md:col-4">
            <CivicStatCard
              label={t("help_center.persona_label")}
              value={payload ? t(`help_center.personas.${payload.persona}`) : t("common.loading")}
              supportingText={t("help_center.persona_help")}
              compact
            />
          </div>
          <div className="col-12 md:col-4">
            <CivicStatCard
              label={t("help_center.active_guides_label")}
              value={visibleGuides.length}
              supportingText={t("help_center.active_guides_help")}
              compact
            />
          </div>
          <div className="col-12 md:col-4">
            <CivicStatCard
              label={t("help_center.completed_steps_label")}
              value={payload?.completedStepKeys.length ?? 0}
              supportingText={t("help_center.completed_steps_help")}
              compact
            />
          </div>
        </div>

        <CivicCard className="mb-6">
          <div className="grid">
            <div className="col-12 md:col-7">
              <CivicField label={t("help_center.search_label")} helpText={t("help_center.search_help")}>
                <InputText
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  className="w-full"
                  data-testid="help-center-search"
                />
              </CivicField>
            </div>
            <div className="col-12 md:col-5">
              <CivicField label={t("help_center.surface_label")} helpText={t("help_center.surface_help")}>
                <CivicSelect
                  value={surface}
                  options={surfaceOptions}
                  optionLabel="label"
                  optionValue="value"
                  onChange={(e) => setSurface(e.value as "ALL" | HelpSurface)}
                  className="w-full"
                  data-testid="help-center-surface-filter"
                />
              </CivicField>
            </div>
          </div>
        </CivicCard>

        <CivicCard title={t("help_center.onboarding_title")} className="mb-6" data-testid="help-center-onboarding-card">
          {loading ? (
            <p className="text-sm text-secondary m-0">{t("common.loading")}</p>
          ) : activeSteps.length === 0 ? (
            <CivicEmptyState
              icon="pi pi-check-circle"
              title={t("help_center.onboarding_empty_title")}
              description={t("help_center.onboarding_empty_desc")}
            />
          ) : (
            <div className="flex flex-column gap-4">
              {activeSteps.map((step) => (
                <div key={step.key} className="border-round-xl border-1 border-surface-soft bg-surface-soft p-4">
                  <div className="flex justify-content-between gap-3 flex-wrap align-items-start">
                    <div className="flex-1 min-w-0">
                      <div className="font-black text-main">{step.title}</div>
                      <p className="text-sm text-secondary mt-2 mb-0 line-height-3">{step.description}</p>
                    </div>
                    <CivicBadge
                      label={step.completed ? t("help_center.completed_badge") : t("help_center.pending_badge")}
                      severity={step.completed ? "resolved" : "progress"}
                    />
                  </div>
                  <div className="mt-3 flex gap-2 flex-wrap">
                    <CivicButton
                      type="button"
                      label={step.actionLabel}
                      icon="pi pi-arrow-right"
                      size="small"
                      onClick={() => navigate(step.actionRoute)}
                    />
                    <CivicButton
                      type="button"
                      label={step.completed ? t("help_center.mark_pending") : t("help_center.mark_done")}
                      icon={step.completed ? "pi pi-undo" : "pi pi-check"}
                      variant="secondary"
                      size="small"
                      loading={updatingKey === step.key}
                      onClick={() => handleToggleStep(step, !step.completed)}
                    />
                    <CivicButton
                      type="button"
                      label={t("help_center.dismiss_guide")}
                      icon="pi pi-times"
                      variant="ghost"
                      size="small"
                      loading={updatingKey === step.key}
                      onClick={() => handleDismissToggle(step.key, true)}
                    />
                  </div>
                </div>
              ))}
            </div>
          )}
        </CivicCard>

        <CivicCard title={t("help_center.guides_title")} className="mb-6" data-testid="help-center-guides-card">
          {loading ? (
            <p className="text-sm text-secondary m-0">{t("common.loading")}</p>
          ) : groupedGuides.length === 0 ? (
            <CivicEmptyState
              icon="pi pi-search"
              title={t("help_center.guides_empty_title")}
              description={t("help_center.guides_empty_desc")}
            />
          ) : (
            <div className="flex flex-column gap-5">
              {groupedGuides.map(([groupSurface, guides]) => (
                <div key={groupSurface} className="flex flex-column gap-3">
                  <div className="flex align-items-center gap-2">
                    <CivicBadge label={t(`help_center.surfaces.${groupSurface}`)} severity="neutral" />
                    <span className="text-sm text-secondary">{guides.length}</span>
                  </div>
                  {guides.map((guide) => (
                    <div
                      key={guide.id}
                      className={`border-round-xl border-1 border-surface-soft bg-surface-soft p-4 ${highlightedGuideId === guide.id ? "border-brand-primary" : ""}`}
                      data-testid={`help-guide-${guide.id}`}
                    >
                      <div className="u-card-split-header">
                        <div className="u-card-copy">
                          <div className="font-black text-main">{guide.title}</div>
                          <p className="text-sm text-secondary mt-2 mb-0 line-height-3">{guide.summary}</p>
                        </div>
                        <CivicBadge label={t(`help_center.kinds.${guide.kind}`)} severity="progress" />
                      </div>
                      <p className="text-sm text-main mt-3 mb-0 line-height-3">{guide.body}</p>
                      <div className="mt-3 flex gap-2 flex-wrap">
                        {guide.tags.map((tag) => (
                          <span key={tag} className="u-pill">{tag}</span>
                        ))}
                      </div>
                      <div className="mt-3 flex gap-2 flex-wrap">
                        {guide.actionRoute && (
                          <CivicButton
                            type="button"
                            label={guide.actionLabel ?? t("help_center.open_flow")}
                            icon="pi pi-arrow-right"
                            size="small"
                            onClick={() => navigate(guide.actionRoute!)}
                          />
                        )}
                        {guide.dismissible && (
                          <CivicButton
                            type="button"
                            label={t("help_center.dismiss_guide")}
                            icon="pi pi-times"
                            variant="ghost"
                            size="small"
                            loading={updatingKey === guide.id}
                            onClick={() => handleDismissToggle(guide.id, true)}
                          />
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              ))}
            </div>
          )}
        </CivicCard>

        <CivicCard title={t("help_center.revisit_title")} data-testid="help-center-revisit-card">
          {dismissedSteps.length === 0 && dismissedGuides.length === 0 ? (
            <p className="text-sm text-secondary m-0">{t("help_center.revisit_empty")}</p>
          ) : (
            <div className="flex flex-column gap-4">
              {dismissedSteps.map((step) => (
                <div key={step.key} className="border-round-xl border-1 border-surface-soft bg-surface-soft p-4">
                  <CivicMetaRow label={t("help_center.revisit_step_label")} value={step.title} />
                  <div className="mt-3">
                    <CivicButton
                      type="button"
                      label={t("help_center.restore_guide")}
                      icon="pi pi-refresh"
                      size="small"
                      loading={updatingKey === step.key}
                      onClick={() => handleDismissToggle(step.key, false)}
                    />
                  </div>
                </div>
              ))}
              {dismissedGuides.map((guide) => (
                <div key={guide.id} className="border-round-xl border-1 border-surface-soft bg-surface-soft p-4">
                  <CivicMetaRow label={t("help_center.revisit_guide_label")} value={guide.title} />
                  <div className="mt-3">
                    <CivicButton
                      type="button"
                      label={t("help_center.restore_guide")}
                      icon="pi pi-refresh"
                      size="small"
                      loading={updatingKey === guide.id}
                      onClick={() => handleDismissToggle(guide.id, false)}
                    />
                  </div>
                </div>
              ))}
            </div>
          )}
        </CivicCard>
      </div>
    </Layout>
  );
}
