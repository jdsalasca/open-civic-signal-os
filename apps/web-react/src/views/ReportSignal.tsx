import { useForm, Controller } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
import { Dropdown } from "primereact/dropdown";
import { Slider } from "primereact/slider";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { classNames } from "primereact/utils";
import { Layout } from "../components/Layout";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { useCommunityStore } from "../store/useCommunityStore";

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
    formState: { isSubmitting },
  } = useForm<ReportForm>({
    mode: "onChange",
    defaultValues: { title: '', description: '', category: '', urgency: 3, impact: 3, affectedPeople: 10 }
  });

  const categories = [
    { label: t('categories.safety'), value: 'safety' },
    { label: t('categories.infrastructure'), value: 'infrastructure' },
    { label: t('categories.environment'), value: 'environment' },
    { label: t('categories.social'), value: 'social' },
    { label: t('categories.mobility'), value: 'mobility' },
    { label: t('categories.education'), value: 'education' },
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

  return (
    <Layout>
      <div className="flex justify-content-center mt-4 pb-6">
        <Card title={t('report.title')} style={{ width: '100%', maxWidth: '700px' }} className="shadow-8 border-round-2xl">
          <p className="text-muted mb-5">{t('report.desc')}</p>
          {!activeCommunityId && (
            <div className="mb-4 p-3 border-round border-1 border-yellow-500 text-yellow-200 bg-yellow-900-alpha-20">
              {t('report.community_required')}
            </div>
          )}
          
          <form onSubmit={handleSubmit(onSubmit)} className="p-fluid grid">
            <div className="field col-12">
              <label htmlFor="title" className="font-bold block mb-2 text-main text-sm uppercase tracking-wider">{t('report.issue_title')}</label>
              <Controller name="title" control={control} rules={{ required: t('common.required'), minLength: { value: 5, message: t('report.title_too_short') } }} 
                render={({ field, fieldState }) => (
                  <>
                    <InputText
                      id="title"
                      value={field.value}
                      onChange={(e) => field.onChange(e.target.value)}
                      className={classNames('w-full', { 'p-invalid': fieldState.error })}
                      placeholder={t('report.issue_title_placeholder')}
                    />
                    {fieldState.error && <small className="p-error block mt-1">{fieldState.error.message}</small>}
                  </>
                )} 
              />
            </div>

            <div className="field col-12 md:col-6">
              <label htmlFor="category" className="font-bold block mb-2 text-main text-sm uppercase tracking-wider">{t('common.category')}</label>
              <Controller name="category" control={control} rules={{ required: t('common.required') }} 
                render={({ field, fieldState }) => (
                  <>
                    <Dropdown
                      id="category"
                      value={field.value}
                      onChange={(e) => field.onChange(e.value)}
                      options={categories}
                      className={classNames('w-full', { 'p-invalid': fieldState.error })}
                      placeholder={t('common.select_category')}
                    />
                    {fieldState.error && <small className="p-error block mt-1">{fieldState.error.message}</small>}
                  </>
                )} 
              />
            </div>

            <div className="field col-12 md:col-6">
              <label htmlFor="affectedPeople" className="font-bold block mb-2 text-main text-sm uppercase tracking-wider">{t('report.scale')}</label>
              <Controller name="affectedPeople" control={control} 
                render={({ field }) => (
                  <div className="flex flex-column gap-2 p-3 border-round border-1 border-subtle bg-black-alpha-10">
                    <div className="flex justify-content-between align-items-center mb-2">
                       <span className="text-sm text-muted">Estimated Citizens</span>
                       <span className="text-xl font-bold text-cyan-400">{field.value}</span>
                    </div>
                    <Slider
                      value={field.value}
                      onChange={(e) => field.onChange(e.value)}
                      min={1}
                      max={1000}
                      className="w-full"
                    />
                  </div>
                )} 
              />
            </div>

            <div className="field col-12">
              <label htmlFor="description" className="font-bold block mb-2 text-main text-sm uppercase tracking-wider">{t('report.context')}</label>
              <Controller name="description" control={control} rules={{ required: t('common.required'), minLength: { value: 20, message: t('report.desc_too_short') } }} 
                render={({ field, fieldState }) => (
                  <>
                    <InputTextarea
                      id="description"
                      value={field.value}
                      onChange={(e) => field.onChange(e.target.value)}
                      rows={4}
                      className={classNames('w-full', { 'p-invalid': fieldState.error })}
                      placeholder={t('report.context_placeholder')}
                    />
                    {fieldState.error && <small className="p-error block mt-1">{fieldState.error.message}</small>}
                  </>
                )} 
              />
            </div>

            <div className="field col-12 md:col-6">
              <div className="flex justify-content-between mb-2">
                <label className="font-bold text-main text-sm uppercase tracking-wider">{t('report.urgency')}</label>
                <span className="text-xs text-muted uppercase font-bold">{t('common.score')}: {control._formValues.urgency}/5</span>
              </div>
              <Controller name="urgency" control={control} render={({ field }) => (
                <div className="p-4 border-round border-1 border-subtle bg-black-alpha-10">
                  <Slider
                    value={field.value}
                    onChange={(e) => field.onChange(e.value)}
                    min={1}
                    max={5}
                    step={1}
                    className="w-full mb-3"
                  />
                  <div className="flex justify-content-between text-xs font-bold text-muted">
                    <span>{t('report.urgency_low')}</span>
                    <span>{t('report.urgency_critical')}</span>
                  </div>
                </div>
              )} />
            </div>

            <div className="field col-12 md:col-6">
              <div className="flex justify-content-between mb-2">
                <label className="font-bold text-main text-sm uppercase tracking-wider">{t('report.impact')}</label>
                <span className="text-xs text-muted uppercase font-bold">{t('common.score')}: {control._formValues.impact}/5</span>
              </div>
              <Controller name="impact" control={control} render={({ field }) => (
                <div className="p-4 border-round border-1 border-subtle bg-black-alpha-10">
                  <Slider
                    value={field.value}
                    onChange={(e) => field.onChange(e.value)}
                    min={1}
                    max={5}
                    step={1}
                    className="w-full mb-3"
                  />
                  <div className="flex justify-content-between text-xs font-bold text-muted">
                    <span>{t('report.impact_minor')}</span>
                    <span>{t('report.impact_systemic')}</span>
                  </div>
                </div>
              )} />
            </div>

            <div className="col-12 mt-4 flex gap-3">
              <Button type="button" label={t('common.discard')} outlined severity="secondary" onClick={() => navigate("/")} />
              <Button
                type="submit"
                label={t('report.submit')}
                icon="pi pi-bolt"
                loading={isSubmitting}
                disabled={isSubmitting}
                className="p-button-primary shadow-4"
                data-testid="report-submit-button"
              />
            </div>
          </form>
        </Card>
      </div>
    </Layout>
  );
}
