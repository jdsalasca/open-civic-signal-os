import { useMemo, useState } from "react";
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

type VerifyLocationState = {
  username?: string;
  emailDeliveryFailed?: boolean;
  supportEmail?: string;
  deliveryMessage?: string;
};

type ResendResponse = {
  message?: string;
  emailDeliveryStatus?: "SENT" | "FAILED";
  supportEmail?: string;
};

export function Verify() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const routeState = (location.state as VerifyLocationState | null) ?? null;
  const username = routeState?.username;
  const [resendCooldownSeconds, setResendCooldownSeconds] = useState(0);
  const [lastResendFailed, setLastResendFailed] = useState(routeState?.emailDeliveryFailed ?? false);
  const [supportEmail, setSupportEmail] = useState(routeState?.supportEmail || "support@open-civic.local");
  const [deliveryMessage, setDeliveryMessage] = useState(routeState?.deliveryMessage || "");
  
  const { control, handleSubmit, formState: { isSubmitting, errors } } = useForm<VerifyForm>();
  const canResend = resendCooldownSeconds === 0;

  const resendLabel = useMemo(() => {
    if (resendCooldownSeconds === 0) {
      return t('auth.resend_button');
    }
    return t('auth.resend_button_wait', { seconds: resendCooldownSeconds });
  }, [resendCooldownSeconds, t]);

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
    if (!canResend) {
      return;
    }
    try {
      const res = await apiClient.post<ResendResponse>("auth/resend-code", { username });
      const emailFailed = res.data?.emailDeliveryStatus === "FAILED";
      setLastResendFailed(emailFailed);
      setDeliveryMessage(res.data?.message || "");
      if (res.data?.supportEmail) {
        setSupportEmail(res.data.supportEmail);
      }
      setResendCooldownSeconds(30);
      const interval = window.setInterval(() => {
        setResendCooldownSeconds((current) => {
          if (current <= 1) {
            window.clearInterval(interval);
            return 0;
          }
          return current - 1;
        });
      }, 1000);

      if (emailFailed) {
        toast.error(t('auth.code_resend_degraded'));
      } else {
        toast.success(t('auth.code_resent'));
      }
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

          {lastResendFailed && (
            <div className="mb-5 p-3 border-round-xl border-1 border-white-alpha-20 bg-white-alpha-5 text-sm text-left line-height-3">
              <div className="font-bold mb-2 text-main">
                <i className="pi pi-exclamation-triangle mr-2 text-status-progress" />
                {t('auth.email_delivery_warning_title')}
              </div>
              <div className="mb-2">{deliveryMessage || t('auth.email_delivery_warning_body')}</div>
              <div>{t('auth.email_delivery_support', { email: supportEmail })}</div>
            </div>
          )}

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
                label={resendLabel}
                variant="ghost"
                onClick={handleResend}
                className="text-xs"
                disabled={!canResend}
              />
              <p className="text-xs text-secondary m-0">{t('auth.email_delivery_help')}</p>
            </div>
          </form>
        </CivicCard>
      </div>
    </Layout>
  );
}
