import { useForm, Controller } from "react-hook-form";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { Password } from "primereact/password";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { classNames } from "primereact/utils";
import { Layout } from "../components/Layout";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";

interface ApiError extends Error {
  friendlyMessage?: string;
}

type RegisterForm = {
  username: string;
  email: string;
  password: string;
};

export function Register() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { control, handleSubmit, formState: { errors } } = useForm<RegisterForm>({
    defaultValues: { username: '', email: '', password: '' }
  });

  const onSubmit = async (data: RegisterForm) => {
    try {
      const res = await apiClient.post("auth/register", data);

      if (res.status === 200 || res.status === 201) {
        toast.success(t('auth.register_success'));
        navigate("/login");
      }
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('common.error'));
    }
  };

  return (
    <Layout authMode>
      <div className="flex justify-content-center align-items-center mt-6">
        <Card 
          title={<div className="text-center w-full">{t('auth.join_title')}</div>} 
          subTitle={<div className="text-center w-full">{t('auth.join_subtitle')}</div>} 
          style={{ width: '100%', maxWidth: '450px' }}
          data-testid="register-card"
        >
          <form onSubmit={handleSubmit(onSubmit)} className="p-fluid" aria-label="Registration Form">
            <div className="field mt-2">
              <span className="p-float-label p-input-icon-left">
                <i className="pi pi-user text-cyan-500" />
                <Controller name="username" control={control} rules={{ required: true, minLength: 4 }} 
                  render={({ field, fieldState }) => (
                    <InputText 
                      id="username-input" 
                      {...field} 
                      autoComplete="username"
                      className={classNames('py-3 pl-5', { 'p-invalid': fieldState.error })} 
                      data-testid="register-username-input"
                    />
                  )} 
                />
                <label htmlFor="username-input">{t('auth.username')}</label>
              </span>
              {errors.username && <small className="p-error block mt-1">{t('common.required')}</small>}
            </div>

            <div className="field mt-5">
              <span className="p-float-label p-input-icon-left">
                <i className="pi pi-envelope text-cyan-500" />
                <Controller name="email" control={control} rules={{ required: true, pattern: /^\S+@\S+$/i }} 
                  render={({ field, fieldState }) => (
                    <InputText 
                      id="email-input" 
                      {...field} 
                      autoComplete="email"
                      className={classNames('py-3 pl-5', { 'p-invalid': fieldState.error })} 
                      data-testid="register-email-input"
                    />
                  )} 
                />
                <label htmlFor="email-input">{t('auth.email')}</label>
              </span>
              {errors.email && <small className="p-error block mt-1">{t('common.invalid_email')}</small>}
            </div>

            <div className="field mt-5">
              <span className="p-float-label">
                <Controller name="password" control={control} rules={{ required: true, minLength: 8 }} 
                  render={({ field, fieldState }) => (
                    <Password 
                      id="password-input-container" 
                      {...field} 
                      toggleMask 
                      inputId="password-input" 
                      autoComplete="new-password"
                      inputClassName="py-3" 
                      className={classNames('w-full', { 'p-invalid': fieldState.error })} 
                      data-testid="register-password-input"
                    />
                  )} 
                />
                <label htmlFor="password-input">{t('auth.password')}</label>
              </span>
              {errors.password && <small className="p-error block mt-1">{t('common.required')}</small>}
            </div>

            <div className="mt-5 p-3 bg-blue-900-alpha-20 border-round text-xs text-blue-300 flex align-items-center gap-2 border-1 border-blue-800">
              <i className="pi pi-shield"></i>
              <span className="font-bold uppercase tracking-wider">{t('auth.clearance_level')}</span>
            </div>

            <Button 
              type="submit" 
              label={t('auth.create_account')} 
              className="p-button-primary mt-5 py-3 font-bold" 
              data-testid="register-submit-button"
              aria-label="Submit Registration"
            />
            
            <p className="text-center mt-5 text-gray-500 text-sm font-medium">
              {t('auth.already_member')} <Link to="/login" className="text-cyan-400 cursor-pointer font-bold hover:underline" data-testid="go-to-login">{t('auth.sign_in_here')}</Link>
            </p>
          </form>
        </Card>
      </div>
    </Layout>
  );
}
