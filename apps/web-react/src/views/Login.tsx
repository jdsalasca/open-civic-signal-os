import { useForm, Controller } from "react-hook-form";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { Password } from "primereact/password";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { classNames } from "primereact/utils";
import { useAuthStore } from "../store/useAuthStore";
import { ShieldCheck } from "lucide-react";
import { Layout } from "../components/Layout";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";

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
  const { control, handleSubmit, formState: { errors } } = useForm<LoginForm>({
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
      <div className="auth-shell flex justify-content-center align-items-center">
        <Card 
          className="auth-card shadow-8 border-round-2xl border-1 border-white-alpha-10 animate-fade-in"
          style={{ width: '100%', maxWidth: '420px', background: 'var(--bg-surface)' }}
          data-testid="login-card"
        >
          <div className="text-center mb-5">
            <ShieldCheck size={60} className="text-cyan-500 mb-3" />
            <h1 className="text-3xl font-black text-main m-0 tracking-tight text-center">{t('auth.login_title')}</h1>
            <p className="text-muted mt-2">{t('auth.login_subtitle')}</p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="p-fluid" aria-label="Login Form">
            <div className="field mb-4">
              <label htmlFor="username-input" className="auth-field-label">{t('auth.username')}</label>
              <span className="p-input-icon-left">
                <i className="pi pi-user text-cyan-500" />
                <Controller 
                  name="username" 
                  control={control} 
                  rules={{ required: true }} 
                  render={({ field, fieldState }) => (
                    <InputText 
                      id="username-input" // Fixed ID
                      {...field} 
                      autoFocus
                      autoComplete="username"
                      placeholder={t('auth.username_placeholder')}
                      className={classNames('p-inputtext-lg py-3 pl-5', { 'p-invalid': fieldState.error })} 
                      data-testid="login-username-input"
                    />
                  )} 
                />
              </span>
              {errors.username && <small className="p-error block mt-1">{t('common.required')}</small>}
            </div>

            <div className="field mb-5">
              <label htmlFor="password-input" className="auth-field-label">{t('auth.password')}</label>
              <Controller 
                name="password" 
                control={control} 
                rules={{ required: true }} 
                render={({ field, fieldState }) => (
                  <Password 
                    id="login-password" 
                    {...field} 
                    toggleMask 
                    feedback={false} 
                    autoComplete="current-password"
                    inputId="password-input" // Fixed internal ID
                    placeholder={t('auth.password_placeholder')}
                    inputClassName="p-inputtext-lg py-3"
                    className={classNames('w-full', { 'p-invalid': fieldState.error })} 
                    data-testid="login-password-input"
                  />
                )} 
              />
              {errors.password && <small className="p-error block mt-1">{t('common.required')}</small>}
            </div>

            <Button 
              type="submit" 
              label={t('auth.sign_in_button')} 
              icon="pi pi-sign-in" 
              className="p-button-primary py-3 font-bold text-lg shadow-4" 
              data-testid="login-submit-button"
              aria-label="Submit Login"
            />
            
            <div className="text-center mt-5">
              <span className="text-muted mr-2">{t('auth.no_account')}</span>
              <Link to="/register" className="text-cyan-400 font-bold no-underline hover:underline" data-testid="go-to-register">{t('auth.create_one')}</Link>
            </div>
          </form>
        </Card>
      </div>
    </Layout>
  );
}
