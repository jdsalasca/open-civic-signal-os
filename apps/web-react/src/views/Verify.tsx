import { useNavigate, useLocation } from "react-router-dom";
import { useForm, Controller } from "react-hook-form";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { Layout } from "../components/Layout";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicField } from "../components/ui/CivicField";

type VerifyForm = {
  code: string;
};

export function Verify() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const username = location.state?.username;
  
  const { control, handleSubmit, formState: { isSubmitting, errors } } = useForm<VerifyForm>();

  if (!username) {
    navigate("/login");
    return null;
  }

  const onSubmit = async (data: VerifyForm) => {
    try {
      await apiClient.post("auth/verify", { username, code: data.code });
      toast.success(t('auth.verified_success'));
      navigate("/login");
    } catch (err: any) {
      toast.error(err.friendlyMessage || "Invalid verification code.");
    }
  };

  const handleResend = async () => {
    try {
      await apiClient.post("auth/resend-code", { username });
      toast.success(t('auth.code_resent') || "Verification code resent to your email.");
    } catch (err: any) {
      toast.error(err.friendlyMessage || "Failed to resend code.");
    }
  };

  return (
    <Layout authMode>
      <div className="min-h-screen flex justify-content-center align-items-center p-4">
        <CivicCard className="w-full max-w-28rem animate-fade-up text-center" padding="lg">
          <div className="mb-8">
            <div className="inline-flex align-items-center justify-content-center p-3 bg-brand-primary-alpha-10 border-round-2xl mb-4 border-1 border-brand-primary-alpha-20">
              <i className="pi pi-shield text-4xl text-brand-primary"></i>
            </div>
            <h1 className="text-4xl font-black text-main m-0 tracking-tighter">{t('auth.activate_title')}</h1>
            <p className="text-secondary mt-2 font-medium">{t('auth.activate_subtitle')}</p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="flex flex-column gap-2 text-left" aria-label="Verification Form">
            <CivicField 
              label="Verification Code" 
              error={errors.code ? t('common.required') : undefined}
              helpText={t('auth.verify_protocol')}
            >
              <Controller name="code" control={control} rules={{ required: true, minLength: 6, maxLength: 6 }} 
                render={({ field }) => (
                  <InputText 
                    {...field} 
                    id="verify-code" 
                    placeholder="000000" 
                    className="text-center text-4xl font-black tracking-widest py-3 w-full"
                    autoFocus
                    maxLength={6}
                  />
                )} 
              />
            </CivicField>

            <CivicButton 
              type="submit" 
              label={t('auth.verify_button')} 
              icon="pi pi-check-shield" 
              className="py-4 text-base mt-2" 
              loading={isSubmitting} 
              glow
            />
            
            <div className="mt-8 pt-6 border-top-1 border-white-alpha-10 flex flex-column gap-3 text-center">
              <p className="text-muted text-sm m-0">Didn't receive the code?</p>
              <CivicButton 
                type="button" 
                label={t('auth.resend_button') || "Dispatch New Code"} 
                variant="ghost"
                onClick={handleResend}
                className="text-xs"
              />
            </div>
          </form>
        </CivicCard>
      </div>
    </Layout>
  );
}
