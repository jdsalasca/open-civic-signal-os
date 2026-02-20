import { useNavigate, useLocation } from "react-router-dom";
import { useForm, Controller } from "react-hook-form";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { Layout } from "../components/Layout";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";

type VerifyForm = {
  code: string;
};

export function Verify() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const username = location.state?.username;
  
  const { control, handleSubmit, formState: { isSubmitting } } = useForm<VerifyForm>();

  if (!username) {
    navigate("/login");
    return null;
  }

  const onSubmit = async (data: VerifyForm) => {
    try {
      await apiClient.post("auth/verify", { username, code: data.code });
      toast.success(t('auth.verified_success'));
      navigate("/login");
    } catch (err: unknown) {
      const apiErr = err as { friendlyMessage?: string };
      toast.error(apiErr.friendlyMessage || "Invalid verification code.");
    }
  };

  const handleResend = async () => {
    try {
      await apiClient.post("auth/resend-code", { username });
      toast.success(t('auth.code_resent') || "Verification code resent to your email.");
    } catch (err: unknown) {
      const apiErr = err as { friendlyMessage?: string };
      toast.error(apiErr.friendlyMessage || "Failed to resend code.");
    }
  };

  return (
    <Layout authMode>
      <div className="flex justify-content-center align-items-center mt-8">
        <Card 
          title={<div className="text-center w-full">{t('auth.activate_title')}</div>} 
          subTitle={<div className="text-center w-full">{t('auth.activate_subtitle')}</div>} 
          style={{ width: '100%', maxWidth: '400px' }}
          className="shadow-8"
        >
          <form onSubmit={handleSubmit(onSubmit)} className="p-fluid">
            <div className="field">
              <Controller name="code" control={control} rules={{ required: true, minLength: 6, maxLength: 6 }} 
                render={({ field }) => (
                  <InputText 
                    {...field} 
                    id="verify-code" 
                    placeholder="000000" 
                    className="text-center text-4xl font-black tracking-widest py-3"
                    autoFocus
                  />
                )} 
              />
            </div>
            <Button 
              type="submit" 
              label={t('auth.verify_button')} 
              className="p-button-primary mt-4 py-3 font-bold" 
              loading={isSubmitting} 
            />
            
            <div className="text-center mt-4">
              <Button 
                type="button" 
                label={t('auth.resend_button') || "Resend Verification Code"} 
                link 
                className="text-cyan-500 font-bold p-0" 
                onClick={handleResend}
              />
            </div>

            <p className="text-center mt-4 text-xs text-gray-500">
              {t('auth.verify_protocol')}
            </p>
          </form>
        </Card>
      </div>
    </Layout>
  );
}
