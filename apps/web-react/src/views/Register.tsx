import { useForm, Controller } from "react-hook-form";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { Password } from "primereact/password";
import { classNames } from "primereact/utils";
import { Layout } from "../components/Layout";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicField } from "../components/ui/CivicField";

interface ApiError extends Error {
  friendlyMessage?: string;
}

type RegisterForm = {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
};

export function Register() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { control, handleSubmit, watch, formState: { errors, isSubmitting } } = useForm<RegisterForm>({
    defaultValues: { username: '', email: '', password: '', confirmPassword: '' }
  });

  const password = watch('password');

  const onSubmit = async (data: RegisterForm) => {
    try {
      const { confirmPassword, ...registerData } = data;
      const res = await apiClient.post("auth/register", registerData);

      if (res.status === 200 || res.status === 201) {
        toast.success(t('auth.register_success'));
        navigate("/verify", { state: { username: data.username } });
      }
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('common.error'));
    }
  };

  return (
    <Layout authMode>
      <div className="min-h-screen flex justify-content-center align-items-center p-4">
        <CivicCard className="w-full max-w-30rem animate-fade-up" padding="lg">
          <div className="text-center mb-8">
            <h1 className="text-4xl font-black text-main m-0 tracking-tighter">{t('auth.join_title')}</h1>
            <p className="text-secondary mt-2 font-medium">{t('auth.join_subtitle')}</p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="flex flex-column gap-2" aria-label="Registration Form">
            <CivicField label={t('auth.username')} error={errors.username?.message}>
              <Controller name="username" control={control} rules={{ required: t('common.required'), minLength: { value: 4, message: t('auth.username_too_short') } }} 
                render={({ field, fieldState }) => (
                  <InputText 
                    {...field} 
                    autoComplete="username"
                    className={classNames('w-full p-inputtext-lg', { 'p-invalid': fieldState.error })} 
                    data-testid="register-username-input"
                    placeholder={t('auth.username_placeholder')}
                  />
                )} 
              />
            </CivicField>

            <CivicField label={t('auth.email')} error={errors.email?.message}>
              <Controller name="email" control={control} rules={{ required: t('common.required'), pattern: { value: /^\S+@\S+$/i, message: t('common.invalid_email') } }} 
                render={({ field, fieldState }) => (
                  <InputText 
                    {...field} 
                    autoComplete="email"
                    className={classNames('w-full p-inputtext-lg', { 'p-invalid': fieldState.error })} 
                    data-testid="register-email-input"
                    placeholder="email@example.com"
                  />
                )} 
              />
            </CivicField>

            <CivicField label={t('auth.password')} error={errors.password?.message}>
              <Controller name="password" control={control} rules={{ required: t('common.required'), minLength: { value: 8, message: t('auth.password_too_short') } }} 
                render={({ field, fieldState }) => (
                  <Password 
                    {...field} 
                    toggleMask 
                    inputId="password-input" 
                    autoComplete="new-password"
                    placeholder="Min. 8 characters"
                    inputClassName="w-full p-inputtext-lg" 
                    className={classNames('w-full', { 'p-invalid': fieldState.error })} 
                    data-testid="register-password-input"
                  />
                )} 
              />
            </CivicField>

            <CivicField label={t('auth.confirm_password')} error={errors.confirmPassword?.message}>
              <Controller name="confirmPassword" control={control} 
                rules={{ 
                  required: t('common.required'), 
                  validate: value => value === password || t('auth.passwords_mismatch') || "Passwords must match"
                }} 
                render={({ field, fieldState }) => (
                  <Password 
                    {...field} 
                    toggleMask 
                    feedback={false}
                    inputId="confirm-password-input" 
                    autoComplete="new-password"
                    placeholder="Repeat password"
                    inputClassName="w-full p-inputtext-lg" 
                    className={classNames('w-full', { 'p-invalid': fieldState.error })} 
                    data-testid="register-confirm-password-input"
                  />
                )} 
              />
            </CivicField>

            <div className="p-4 bg-white-alpha-5 border-round-xl text-xs text-secondary flex align-items-center gap-3 border-1 border-white-alpha-10 mb-4">
              <i className="pi pi-shield text-brand-primary text-lg"></i>
              <span className="font-bold uppercase tracking-wider leading-relaxed">{t('auth.clearance_level')}</span>
            </div>

            <CivicButton 
              type="submit" 
              label={t('auth.create_account')} 
              className="py-4 text-base" 
              loading={isSubmitting}
              glow
              data-testid="register-submit-button"
            />
            
            <p className="text-center text-muted text-sm font-medium mt-6">
              {t('auth.already_member')} <Link to="/login" className="text-brand-primary font-bold no-underline hover:underline" data-testid="go-to-login">{t('auth.sign_in_here')}</Link>
            </p>
          </form>
        </CivicCard>
      </div>
    </Layout>
  );
}
