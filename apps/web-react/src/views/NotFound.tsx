import { useNavigate } from "react-router-dom";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { Layout } from "../components/Layout";
import { useTranslation } from "react-i18next";

export function NotFound() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  return (
    <Layout authMode>
      <div className="min-h-screen flex justify-content-center align-items-center p-4">
        <CivicCard className="w-full max-w-30rem text-center animate-fade-up" padding="lg">
          <img
            className="state-illustration state-illustration-wide"
            data-testid="not-found-illustration"
            src="/illustrations/city-404.svg"
            alt={t("exceptions.404_illustration_alt")}
            width={1200}
            height={1200}
          />
          <div className="mb-8">
            <h1 className="text-6xl font-black text-main m-0 tracking-tighter">{t('exceptions.404_title')}</h1>
            <h2 className="text-2xl font-bold text-main mt-2 tracking-tight">{t('exceptions.404_subtitle')}</h2>
            <p className="text-secondary mt-4 mb-0 font-medium leading-relaxed">
              {t('exceptions.404_desc')}
            </p>
          </div>

          <div className="flex flex-column gap-3">
            <CivicButton
              label={t('exceptions.404_home')}
              icon="pi pi-home"
              className="py-4 text-base"
              onClick={() => navigate("/")}
              glow
            />
            <CivicButton
              label={t('exceptions.404_verify')}
              variant="ghost"
              icon="pi pi-info-circle"
              className="text-xs"
              onClick={() => navigate("/backlog")}
            />
          </div>
        </CivicCard>
      </div>
    </Layout>
  );
}
