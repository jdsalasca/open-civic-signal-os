import { useCallback, useEffect, useState } from "react";
import { useForm, Controller } from "react-hook-form";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { Password } from "primereact/password";
import { ProgressSpinner } from "primereact/progressspinner";
import { classNames } from "primereact/utils";
import { useAuthStore } from "../store/useAuthStore";
import { ShieldCheck } from "lucide-react";
import { Layout } from "../components/Layout";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicField } from "../components/ui/CivicField";
import { GoogleSignIn } from "../components/GoogleSignIn";

interface ApiError extends Error {
  friendlyMessage?: string;
}

type LoginForm = {
  username: string;
  password: string;
};

type AuthConfig = {
  clientId?: string;
  passwordLoginEnabled?: boolean;
};

export function Login() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const setAuth = useAuthStore((state) => state.setAuth);
  const { control, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginForm>({
    defaultValues: { username: '', password: '' }
  });
  const [configLoaded, setConfigLoaded] = useState(false);
  const [googleClientId, setGoogleClientId] = useState('');
  const [passwordLoginEnabled, setPasswordLoginEnabled] = useState(true);

  useEffect(() => {
    let cancelled = false;
    apiClient
      .get<AuthConfig>("auth/google/config")
      .then(({ data }) => {
        if (cancelled) {
          return;
        }
        setGoogleClientId(data?.clientId ?? '');
        setPasswordLoginEnabled(data?.passwordLoginEnabled !== false);
      })
      .catch(() => {
        // If the config cannot be read, keep the password form as a safe fallback.
      })
      .finally(() => {
        if (!cancelled) {
          setConfigLoaded(true);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const afterSignIn = useCallback(async () => {
    const pendingCommunityId = sessionStorage.getItem('pendingCommunityId');
    if (pendingCommunityId) {
      sessionStorage.removeItem('pendingCommunityId');
      try {
        await apiClient.post(`communities/${pendingCommunityId}/join`, { role: 'MEMBER' });
        navigate('/');
        return;
      } catch {
        // Fall through to the normal memberships check.
      }
    }
    try {
      const memberships = await apiClient.get("communities/my");
      navigate(memberships.data?.length ? "/" : "/onboarding");
    } catch {
      navigate("/");
    }
  }, [navigate]);

  const onSubmit = async (data: LoginForm) => {
    try {
      const res = await apiClient.post("auth/login", data);
      setAuth(res.data);
      toast.success(t('auth.login_success', { name: res.data.username }));
      await afterSignIn();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('auth.invalid_credentials'));
    }
  };

  const handleGoogleCredential = useCallback(async (credential: string) => {
    try {
      const res = await apiClient.post("auth/google", { credential });
      setAuth(res.data);
      toast.success(t('auth.login_success', { name: res.data.username }));
      await afterSignIn();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('auth.invalid_credentials'));
    }
  }, [afterSignIn, setAuth, t]);

  return (
    <Layout authMode>
      <div className="min-h-screen flex justify-content-center align-items-center p-4">
        <CivicCard className="auth-panel animate-fade-up" padding="lg" data-testid="login-card">
          <div className="text-center mb-8">
            <div className="inline-flex align-items-center justify-content-center p-3 bg-surface-soft border-round-2xl mb-4 border-1 border-subtle">
              <ShieldCheck size={48} className="text-brand-primary" />
            </div>
            <h1 className="text-4xl font-black text-main m-0 tracking-tighter">{t('auth.login_title')}</h1>
            <p className="text-secondary mt-2 font-medium">{t('auth.login_subtitle')}</p>
          </div>

          {!configLoaded ? (
            <div className="flex justify-content-center py-6" data-testid="login-config-loading">
              <ProgressSpinner style={{ width: '2rem', height: '2rem' }} strokeWidth="6" />
            </div>
          ) : passwordLoginEnabled ? (
            <>
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
                          className={classNames('w-full p-inputtext-lg pl-5 text-sm sm:text-base', { 'p-invalid': fieldState.error })}
                          data-testid="login-username-input"
                        />
                      </span>
                    )}
                  />
                </CivicField>

                <CivicField
                  label={t('auth.password')}
                  error={errors.password ? t('common.required') : undefined}
                  helpText={t('auth.login_password_hint')}
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
                        inputClassName="w-full p-inputtext-lg text-sm sm:text-base"
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
              </form>

              <div className="auth-divider my-4" aria-hidden="true">
                <span>{t('auth.or_continue_with')}</span>
              </div>
              <GoogleSignIn clientId={googleClientId} onCredential={handleGoogleCredential} />

              <div className="text-center mt-8 pt-6 border-top-1 border-surface-soft">
                <span className="text-secondary mr-2 font-medium">{t('auth.no_account')}</span>
                <Link to="/register" className="text-brand-primary font-bold no-underline hover:underline" data-testid="go-to-register">
                  {t('auth.create_one')}
                </Link>
              </div>
            </>
          ) : (
            <div className="google-only-panel" data-testid="google-only-panel">
              <p className="text-secondary text-sm text-center mb-4">{t('auth.google_only_hint')}</p>
              <GoogleSignIn clientId={googleClientId} onCredential={handleGoogleCredential} />
              <p className="text-xs text-muted text-center mt-4">{t('auth.no_password_note')}</p>
            </div>
          )}

          <div className="text-center mt-4">
            <Link
              to="/backlog"
              className="text-secondary text-sm no-underline hover:underline"
              data-testid="go-to-public-backlog"
            >
              {t('public_backlog.title')}
            </Link>
          </div>
        </CivicCard>
      </div>
    </Layout>
  );
}
