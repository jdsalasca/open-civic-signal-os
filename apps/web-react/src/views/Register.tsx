import { useEffect, useMemo, useState } from "react";
import { useForm, Controller } from "react-hook-form";
import { useNavigate, useSearchParams, Link } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { Password } from "primereact/password";
import { classNames } from "primereact/utils";
import { Layout } from "../components/Layout";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { Community } from "../types";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicField } from "../components/ui/CivicField";
import { CivicSelect } from "../components/ui/CivicSelect";

interface ApiError extends Error {
  friendlyMessage?: string;
}

type RegisterForm = {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  communityId: string;
  communityName: string;
  communityCountry: string;
  communityRegion: string;
};

type CityMode = "join" | "create";

type RegisterResponse = {
  message?: string;
  username?: string;
  emailDeliveryStatus?: "SENT" | "FAILED";
  supportEmail?: string;
  deliveryFailureReason?: string;
};

export function Register() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { control, handleSubmit, watch, formState: { errors, isSubmitting } } = useForm<RegisterForm>({
    defaultValues: {
      username: '',
      email: '',
      password: '',
      confirmPassword: '',
      communityId: searchParams.get('communityId') ?? '',
      communityName: '',
      communityCountry: '',
      communityRegion: '',
    }
  });

  const password = watch('password');
  const [cities, setCities] = useState<Community[]>([]);
  const [cityMode, setCityMode] = useState<CityMode>(
    searchParams.get('mode') === 'create' ? 'create' : 'join'
  );

  useEffect(() => {
    let cancelled = false;
    const loadCities = async () => {
      try {
        const response = await apiClient.get<Community[]>("communities/public");
        if (!cancelled) {
          setCities(response.data || []);
        }
      } catch {
        // The city directory is a convenience at registration. If it is unavailable, the person can
        // still create an account and join a city later rather than be blocked by a failed read.
      }
    };
    loadCities();
    return () => {
      cancelled = true;
    };
  }, []);

  const cityOptions = useMemo(
    () =>
      cities.map((city) => ({
        label: [city.name, [city.region, city.country].filter(Boolean).join(", ")]
          .filter((part) => part && part.length > 0)
          .join(" — "),
        value: city.id,
      })),
    [cities]
  );

  const onSubmit = async (data: RegisterForm) => {
    try {
      const payload: Record<string, string> = {
        username: data.username,
        email: data.email,
        password: data.password,
      };

      if (cityMode === 'join' && data.communityId) {
        payload.communityId = data.communityId;
      } else if (cityMode === 'create' && data.communityName.trim()) {
        payload.communityName = data.communityName.trim();
        if (data.communityCountry.trim()) {
          payload.communityCountry = data.communityCountry.trim();
        }
        if (data.communityRegion.trim()) {
          payload.communityRegion = data.communityRegion.trim();
        }
      }

      const res = await apiClient.post<RegisterResponse>("auth/register", payload);

      if (res.status === 200 || res.status === 201) {
        const emailDeliveryFailed = res.data?.emailDeliveryStatus === "FAILED";
        if (emailDeliveryFailed) {
          toast.error(t('auth.register_email_degraded'));
        } else {
          toast.success(t('auth.register_success'));
        }
        navigate("/verify", {
          state: {
            username: data.username,
            emailDeliveryFailed,
            supportEmail: res.data?.supportEmail,
            deliveryMessage: res.data?.message,
          }
        });
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
          <div className="mb-4 p-3 border-round-xl border-1 border-subtle bg-surface-soft" data-testid="onboarding-progress-register">
            <div className="u-eyebrow">{t('auth.onboarding_step_register_title')}</div>
            <div className="text-xs text-muted mt-2">{t('auth.onboarding_step_register_next')}</div>
          </div>
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

            <div className="mb-4 p-4 border-round-xl border-1 border-subtle bg-surface-soft" data-testid="register-city-section">
              <div className="mb-3">
                <div className="u-eyebrow">{t('auth.city_section_title')}</div>
                <div className="text-xs text-muted mt-2">{t('auth.city_section_desc')}</div>
              </div>

              <div className="flex gap-2 mb-3 flex-wrap">
                <CivicButton
                  type="button"
                  label={t('auth.city_mode_join')}
                  variant={cityMode === 'join' ? 'secondary' : 'ghost'}
                  size="small"
                  onClick={() => setCityMode('join')}
                  data-testid="register-city-mode-join"
                />
                <CivicButton
                  type="button"
                  label={t('auth.city_mode_create')}
                  variant={cityMode === 'create' ? 'secondary' : 'ghost'}
                  size="small"
                  onClick={() => setCityMode('create')}
                  data-testid="register-city-mode-create"
                />
              </div>

              {cityMode === 'join' ? (
                <CivicField label={t('auth.city_select_label')} error={errors.communityId?.message}>
                  <Controller name="communityId" control={control}
                    render={({ field }) => (
                      <CivicSelect
                        value={field.value}
                        options={cityOptions}
                        onChange={(event) => field.onChange(event.value)}
                        filter
                        filterBy="label"
                        showClear
                        placeholder={t('auth.city_select_placeholder')}
                        emptyMessage={t('auth.city_select_empty')}
                        className="w-full"
                        data-testid="register-city-select"
                      />
                    )}
                  />
                </CivicField>
              ) : (
                <div className="flex flex-column">
                  <CivicField label={t('auth.city_name_label')} error={errors.communityName?.message}>
                    <Controller name="communityName" control={control}
                      rules={{
                        validate: (value) =>
                          cityMode !== 'create' || (value && value.trim().length >= 2) || t('auth.city_name_required')
                      }}
                      render={({ field, fieldState }) => (
                        <InputText
                          {...field}
                          className={classNames('w-full p-inputtext-lg', { 'p-invalid': fieldState.error })}
                          placeholder={t('auth.city_name_placeholder')}
                          data-testid="register-city-name-input"
                        />
                      )}
                    />
                  </CivicField>

                  <CivicField label={t('auth.city_country_label')} error={errors.communityCountry?.message}>
                    <Controller name="communityCountry" control={control}
                      rules={{
                        validate: (value) =>
                          cityMode !== 'create' || (value && value.trim().length >= 2) || t('auth.city_country_required')
                      }}
                      render={({ field, fieldState }) => (
                        <InputText
                          {...field}
                          className={classNames('w-full p-inputtext-lg', { 'p-invalid': fieldState.error })}
                          placeholder={t('auth.city_country_placeholder')}
                          data-testid="register-city-country-input"
                        />
                      )}
                    />
                  </CivicField>

                  <CivicField label={t('auth.city_region_label')}>
                    <Controller name="communityRegion" control={control}
                      render={({ field }) => (
                        <InputText
                          {...field}
                          className="w-full"
                          placeholder={t('auth.city_region_placeholder')}
                          data-testid="register-city-region-input"
                        />
                      )}
                    />
                  </CivicField>

                  <p className="text-xs text-muted m-0" data-testid="register-city-organizer-note">
                    {t('auth.city_organizer_note')}
                  </p>
                </div>
              )}

              <p className="text-xs text-muted mt-3 mb-0">{t('auth.city_optional_note')}</p>
            </div>

            <div className="p-4 bg-surface-soft border-round-xl text-sm text-secondary flex align-items-center gap-3 border-1 border-subtle mb-4">
              <i className="pi pi-shield text-brand-primary text-lg"></i>
              <span className="font-bold leading-relaxed">{t('auth.clearance_level')}</span>
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
