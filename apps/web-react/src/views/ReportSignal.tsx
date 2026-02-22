import { useForm, Controller } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
import { Dropdown } from "primereact/dropdown";
import { Slider } from "primereact/slider";
import { classNames } from "primereact/utils";
import { Layout } from "../components/Layout";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { useCommunityStore } from "../store/useCommunityStore";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicField } from "../components/ui/CivicField";

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

  const getScaleColor = (val: number) => {
    if (val <= 2) return 'var(--status-resolved)';
    if (val <= 3) return 'var(--status-progress)';
    return 'var(--status-rejected)';
  };

  return (
    <Layout>
      <div className="animate-fade-up max-w-60rem mx-auto pb-8">
        <div className="mb-8">
          <h1 className="text-5xl font-black mb-2 text-main tracking-tighter">Submit Intelligence</h1>
          <p className="text-secondary text-lg font-medium">Record a new civic signal for community prioritization.</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="grid">
            <div className="col-12 lg:col-7">
              <CivicCard title="Signal Metadata" className="mb-6">
                <CivicField label="Signal Headline" error={errors.title?.message}>
                  <Controller name="title" control={control} rules={{ required: t('common.required'), minLength: { value: 5, message: t('report.title_too_short') } }} 
                    render={({ field, fieldState }) => (
                      <InputText
                        {...field}
                        className={classNames('w-full', { 'p-invalid': fieldState.error })}
                        placeholder="What is the problem?"
                      />
                    )} 
                  />
                </CivicField>

                <div className="grid">
                  <div className="col-12 md:col-6">
                    <CivicField label="Civic Category">
                      <Controller name="category" control={control} rules={{ required: t('common.required') }} 
                        render={({ field }) => (
                          <Dropdown
                            value={field.value}
                            onChange={(e) => field.onChange(e.value)}
                            options={categories}
                            placeholder="Select Type"
                            className="w-full bg-black-alpha-20"
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
                    <CivicField label="Estimated Scope">
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

                <CivicField label="Deep Context" error={errors.description?.message}>
                  <Controller name="description" control={control} rules={{ required: t('common.required'), minLength: { value: 20, message: t('report.desc_too_short') } }} 
                    render={({ field }) => (
                      <InputTextarea
                        {...field}
                        rows={6}
                        className="w-full"
                        placeholder="Describe the situation, location, and potential consequences..."
                      />
                    )} 
                  />
                </CivicField>
              </CivicCard>
            </div>

            <div className="col-12 lg:col-5">
              <CivicCard title="Prioritization Factors" variant="brand" className="mb-6">
                <div className="flex flex-column gap-8 py-4">
                  {/* Urgency Picker */}
                  <div className="flex flex-column gap-4">
                    <div className="flex justify-content-between align-items-end">
                      <div>
                        <label className="text-xs font-black uppercase tracking-widest text-main block mb-1">Urgency Level</label>
                        <span className="text-xs text-muted font-bold">How fast does this need a response?</span>
                      </div>
                      <span className="text-3xl font-black" style={{ color: getScaleColor(currentUrgency) }}>{currentUrgency}</span>
                    </div>
                    <div className="p-4 border-round-2xl transition-colors duration-500 shadow-inner" style={{ background: `linear-gradient(135deg, ${getScaleColor(currentUrgency)}15 0%, transparent 100%)`, border: `1px solid ${getScaleColor(currentUrgency)}30` }}>
                      <Controller name="urgency" control={control} render={({ field }) => (
                        <Slider value={field.value} onChange={(e) => field.onChange(e.value)} min={1} max={5} step={1} className="w-full" />
                      )} />
                      <div className="flex justify-content-between mt-4 text-min font-black uppercase tracking-tighter opacity-50">
                        <span>Low Priority</span>
                        <span>Immediate Action</span>
                      </div>
                    </div>
                  </div>

                  {/* Impact Picker */}
                  <div className="flex flex-column gap-4">
                    <div className="flex justify-content-between align-items-end">
                      <div>
                        <label className="text-xs font-black uppercase tracking-widest text-main block mb-1">Social Impact</label>
                        <span className="text-xs text-muted font-bold">Severity of the problem's effects.</span>
                      </div>
                      <span className="text-3xl font-black" style={{ color: getScaleColor(currentImpact) }}>{currentImpact}</span>
                    </div>
                    <div className="p-4 border-round-2xl transition-colors duration-500 shadow-inner" style={{ background: `linear-gradient(135deg, ${getScaleColor(currentImpact)}15 0%, transparent 100%)`, border: `1px solid ${getScaleColor(currentImpact)}30` }}>
                      <Controller name="impact" control={control} render={({ field }) => (
                        <Slider value={field.value} onChange={(e) => field.onChange(e.value)} min={1} max={5} step={1} className="w-full" />
                      )} />
                      <div className="flex justify-content-between mt-4 text-min font-black uppercase tracking-tighter opacity-50">
                        <span>Minor Nuisance</span>
                        <span>Systemic Failure</span>
                      </div>
                    </div>
                  </div>
                </div>
              </CivicCard>

              <CivicCard title="Protocol Validation">
                {!activeCommunityId ? (
                  <div className="p-4 border-round-xl bg-status-rejected-alpha-10 border-1 border-status-rejected-alpha-20 text-status-rejected text-sm font-bold flex align-items-center gap-3">
                    <i className="pi pi-lock text-xl"></i>
                    No Community Active. Access Denied.
                  </div>
                ) : (
                  <div className="flex flex-column gap-4">
                    <div className="flex align-items-center gap-3 p-3 bg-white-alpha-5 border-round-xl border-1 border-white-alpha-10">
                      <i className="pi pi-shield text-brand-primary text-xl"></i>
                      <div className="flex flex-column">
                        <span className="text-xs font-bold text-main uppercase">Encryption Verified</span>
                        <span className="text-min text-muted">Ready for secure transmission</span>
                      </div>
                    </div>
                    <CivicButton
                      type="submit"
                      label="Dispatch Signal"
                      icon="pi pi-bolt"
                      loading={isSubmitting}
                      className="w-full py-4 text-lg"
                      glow
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
