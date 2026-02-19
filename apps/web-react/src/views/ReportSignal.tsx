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
  const { control, handleSubmit, formState: { errors, isSubmitting } } = useForm<ReportForm>({
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
          <p className="text-gray-500 mb-5">{t('report.desc')}</p>
          
          <form onSubmit={handleSubmit(onSubmit)} className="p-fluid grid">
            <div className="field col-12">
              <label htmlFor="title" className="font-bold block mb-2">{t('report.issue_title')}</label>
              <Controller name="title" control={control} rules={{ required: true }} 
                render={({ field, fieldState }) => (
                  <InputText id="title" {...field} className={classNames({ 'p-invalid': fieldState.error })} placeholder={t('report.issue_title_placeholder')} />
                )} 
              />
              {errors.title && <small className="p-error">{t('common.required')}</small>}
            </div>

            <div className="field col-12 md:col-6">
              <label htmlFor="category" className="font-bold block mb-2">{t('common.category')}</label>
              <Controller name="category" control={control} rules={{ required: true }} 
                render={({ field, fieldState }) => (
                  <Dropdown id="category" {...field} options={categories} className={classNames({ 'p-invalid': fieldState.error })} placeholder={t('common.select_category')} />
                )} 
              />
            </div>

            <div className="field col-12 md:col-6">
              <label htmlFor="affectedPeople" className="font-bold block mb-2">{t('report.scale')}</label>
              <Controller name="affectedPeople" control={control} 
                render={({ field }) => (
                  <div className="flex align-items-center gap-3 bg-gray-900 p-2 border-round border-1 border-white-alpha-10">
                    <Slider {...field} min={1} max={1000} className="flex-grow-1" />
                    <span className="font-black text-cyan-400" style={{ minWidth: '40px' }}>{field.value}</span>
                  </div>
                )} 
              />
            </div>

            <div className="field col-12">
              <label htmlFor="description" className="font-bold block mb-2">{t('report.context')}</label>
              <Controller name="description" control={control} rules={{ required: true }} 
                render={({ field, fieldState }) => (
                  <InputTextarea id="description" {...field} rows={4} className={classNames({ 'p-invalid': fieldState.error })} placeholder={t('report.context_placeholder')} />
                )} 
              />
            </div>

            <div className="field col-12 md:col-6">
              <div className="flex justify-content-between mb-2">
                <label className="font-bold">{t('report.urgency')}</label>
                <span className="text-xs text-gray-500 uppercase font-black">{t('common.score')}: 1-5</span>
              </div>
              <Controller name="urgency" control={control} render={({ field }) => (
                <div className="p-3 bg-black-alpha-20 border-round">
                  <Slider {...field} min={1} max={5} step={1} />
                  <div className="flex justify-content-between mt-2 text-xs font-bold text-gray-600">
                    <span>{t('report.urgency_low')}</span>
                    <span className="text-cyan-500">{field.value}</span>
                    <span>{t('report.urgency_critical')}</span>
                  </div>
                </div>
              )} />
            </div>

            <div className="field col-12 md:col-6">
              <div className="flex justify-content-between mb-2">
                <label className="font-bold">{t('report.impact')}</label>
                <span className="text-xs text-gray-500 uppercase font-black">{t('common.score')}: 1-5</span>
              </div>
              <Controller name="impact" control={control} render={({ field }) => (
                <div className="p-3 bg-black-alpha-20 border-round">
                  <Slider {...field} min={1} max={5} step={1} />
                  <div className="flex justify-content-between mt-2 text-xs font-bold text-gray-600">
                    <span>{t('report.impact_minor')}</span>
                    <span className="text-purple-500">{field.value}</span>
                    <span>{t('report.impact_systemic')}</span>
                  </div>
                </div>
              )} />
            </div>

            <div className="col-12 mt-4 flex gap-3">
              <Button type="button" label={t('common.discard')} outlined className="p-button-secondary border-gray-700" onClick={() => navigate("/")} />
              <Button type="submit" label={t('report.submit')} icon="pi pi-bolt" loading={isSubmitting} className="p-button-primary" />
            </div>
          </form>
        </Card>
      </div>
    </Layout>
  );
}
