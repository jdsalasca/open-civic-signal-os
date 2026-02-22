import { useForm, Controller } from "react-hook-form";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { Password } from "primereact/password";
import { classNames } from "primereact/utils";
import { useAuthStore } from "../store/useAuthStore";
import { ShieldCheck } from "lucide-react";
import { Layout } from "../components/Layout";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicField } from "../components/ui/CivicField";

interface ApiError extends Error {
  friendlyMessage?: string;
}

type LoginForm = {
  username: string;
  password: string;
};

export function Login() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const setAuth = useAuthStore((state) => state.setAuth);
  const { control, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginForm>({
    defaultValues: { username: '', password: '' }
  });

  const onSubmit = async (data: LoginForm) => {
    try {
      const res = await apiClient.post("auth/login", data);
      setAuth(res.data);
      toast.success(t('auth.login_success', { name: res.data.username }));
      navigate("/");
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('auth.invalid_credentials'));
    }
  };

  return (
    <Layout authMode>
      <div className="min-h-screen flex justify-content-center align-items-center p-4">
        <CivicCard className="w-full max-w-28rem animate-fade-up" padding="lg">
          <div className="text-center mb-8">
            <div className="inline-flex align-items-center justify-content-center p-3 bg-brand-primary-alpha-10 border-round-2xl mb-4 border-1 border-brand-primary-alpha-20">
              <ShieldCheck size={48} className="text-brand-primary" />
            </div>
            <h1 className="text-4xl font-black text-main m-0 tracking-tighter">{t('auth.login_title')}</h1>
            <p className="text-secondary mt-2 font-medium">{t('auth.login_subtitle')}</p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="flex flex-column" aria-label="Login Form">
            <CivicField 
              label={t('auth.username')} 
              error={errors.username ? t('common.required') : undefined}
            >
              <Controller 
                name="username" 
                control={control} 
                rules={{ required: true }} 
                render={({ field, fieldState }) => (
                  <span className="p-input-icon-left w-full">
                    <i className="pi pi-user text-muted" />
                    <InputText 
                      {...field} 
                      autoFocus
                      autoComplete="username"
                      placeholder={t('auth.username_placeholder')}
                      className={classNames('w-full p-inputtext-lg pl-5', { 'p-invalid': fieldState.error })} 
                      data-testid="login-username-input"
                    />
                  </span>
                )} 
              />
            </CivicField>

            <CivicField 
              label={t('auth.password')} 
              error={errors.password ? t('common.required') : undefined}
            >
              <Controller 
                name="password" 
                control={control} 
                rules={{ required: true }} 
                render={({ field, fieldState }) => (
                  <Password 
                    {...field} 
                    toggleMask 
                    feedback={false} 
                    autoComplete="current-password"
                    placeholder={t('auth.password_placeholder')}
                    inputClassName="w-full p-inputtext-lg"
                    className={classNames('w-full', { 'p-invalid': fieldState.error })} 
                    data-testid="login-password-input"
                  />
                )} 
              />
            </CivicField>

            <CivicButton 
              type="submit" 
              label={t('auth.sign_in_button')} 
              icon="pi pi-sign-in" 
              className="py-4 text-base mt-2" 
              loading={isSubmitting}
              glow
              data-testid="login-submit-button"
            />
            
            <div className="text-center mt-8 pt-6 border-top-1 border-white-alpha-10">
              <span className="text-secondary mr-2 font-medium">{t('auth.no_account')}</span>
              <Link to="/register" className="text-brand-primary font-bold no-underline hover:underline" data-testid="go-to-register">
                {t('auth.create_one')}
              </Link>
            </div>
          </form>
        </CivicCard>
      </div>
    </Layout>
  );
}
