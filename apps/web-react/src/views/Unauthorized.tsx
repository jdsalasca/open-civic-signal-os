import { useNavigate } from "react-router-dom";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { Layout } from "../components/Layout";
import { useTranslation } from "react-i18next";

export function Unauthorized() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  return (
    <Layout authMode>
      <div className="min-h-screen flex justify-content-center align-items-center p-4">
        <CivicCard className="w-full max-w-30rem text-center animate-fade-up" padding="lg" data-testid="unauthorized-card">
          <div className="mb-8">
            <div className="inline-flex align-items-center justify-content-center p-4 bg-surface-soft border-round-3xl mb-6 border-1 border-subtle">
              <i className="pi pi-lock text-5xl text-status-progress"></i>
            </div>
            <h1 className="text-4xl font-black text-main m-0 tracking-tighter">{t('exceptions.403_subtitle')}</h1>
            <p className="text-secondary mt-4 mb-0 font-medium leading-relaxed">
              {t('exceptions.403_desc')}
            </p>
            <p className="text-sm text-muted mt-3 mb-0 line-height-3" data-testid="unauthorized-guidance">
              {t('exceptions.403_guidance')}
            </p>
          </div>

          <div className="flex flex-column gap-3">
            <CivicButton 
              label={t('exceptions.403_switch')} 
              icon="pi pi-shield" 
              className="py-4 text-base" 
              onClick={() => navigate("/settings")}
              glow
              data-testid="unauthorized-go-settings"
            />
            <CivicButton 
              label={t('exceptions.403_home')} 
              variant="secondary"
              icon="pi pi-arrow-left" 
              className="py-4 text-base"
              onClick={() => navigate("/")}
              data-testid="unauthorized-go-home"
            />
          </div>
        </CivicCard>
      </div>
    </Layout>
  );
}
