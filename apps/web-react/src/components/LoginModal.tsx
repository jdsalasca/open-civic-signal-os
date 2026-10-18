import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { InputText } from "primereact/inputtext";
import { Password } from "primereact/password";
import { CivicButton } from "./ui/CivicButton";
import { CivicCard } from "./ui/CivicCard";
import { CivicField } from "./ui/CivicField";

type Props = {
  onLogin: (user: string, pass: string) => void;
  onClose: () => void;
};

/**
 * The inline sign-in sheet used when a flow needs a session without leaving the page. It mirrors
 * the full Login screen: explicit labels, a real password field and a clear way out, rather than a
 * bare pair of placeholder inputs.
 */
export function LoginModal({ onLogin, onClose }: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [credentials, setCredentials] = useState({ user: "", pass: "" });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onLogin(credentials.user, credentials.pass);
  };

  const handleRegisterRedirect = () => {
    onClose();
    navigate("/register");
  };

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true" aria-label={t('auth.login_title')}>
      <CivicCard className="auth-panel login-modal" padding="lg">
        <form className="flex flex-column" onSubmit={handleSubmit}>
          <h2 className="text-3xl font-black text-main m-0 mb-6 text-center tracking-tighter">
            {t('auth.login_title')}
          </h2>

          <CivicField label={t('auth.username')}>
            <InputText
              value={credentials.user}
              onChange={(e) => setCredentials({ ...credentials, user: e.target.value })}
              autoComplete="username"
              placeholder={t('auth.username_placeholder')}
              className="w-full p-inputtext-lg"
              required
            />
          </CivicField>

          <CivicField label={t('auth.password')}>
            <Password
              value={credentials.pass}
              onChange={(e) => setCredentials({ ...credentials, pass: e.target.value })}
              toggleMask
              feedback={false}
              autoComplete="current-password"
              placeholder={t('auth.password_placeholder')}
              inputClassName="w-full p-inputtext-lg"
              className="w-full"
              required
            />
          </CivicField>

          <div className="flex gap-2 mt-2">
            <CivicButton type="submit" label={t('auth.sign_in_button')} className="flex-1" glow />
            <CivicButton type="button" label={t('common.cancel')} variant="ghost" onClick={onClose} />
          </div>

          <p className="auth-hint justify-content-center mt-4">
            <span>{t('auth.no_account')}</span>
            <button
              type="button"
              className="text-brand-primary font-bold bg-transparent border-none cursor-pointer p-0"
              onClick={handleRegisterRedirect}
            >
              {t('auth.create_one')}
            </button>
          </p>
        </form>
      </CivicCard>
    </div>
  );
}
