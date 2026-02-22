import { useForm, Controller } from "react-hook-form";
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
import { FORM_LIMITS } from "../constants/formLimits";

interface ApiError extends Error {
  friendlyMessage?: string;
}

type ReportForm = {
  title: string;
  description: string;
  category: string;
  urgency: number;
  impact: number;
  affectedPeople: number;
};

export function ReportSignal() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { activeCommunityId } = useCommunityStore();
  const {
    control,
    handleSubmit,
    watch,
    formState: { isSubmitting, errors },
  } = useForm<ReportForm>({
    mode: "onChange",
    defaultValues: { title: '', description: '', category: '', urgency: 3, impact: 3, affectedPeople: 10 }
  });

  const currentUrgency = watch('urgency');
  const currentImpact = watch('impact');
  const currentTitleLength = watch('title')?.length ?? 0;
  const currentDescriptionLength = watch('description')?.length ?? 0;

  const categories = [
    { label: t('categories.safety'), value: 'safety', icon: 'pi-shield' },
    { label: t('categories.infrastructure'), value: 'infrastructure', icon: 'pi-building' },
    { label: t('categories.environment'), value: 'environment', icon: 'pi-sun' },
    { label: t('categories.social'), value: 'social', icon: 'pi-users' },
    { label: t('categories.mobility'), value: 'mobility', icon: 'pi-car' },
    { label: t('categories.education'), value: 'education', icon: 'pi-book' },
  ];

  const onSubmit = async (data: ReportForm) => {
    try {
      await apiClient.post("signals", data);
      toast.success(t('report.success'));
      navigate("/");
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('common.error'));
    }
  };

  const handleDiscard = () => {
    navigate("/");
  };

  const getScaleColor = (val: number) => {
    if (val <= 2) return 'var(--status-resolved)';
    if (val <= 3) return 'var(--status-progress)';
    return 'var(--status-rejected)';
  };

  return (
    <Layout>
      <div className="animate-fade-up max-w-60rem mx-auto pb-8">
        <CivicPageHeader title={t('report.title')} description={t('report.desc')} />

        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="grid">
            <div className="col-12 lg:col-7">
              <CivicCard title={t('report.title')} className="mb-6">
                <CivicField label={t('report.issue_title')} error={errors.title?.message}>
                  <Controller name="title" control={control} rules={{ required: t('common.required'), minLength: { value: FORM_LIMITS.report.titleMin, message: t('report.title_too_short') }, maxLength: { value: FORM_LIMITS.report.titleMax, message: t('report.title_too_long') } }} 
                    render={({ field, fieldState }) => (
                      <div className="flex flex-column gap-2">
                        <InputText
                          {...field}
                          id="report-title"
                          className={classNames('w-full', { 'p-invalid': fieldState.error })}
                          placeholder={t('report.issue_title_placeholder')}
                          data-testid="report-title-input"
                          maxLength={FORM_LIMITS.report.titleMax}
                        />
                        <CivicCharacterCount current={currentTitleLength} max={FORM_LIMITS.report.titleMax} min={FORM_LIMITS.report.titleMin} />
                      </div>
                    )} 
                  />
                </CivicField>

                <div className="grid">
                  <div className="col-12 md:col-6">
                    <CivicField label={t('common.category')} error={errors.category?.message}>
                      <Controller name="category" control={control} rules={{ required: t('common.required') }} 
                        render={({ field, fieldState }) => (
                          <CivicSelect
                            value={field.value}
                            onChange={(e) => field.onChange(e.value)}
                            options={categories}
                            placeholder={t('common.select_category')}
                            inputId="report-category"
                            className={classNames('w-full', { 'p-invalid': fieldState.error })}
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
                  </div>
                  <div className="col-12 md:col-6">
                    <CivicField label={t('report.scale')}>
                      <Controller name="affectedPeople" control={control} 
                        render={({ field }) => (
                          <div className="flex flex-column gap-3 p-3 border-round-xl bg-black-alpha-20 border-1 border-white-alpha-10">
                            <div className="flex justify-content-between font-black text-main">
                              <span className="text-xs uppercase opacity-50">Citizens</span>
                              <span className="text-brand-primary">{field.value}</span>
                            </div>
                            <Slider value={field.value} onChange={(e) => field.onChange(e.value)} min={1} max={1000} className="w-full" />
                          </div>
                        )} 
                      />
                    </CivicField>
                  </div>
                </div>

                <CivicField label={t('report.context')} error={errors.description?.message}>
                  <Controller name="description" control={control} rules={{ required: t('common.required'), minLength: { value: FORM_LIMITS.report.descriptionMin, message: t('report.desc_too_short') }, maxLength: { value: FORM_LIMITS.report.descriptionMax, message: t('report.desc_too_long') } }} 
                    render={({ field }) => (
                      <div className="flex flex-column gap-2">
                        <InputTextarea
                          {...field}
                          id="report-description"
                          rows={6}
                          className="w-full"
                          placeholder={t('report.context_placeholder')}
                          data-testid="report-description-textarea"
                          maxLength={FORM_LIMITS.report.descriptionMax}
                        />
                        <CivicCharacterCount current={currentDescriptionLength} max={FORM_LIMITS.report.descriptionMax} min={FORM_LIMITS.report.descriptionMin} />
                      </div>
                      )} 
                  />
                </CivicField>
              </CivicCard>
            </div>

            <div className="col-12 lg:col-5">
              <CivicCard title={t('signals.why_ranked_title')} variant="brand" className="mb-6">
                <div className="flex flex-column gap-8 py-4">
                  {/* Urgency Picker */}
                  <div className="flex flex-column gap-4">
                    <div className="flex justify-content-between align-items-end">
                      <div>
                        <label className="text-xs font-black uppercase tracking-widest text-main block mb-1">{t('report.urgency')}</label>
                        <span className="text-xs text-muted font-bold">{t('signals.urgency_formula')}</span>
                      </div>
                      <span className="text-3xl font-black" style={{ color: getScaleColor(currentUrgency) }}>{currentUrgency}</span>
                    </div>
                    <div className="p-4 border-round-2xl transition-colors duration-500 shadow-inner" style={{ background: `linear-gradient(135deg, ${getScaleColor(currentUrgency)}15 0%, transparent 100%)`, border: `1px solid ${getScaleColor(currentUrgency)}30` }}>
                      <div data-testid="report-urgency-slider">
                      <Controller name="urgency" control={control} render={({ field }) => (
                        <Slider value={field.value} onChange={(e) => field.onChange(e.value)} min={1} max={5} step={1} className="w-full" />
                      )} />
                      </div>
                      <div className="flex justify-content-between mt-4 text-min font-black uppercase tracking-tighter opacity-50">
                        <span>{t('report.urgency_low')}</span>
                        <span>{t('report.urgency_critical')}</span>
                      </div>
                    </div>
                  </div>

                  {/* Impact Picker */}
                  <div className="flex flex-column gap-4">
                    <div className="flex justify-content-between align-items-end">
                      <div>
                        <label className="text-xs font-black uppercase tracking-widest text-main block mb-1">{t('report.impact')}</label>
                        <span className="text-xs text-muted font-bold">{t('signals.impact_formula')}</span>
                      </div>
                      <span className="text-3xl font-black" style={{ color: getScaleColor(currentImpact) }}>{currentImpact}</span>
                    </div>
                    <div className="p-4 border-round-2xl transition-colors duration-500 shadow-inner" style={{ background: `linear-gradient(135deg, ${getScaleColor(currentImpact)}15 0%, transparent 100%)`, border: `1px solid ${getScaleColor(currentImpact)}30` }}>
                      <div data-testid="report-impact-slider">
                      <Controller name="impact" control={control} render={({ field }) => (
                        <Slider value={field.value} onChange={(e) => field.onChange(e.value)} min={1} max={5} step={1} className="w-full" />
                      )} />
                      </div>
                      <div className="flex justify-content-between mt-4 text-min font-black uppercase tracking-tighter opacity-50">
                        <span>{t('report.impact_minor')}</span>
                        <span>{t('report.impact_systemic')}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </CivicCard>

              <CivicCard title={t('auth.verify_protocol')}>
                {!activeCommunityId ? (
                  <div className="flex flex-column gap-3">
                    <div className="p-4 border-round-xl bg-status-rejected-alpha-10 border-1 border-status-rejected-alpha-20 text-status-rejected text-sm font-bold flex align-items-center gap-3">
                      <i className="pi pi-lock text-xl"></i>
                      {t('report.community_required')}
                    </div>
                    <CivicButton
                      label={t('nav.communities')}
                      icon="pi pi-users"
                      variant="secondary"
                      className="w-full py-3"
                      onClick={() => navigate("/communities")}
                      data-testid="report-go-communities-button"
                    />
                  </div>
                ) : (
                  <div className="flex flex-column gap-4">
                    <div className="flex align-items-center gap-3 p-3 bg-white-alpha-5 border-round-xl border-1 border-white-alpha-10">
                      <i className="pi pi-shield text-brand-primary text-xl"></i>
                      <div className="flex flex-column">
                        <span className="text-xs font-bold text-main uppercase">{t('settings.encryption')}</span>
                        <span className="text-min text-muted">{t('settings.encryption_value')}</span>
                      </div>
                    </div>
                    <CivicButton
                      label={t('common.discard')}
                      variant="secondary"
                      className="w-full py-4 text-lg"
                      onClick={handleDiscard}
                      data-testid="report-discard-button"
                    />
                    <CivicButton
                      type="submit"
                      label={t('report.submit')}
                      icon="pi pi-bolt"
                      loading={isSubmitting}
                      className="w-full py-4 text-lg"
                      glow
                      data-testid="report-submit-button"
                    />
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
