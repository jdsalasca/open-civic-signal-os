
import { useMemo, useState } from "react";
import { useFieldArray, useForm, Controller, type FieldPath } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
import { Slider } from "primereact/slider";
import { classNames } from "primereact/utils";
import { Layout } from "../components/Layout";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { useCommunityStore } from "../store/useCommunityStore";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicField } from "../components/ui/CivicField";
import { CivicSelect } from "../components/ui/CivicSelect";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";
import { CivicCharacterCount } from "../components/ui/CivicCharacterCount";
import { CivicStatCard } from "../components/ui/CivicStatCard";
import { FORM_LIMITS } from "../constants/formLimits";
import { isSubmitShortcut } from "../utils/keyboard";
import { Signal } from "../types";

interface ApiError extends Error {
  friendlyMessage?: string;
}

type EvidenceItem = {
  url: string;
};

type ReportForm = {
  title: string;
  description: string;
  category: string;
  locationLabel: string;
  evidenceItems: EvidenceItem[];
  urgency: number;
  impact: number;
  affectedPeople: number;
  latitude?: number;
  longitude?: number;
};

type WizardStepKey = "basics" | "evidence" | "impact";

export function ReportSignal() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { activeCommunityId } = useCommunityStore();
  const {
    control,
    handleSubmit,
    watch,
    setValue,
    trigger,
    formState: { isSubmitting, errors },
  } = useForm<ReportForm>({
    mode: "onChange",
    defaultValues: {
      title: "",
      description: "",
      category: "safety",
      locationLabel: "",
      evidenceItems: [{ url: "" }],
      urgency: 3,
      impact: 3,
      affectedPeople: 10,
    },
  });
  const { fields: evidenceFields, append, remove } = useFieldArray({
    control,
    name: "evidenceItems",
  });

  const [currentStep, setCurrentStep] = useState(0);
  const [isLocating, setIsLocating] = useState(false);
  const [locationFound, setLocationFound] = useState(false);
  const [submittedSignal, setSubmittedSignal] = useState<Signal | null>(null);

  const categories = [
    { label: t("categories.safety"), value: "safety", icon: "pi-shield" },
    { label: t("categories.infrastructure"), value: "infrastructure", icon: "pi-building" },
    { label: t("categories.environment"), value: "environment", icon: "pi-sun" },
    { label: t("categories.social"), value: "social", icon: "pi-users" },
    { label: t("categories.mobility"), value: "mobility", icon: "pi-car" },
    { label: t("categories.education"), value: "education", icon: "pi-book" },
  ];

  const quickTemplates = [
    {
      key: "pothole",
      title: t("report.template_pothole_title"),
      description: t("report.template_pothole_desc"),
      category: "infrastructure",
      urgency: 3,
      impact: 2,
      affectedPeople: 40,
      locationLabel: t("report.template_pothole_location"),
      label: t("report.template_pothole_label"),
    },
    {
      key: "lighting",
      title: t("report.template_lighting_title"),
      description: t("report.template_lighting_desc"),
      category: "safety",
      urgency: 4,
      impact: 3,
      affectedPeople: 60,
      locationLabel: t("report.template_lighting_location"),
      label: t("report.template_lighting_label"),
    },
    {
      key: "waste",
      title: t("report.template_waste_title"),
      description: t("report.template_waste_desc"),
      category: "environment",
      urgency: 2,
      impact: 3,
      affectedPeople: 30,
      locationLabel: t("report.template_waste_location"),
      label: t("report.template_waste_label"),
    },
  ] as const;

  const citizenPresets = [10, 30, 60, 120, 300];
  const severityPresets = [
    { key: "low", urgency: 1, impact: 1, label: t("report.preset_low") },
    { key: "medium", urgency: 3, impact: 3, label: t("report.preset_medium") },
    { key: "high", urgency: 5, impact: 5, label: t("report.preset_high") },
  ] as const;

  const stepDefinitions = useMemo(
    () => [
      {
        key: "basics" as WizardStepKey,
        label: t("report.step_basics_label"),
        description: t("report.step_basics_desc"),
      },
      {
        key: "evidence" as WizardStepKey,
        label: t("report.step_evidence_label"),
        description: t("report.step_evidence_desc"),
      },
      {
        key: "impact" as WizardStepKey,
        label: t("report.step_impact_label"),
        description: t("report.step_impact_desc"),
      },
    ],
    [t]
  );

  const currentUrgency = watch("urgency");
  const currentImpact = watch("impact");
  const currentTitle = watch("title") ?? "";
  const currentDescription = watch("description") ?? "";
  const currentCategory = watch("category");
  const currentLocationLabel = watch("locationLabel") ?? "";
  const currentEvidenceItems = watch("evidenceItems") ?? [];
  const currentEvidenceUrls = currentEvidenceItems
    .map((item) => item?.url?.trim() ?? "")
    .filter((value) => value.length > 0);
  const currentTitleLength = currentTitle.length;
  const currentDescriptionLength = currentDescription.length;
  const currentLocationLength = currentLocationLabel.length;
  const currentImageUrl = currentEvidenceUrls[0] ?? "";

  const detectLocation = () => {
    if (!navigator.geolocation) {
      toast.error(t("report.geolocation_not_supported"));
      return;
    }
    setIsLocating(true);
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setValue("latitude", position.coords.latitude);
        setValue("longitude", position.coords.longitude);
        if (!currentLocationLabel.trim()) {
          setValue(
            "locationLabel",
            t("report.detected_coordinates_label", {
              latitude: position.coords.latitude.toFixed(5),
              longitude: position.coords.longitude.toFixed(5),
            }),
            { shouldDirty: true, shouldValidate: true }
          );
        }
        setLocationFound(true);
        setIsLocating(false);
        toast.success(t("report.geolocation_success"));
      },
      () => {
        setIsLocating(false);
        toast.error(t("report.geolocation_error"));
      }
    );
  };

  const getStepFields = (stepIndex: number): FieldPath<ReportForm>[] => {
    if (stepIndex === 0) {
      return ["title", "category", "locationLabel"];
    }
    if (stepIndex === 1) {
      return [
        "description",
        ...currentEvidenceItems.map((_, index) => `evidenceItems.${index}.url` as FieldPath<ReportForm>),
      ];
    }
    return ["urgency", "impact", "affectedPeople"];
  };

  const goToNextStep = async () => {
    const valid = await trigger(getStepFields(currentStep));
    if (!valid) {
      toast.error(t("report.fix_step_errors"));
      return;
    }
    setCurrentStep((step) => Math.min(step + 1, stepDefinitions.length - 1));
  };

  const goToPreviousStep = () => {
    setCurrentStep((step) => Math.max(step - 1, 0));
  };

  const applyTemplate = (templateKey: (typeof quickTemplates)[number]["key"]) => {
    const template = quickTemplates.find((item) => item.key === templateKey);
    if (!template) return;

    setValue("title", template.title, { shouldDirty: true, shouldValidate: true });
    setValue("description", template.description, { shouldDirty: true, shouldValidate: true });
    setValue("category", template.category, { shouldDirty: true, shouldValidate: true });
    setValue("urgency", template.urgency, { shouldDirty: true, shouldValidate: true });
    setValue("impact", template.impact, { shouldDirty: true, shouldValidate: true });
    setValue("affectedPeople", template.affectedPeople, { shouldDirty: true, shouldValidate: true });
    setValue("locationLabel", template.locationLabel, { shouldDirty: true, shouldValidate: true });
    toast.success(t("report.template_applied"));
  };

  const handleDiscard = () => {
    navigate("/");
  };

  const getScaleColor = (val: number) => {
    if (val <= 2) return "var(--status-resolved)";
    if (val <= 3) return "var(--status-progress)";
    return "var(--status-rejected)";
  };

  const onSubmit = async (data: ReportForm) => {
    const evidenceUrls = data.evidenceItems
      .map((item) => item.url.trim())
      .filter((value) => value.length > 0);

    try {
      const response = await apiClient.post<Signal>("signals", {
        title: data.title,
        description: data.description,
        category: data.category,
        locationLabel: data.locationLabel.trim() || undefined,
        urgency: data.urgency,
        impact: data.impact,
        affectedPeople: data.affectedPeople,
        imageUrl: evidenceUrls[0] || undefined,
        evidenceUrls,
        latitude: data.latitude,
        longitude: data.longitude,
      });
      setSubmittedSignal(response.data);
      toast.success(t("report.success"));
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("common.error"));
    }
  };
  const renderStepContent = () => {
    if (currentStep === 0) {
      return (
        <div className="flex flex-column gap-4" data-testid="report-wizard-step-1">
          <div>
            <label className="text-xs font-black uppercase tracking-widest text-main block mb-2">{t("report.quick_templates_label")}</label>
            <div className="flex flex-wrap gap-2">
              {quickTemplates.map((template) => (
                <CivicButton
                  key={template.key}
                  type="button"
                  variant="ghost"
                  size="small"
                  label={template.label}
                  onClick={() => applyTemplate(template.key)}
                />
              ))}
            </div>
          </div>

          <CivicField label={t("report.issue_title")} error={errors.title?.message} helpText={t("report.issue_title_help")}>
            <Controller
              name="title"
              control={control}
              rules={{
                required: t("common.required"),
                minLength: { value: FORM_LIMITS.report.titleMin, message: t("report.title_too_short") },
                maxLength: { value: FORM_LIMITS.report.titleMax, message: t("report.title_too_long") },
              }}
              render={({ field, fieldState }) => (
                <div className="flex flex-column gap-2">
                  <InputText
                    {...field}
                    id="report-title"
                    className={classNames("w-full", { "p-invalid": fieldState.error })}
                    placeholder={t("report.issue_title_placeholder")}
                    data-testid="report-title-input"
                    maxLength={FORM_LIMITS.report.titleMax}
                  />
                  <CivicCharacterCount current={currentTitleLength} max={FORM_LIMITS.report.titleMax} min={FORM_LIMITS.report.titleMin} />
                </div>
              )}
            />
          </CivicField>

          <CivicField label={t("common.category")} error={errors.category?.message} helpText={t("report.category_help")}>
            <Controller
              name="category"
              control={control}
              rules={{ required: t("common.required") }}
              render={({ field, fieldState }) => (
                <CivicSelect
                  value={field.value}
                  onChange={(e) => field.onChange(e.value)}
                  options={categories}
                  placeholder={t("common.select_category")}
                  inputId="report-category"
                  className={classNames("w-full", { "p-invalid": fieldState.error })}
                  data-testid="report-category-dropdown"
                  itemTemplate={(option) => (
                    <div className="flex align-items-center gap-2">
                      <i className={`pi ${option.icon} text-brand-primary`}></i>
                      <span>{option.label}</span>
                    </div>
                  )}
                />
              )}
            />
          </CivicField>

          <CivicField label={t("report.location_label")} error={errors.locationLabel?.message} helpText={t("report.location_help")}>
            <Controller
              name="locationLabel"
              control={control}
              rules={{
                required: t("common.required"),
                maxLength: { value: FORM_LIMITS.report.locationMax, message: t("report.location_too_long") },
              }}
              render={({ field, fieldState }) => (
                <div className="flex flex-column gap-2">
                  <InputText
                    {...field}
                    id="report-location-label"
                    className={classNames("w-full", { "p-invalid": fieldState.error })}
                    placeholder={t("report.location_placeholder")}
                    data-testid="report-location-input"
                    maxLength={FORM_LIMITS.report.locationMax}
                  />
                  <CivicCharacterCount current={currentLocationLength} max={FORM_LIMITS.report.locationMax} />
                </div>
              )}
            />
          </CivicField>
        </div>
      );
    }

    if (currentStep === 1) {
      return (
        <div className="flex flex-column gap-4" data-testid="report-wizard-step-2">
          <CivicField label={t("report.context")} error={errors.description?.message} helpText={t("report.context_help")}>
            <Controller
              name="description"
              control={control}
              rules={{
                required: t("common.required"),
                minLength: { value: FORM_LIMITS.report.descriptionMin, message: t("report.desc_too_short") },
                maxLength: { value: FORM_LIMITS.report.descriptionMax, message: t("report.desc_too_long") },
              }}
              render={({ field, fieldState }) => (
                <div className="flex flex-column gap-2">
                  <InputTextarea
                    {...field}
                    id="report-description"
                    rows={7}
                    onChange={(e) => field.onChange(e.target.value)}
                    onKeyDown={(e) => {
                      if (isSubmitShortcut(e) && currentStep === stepDefinitions.length - 1) {
                        e.preventDefault();
                        const form = e.currentTarget.form;
                        form?.requestSubmit();
                      }
                    }}
                    className={classNames("w-full", { "p-invalid": fieldState.error })}
                    placeholder={t("report.context_placeholder")}
                    data-testid="report-description-textarea"
                    maxLength={FORM_LIMITS.report.descriptionMax}
                  />
                  <CivicCharacterCount current={currentDescriptionLength} max={FORM_LIMITS.report.descriptionMax} min={FORM_LIMITS.report.descriptionMin} />
                  <small className="text-muted text-xs">{t("report.submit_shortcut_hint")}</small>
                </div>
              )}
            />
          </CivicField>

          <CivicCard title={t("report.evidence_title")} className="mb-0">
            <div className="flex flex-column gap-4">
              <p className="text-sm text-secondary m-0">{t("report.evidence_desc")}</p>
              {evidenceFields.map((item, index) => (
                <CivicField
                  key={item.id}
                  label={t("report.evidence_item_label", { index: index + 1 })}
                  error={errors.evidenceItems?.[index]?.url?.message}
                  helpText={index === 0 ? t("report.image_url_help") : undefined}
                  className="mb-0"
                >
                  <Controller
                    name={`evidenceItems.${index}.url`}
                    control={control}
                    rules={{
                      pattern: {
                        value: /^https?:\/\/.+/i,
                        message: t("report.image_url_invalid"),
                      },
                      maxLength: {
                        value: FORM_LIMITS.report.evidenceUrlMax,
                        message: t("report.image_url_too_long"),
                      },
                    }}
                    render={({ field, fieldState }) => (
                      <div className="flex flex-column md:flex-row gap-2 align-items-stretch">
                        <InputText
                          {...field}
                          value={field.value || ""}
                          onChange={(e) => field.onChange(e.target.value)}
                          className={classNames("w-full", { "p-invalid": fieldState.error })}
                          placeholder={t("report.image_url_placeholder")}
                          data-testid={`report-evidence-input-${index}`}
                          maxLength={FORM_LIMITS.report.evidenceUrlMax}
                        />
                        {evidenceFields.length > 1 && (
                          <CivicButton
                            type="button"
                            variant="ghost"
                            icon="pi pi-times"
                            label={t("report.remove_evidence")}
                            onClick={() => remove(index)}
                            data-testid={`report-remove-evidence-${index}`}
                          />
                        )}
                      </div>
                    )}
                  />
                </CivicField>
              ))}
              <div className="flex flex-wrap gap-2 align-items-center">
                <CivicButton
                  type="button"
                  variant="secondary"
                  icon="pi pi-plus"
                  label={t("report.add_evidence")}
                  onClick={() => append({ url: "" })}
                  disabled={evidenceFields.length >= FORM_LIMITS.report.evidenceMax}
                  data-testid="report-add-evidence-button"
                />
                <small className="text-muted text-xs">
                  {t("report.evidence_limit", { count: FORM_LIMITS.report.evidenceMax })}
                </small>
              </div>
            </div>
          </CivicCard>
        </div>
      );
    }

    return (
      <div className="flex flex-column gap-5" data-testid="report-wizard-step-3">
        <div>
          <label className="text-xs font-black uppercase tracking-widest text-main block mb-2">{t("report.quick_preset_label")}</label>
          <div className="flex flex-wrap gap-2">
            {severityPresets.map((preset) => {
              const selected = currentUrgency === preset.urgency && currentImpact === preset.impact;
              return (
                <CivicButton
                  key={preset.key}
                  type="button"
                  size="small"
                  variant={selected ? "primary" : "ghost"}
                  label={preset.label}
                  onClick={() => {
                    setValue("urgency", preset.urgency, { shouldDirty: true, shouldValidate: true });
                    setValue("impact", preset.impact, { shouldDirty: true, shouldValidate: true });
                  }}
                />
              );
            })}
          </div>
        </div>

        <div className="grid">
          <div className="col-12 md:col-6">
            <CivicField label={t("report.urgency")} helpText={t("signals.urgency_formula")}>
              <div className="flex flex-column gap-4">
                <div className="flex justify-content-between align-items-end">
                  <span className="text-xs font-black uppercase tracking-widest text-muted">{t("report.urgency_scale_label")}</span>
                  <span className="text-3xl font-black" style={{ color: getScaleColor(currentUrgency) }}>{currentUrgency}</span>
                </div>
                <div className="p-4 border-round-2xl shadow-inner" style={{ background: `linear-gradient(135deg, ${getScaleColor(currentUrgency)}15 0%, transparent 100%)`, border: `1px solid ${getScaleColor(currentUrgency)}30` }}>
                  <div data-testid="report-urgency-slider">
                    <Controller
                      name="urgency"
                      control={control}
                      render={({ field }) => (
                        <Slider value={field.value} onChange={(e) => field.onChange(e.value)} min={1} max={5} step={1} className="w-full" />
                      )}
                    />
                  </div>
                  <div className="flex justify-content-between mt-4 text-min font-black uppercase tracking-tighter opacity-50">
                    <span>{t("report.urgency_low")}</span>
                    <span>{t("report.urgency_critical")}</span>
                  </div>
                </div>
              </div>
            </CivicField>
          </div>

          <div className="col-12 md:col-6">
            <CivicField label={t("report.impact")} helpText={t("signals.impact_formula")}>
              <div className="flex flex-column gap-4">
                <div className="flex justify-content-between align-items-end">
                  <span className="text-xs font-black uppercase tracking-widest text-muted">{t("report.impact_scale_label")}</span>
                  <span className="text-3xl font-black" style={{ color: getScaleColor(currentImpact) }}>{currentImpact}</span>
                </div>
                <div className="p-4 border-round-2xl shadow-inner" style={{ background: `linear-gradient(135deg, ${getScaleColor(currentImpact)}15 0%, transparent 100%)`, border: `1px solid ${getScaleColor(currentImpact)}30` }}>
                  <div data-testid="report-impact-slider">
                    <Controller
                      name="impact"
                      control={control}
                      render={({ field }) => (
                        <Slider value={field.value} onChange={(e) => field.onChange(e.value)} min={1} max={5} step={1} className="w-full" />
                      )}
                    />
                  </div>
                  <div className="flex justify-content-between mt-4 text-min font-black uppercase tracking-tighter opacity-50">
                    <span>{t("report.impact_minor")}</span>
                    <span>{t("report.impact_systemic")}</span>
                  </div>
                </div>
              </div>
            </CivicField>
          </div>
        </div>

        <CivicField label={t("report.scale")} helpText={t("report.scale_help")}>
          <Controller
            name="affectedPeople"
            control={control}
            render={({ field }) => (
              <div className="flex flex-column gap-3 p-3 border-round-xl border-1 border-subtle" style={{ background: "var(--panel-soft-bg)" }}>
                <div className="flex justify-content-between font-black text-main">
                  <span className="text-xs uppercase opacity-50">{t("signals.citizens")}</span>
                  <span className="text-brand-primary">{field.value}</span>
                </div>
                <Slider value={field.value} onChange={(e) => field.onChange(e.value)} min={1} max={1000} className="w-full" />
                <div className="flex flex-wrap gap-2">
                  {citizenPresets.map((preset) => (
                    <CivicButton
                      key={preset}
                      type="button"
                      variant={field.value === preset ? "primary" : "ghost"}
                      size="small"
                      label={String(preset)}
                      onClick={() => field.onChange(preset)}
                    />
                  ))}
                </div>
              </div>
            )}
          />
        </CivicField>

        <CivicCard title={t("report.geolocation_title")} className="mb-0">
          <div className="flex flex-column gap-3">
            <p className="text-sm text-secondary m-0">{t("report.geolocation_help")}</p>
            <CivicButton
              type="button"
              variant={locationFound ? "primary" : "secondary"}
              icon={isLocating ? "pi pi-spin pi-spinner" : locationFound ? "pi pi-check" : "pi pi-map-marker"}
              label={isLocating ? t("report.detecting_coordinates") : locationFound ? t("report.coordinates_secured") : t("report.detect_location_auto")}
              onClick={detectLocation}
              className="w-full py-3"
              data-testid="report-detect-location-button"
            />
            {(watch("latitude") || watch("longitude")) && (
              <div className="u-pill w-fit" data-testid="report-location-coordinates">
                <i className="pi pi-compass text-brand-primary"></i>
                {t("report.coordinates_value", {
                  latitude: watch("latitude")?.toFixed(5),
                  longitude: watch("longitude")?.toFixed(5),
                })}
              </div>
            )}
          </div>
        </CivicCard>
      </div>
    );
  };

  if (submittedSignal) {
    const topFactors = submittedSignal.explainabilitySummary?.topFactors ?? [];
    const primaryFactor = topFactors[0]?.key
      ? t(`signals.factor_keys.${topFactors[0].key}`)
      : t("report.success_factor_fallback");
    const secondaryFactor = topFactors[1]?.key
      ? t(`signals.factor_keys.${topFactors[1].key}`)
      : null;

    return (
      <Layout>
        <div className="animate-fade-up max-w-50rem mx-auto pb-8">
          <CivicPageHeader title={t("report.success_title")} description={t("report.success_desc")} />
          <CivicCard variant="brand" className="mb-6" data-testid="report-success-card">
            <div className="flex flex-column gap-4">
              <div className="u-pill w-fit" data-testid="report-success-status">
                <i className="pi pi-check-circle text-brand-primary"></i>
                {t("report.success_status")}
              </div>
              <h2 className="text-3xl font-black text-main m-0" data-testid="report-success-title">
                {submittedSignal.title}
              </h2>
              <p className="text-secondary m-0" data-testid="report-success-lifecycle-intro">
                {t("report.success_lifecycle_intro")}
              </p>
              <div className="grid">
                <div className="col-12 md:col-4">
                  <div className="p-4 border-round-xl border-1 border-subtle h-full" data-testid="report-success-step-review">
                    <div className="text-xs font-black uppercase tracking-widest text-muted mb-2">{t("report.success_step_1_label")}</div>
                    <div className="text-sm text-secondary">{t("report.success_step_1_desc")}</div>
                  </div>
                </div>
                <div className="col-12 md:col-4">
                  <div className="p-4 border-round-xl border-1 border-subtle h-full" data-testid="report-success-step-priority">
                    <div className="text-xs font-black uppercase tracking-widest text-muted mb-2">{t("report.success_step_2_label")}</div>
                    <div className="text-sm text-secondary">
                      {secondaryFactor
                        ? t("report.success_step_2_desc_with_secondary", { primary: primaryFactor, secondary: secondaryFactor })
                        : t("report.success_step_2_desc", { primary: primaryFactor })}
                    </div>
                  </div>
                </div>
                <div className="col-12 md:col-4">
                  <div className="p-4 border-round-xl border-1 border-subtle h-full" data-testid="report-success-step-followup">
                    <div className="text-xs font-black uppercase tracking-widest text-muted mb-2">{t("report.success_step_3_label")}</div>
                    <div className="text-sm text-secondary">{t("report.success_step_3_desc")}</div>
                  </div>
                </div>
              </div>
              <div className="p-4 border-round-xl border-1 border-subtle bg-surface" data-testid="report-success-why-ranked">
                <div className="text-xs font-black uppercase tracking-widest text-muted mb-2">{t("report.success_why_ranked_label")}</div>
                <div className="text-sm text-secondary">
                  {submittedSignal.explainabilitySummary?.summary || t("report.success_why_ranked_fallback")}
                </div>
              </div>
              <div className="flex flex-column md:flex-row gap-3">
                <CivicButton type="button" label={t("report.success_cta_my_reports")} icon="pi pi-user" onClick={() => navigate("/mine")} data-testid="report-success-go-mine" />
                <CivicButton type="button" label={t("report.success_cta_view_detail")} icon="pi pi-arrow-right" variant="secondary" onClick={() => navigate(`/signals/${submittedSignal.id}`)} data-testid="report-success-go-detail" />
                <CivicButton type="button" label={t("report.success_cta_backlog")} icon="pi pi-list" variant="ghost" onClick={() => navigate("/")} data-testid="report-success-go-dashboard" />
              </div>
            </div>
          </CivicCard>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="animate-fade-up max-w-70rem mx-auto pb-8">
        <CivicPageHeader title={t("report.title")} description={t("report.desc")} />

        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="grid">
            <div className="col-12 lg:col-8">
              <CivicCard className="mb-6">
                <div className="flex flex-column gap-4">
                  <div className="flex flex-wrap gap-3" aria-label={t("report.wizard_progress_label")}>
                    {stepDefinitions.map((step, index) => {
                      const isActive = index === currentStep;
                      const isComplete = index < currentStep;
                      return (
                        <button
                          key={step.key}
                          type="button"
                          className={classNames("border-none bg-transparent p-0 text-left cursor-pointer flex-grow-1 min-w-0", { "opacity-60": !isActive && !isComplete })}
                          onClick={() => {
                            if (index <= currentStep) setCurrentStep(index);
                          }}
                          aria-current={isActive ? "step" : undefined}
                          data-testid={`report-step-tab-${index + 1}`}
                        >
                          <div className={classNames("p-3 border-round-2xl border-1 h-full", {
                            "border-brand-primary bg-brand-primary-alpha-10": isActive,
                            "border-surface-soft bg-surface-soft": !isActive,
                          })}>
                            <div className="flex align-items-center gap-2 mb-2">
                              <span className={classNames("flex align-items-center justify-content-center border-circle font-black", {
                                "bg-brand-primary text-on-brand": isActive || isComplete,
                                "bg-surface-soft-strong text-main": !isActive && !isComplete,
                              })} style={{ width: "2rem", height: "2rem" }}>
                                {index + 1}
                              </span>
                              <span className="text-xs font-black uppercase tracking-widest text-main">{step.label}</span>
                            </div>
                            <p className="text-sm text-secondary m-0">{step.description}</p>
                          </div>
                        </button>
                      );
                    })}
                  </div>

                  <div>
                    <div className="text-xs font-black uppercase tracking-widest text-muted mb-2">{t("report.current_step_label")}</div>
                    <h2 className="text-3xl font-black text-main m-0">{stepDefinitions[currentStep].label}</h2>
                    <p className="text-secondary mt-2 mb-0">{stepDefinitions[currentStep].description}</p>
                  </div>

                  {renderStepContent()}

                  <div className="flex flex-column md:flex-row justify-content-between gap-3 pt-3 border-top-1 border-subtle">
                    <div className="flex gap-2">
                      {currentStep > 0 && (
                        <CivicButton type="button" variant="ghost" icon="pi pi-arrow-left" label={t("report.back_step")} onClick={goToPreviousStep} data-testid="report-back-button" />
                      )}
                    </div>
                    <div className="flex gap-2 flex-wrap justify-content-end">
                      <CivicButton type="button" label={t("common.discard")} variant="secondary" onClick={handleDiscard} data-testid="report-discard-button" />
                      {currentStep < stepDefinitions.length - 1 ? (
                        <CivicButton type="button" label={t("report.next_step")} icon="pi pi-arrow-right" iconPos="right" onClick={goToNextStep} data-testid="report-next-button" />
                      ) : (
                        <CivicButton type="submit" label={t("report.submit")} icon="pi pi-bolt" loading={isSubmitting} glow data-testid="report-submit-button" />
                      )}
                    </div>
                  </div>
                </div>
              </CivicCard>
            </div>

            <div className="col-12 lg:col-4">
              <CivicCard title={t("report.summary_title")} variant="brand" className="mb-6">
                <div className="flex flex-column gap-4">
                  <div className="u-pill w-fit">
                    <i className="pi pi-users text-brand-primary"></i>
                    {activeCommunityId ? t("report.summary_community_ready") : t("report.community_required")}
                  </div>
                  <div className="civic-stat-grid civic-stat-grid-comfortable">
                    <CivicStatCard
                      label={t("report.summary_category")}
                      value={t(`categories.${currentCategory}`)}
                      compact
                    />
                    <CivicStatCard
                      label={t("report.summary_location")}
                      value={currentLocationLabel.trim() || t("report.summary_missing")}
                      compact
                    />
                    <CivicStatCard
                      label={t("report.summary_evidence")}
                      value={t("report.summary_evidence_count", { count: currentEvidenceUrls.length })}
                      compact
                    />
                    <CivicStatCard
                      label={t("report.scale")}
                      value={t("report.summary_people", { count: watch("affectedPeople") })}
                      compact
                    />
                  </div>
                  {currentImageUrl && (
                    <div className="border-round-xl overflow-hidden border-1 border-subtle">
                      <img src={currentImageUrl} alt={t("report.image_preview_alt")} className="w-full" style={{ maxHeight: "14rem", objectFit: "cover" }} />
                    </div>
                  )}
                  <div className="p-3 border-round-xl border-1 border-subtle bg-surface">
                    <div className="text-xs font-black uppercase tracking-widest text-muted mb-2">{t("report.summary_checklist_title")}</div>
                    <ul className="m-0 pl-3 text-sm text-secondary line-height-3">
                      <li>{t("report.summary_checklist_1")}</li>
                      <li>{t("report.summary_checklist_2")}</li>
                      <li>{t("report.summary_checklist_3")}</li>
                    </ul>
                  </div>
                </div>
              </CivicCard>

              <CivicCard title={t("auth.verify_protocol")}>
                {!activeCommunityId ? (
                  <div className="flex flex-column gap-3">
                    <div className="p-4 border-round-xl bg-status-rejected-alpha-10 border-1 border-status-rejected-alpha-20 text-status-rejected text-sm font-bold flex align-items-center gap-3">
                      <i className="pi pi-lock text-xl"></i>
                      {t("report.community_required")}
                    </div>
                    <CivicButton type="button" label={t("nav.communities")} icon="pi pi-users" variant="secondary" className="w-full py-3" onClick={() => navigate("/communities")} data-testid="report-go-communities-button" />
                  </div>
                ) : (
                  <div className="flex flex-column gap-4">
                    <div className="u-pill w-full justify-content-start px-3">
                      <i className="pi pi-shield text-brand-primary text-xl"></i>
                      <div className="flex flex-column">
                        <span className="text-xs font-bold text-main uppercase">{t("settings.encryption")}</span>
                        <span className="text-min text-muted">{t("settings.encryption_value")}</span>
                      </div>
                    </div>
                    <div className="p-4 border-round-xl border-1 border-subtle bg-surface">
                      <div className="text-xs font-black uppercase tracking-widest text-muted mb-2">{t("report.summary_title")}</div>
                      <div className="text-sm text-secondary">{t("report.sidebar_help")}</div>
                    </div>
                  </div>
                )}
              </CivicCard>
            </div>
          </div>
        </form>
      </div>
    </Layout>
  );
}
