import { useNavigate } from "react-router-dom";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { useAuthStore } from "../store/useAuthStore";
import { Layout } from "../components/Layout";
import { useTranslation } from "react-i18next";

export function Unauthorized() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const logout = useAuthStore((state) => state.logout);

  const handleSwitchAccount = () => {
    logout();
    navigate("/login");
  };

  return (
    <Layout authMode>
      <div className="flex align-items-center justify-content-center mt-8">
        <Card className="exception-card shadow-8 border-1 border-white-alpha-10 animate-fade-in border-red-900" style={{ maxWidth: '450px' }}>
          <div className="text-center">
            <div className="inline-flex align-items-center justify-content-center bg-red-900-alpha-20 border-circle mb-4 shadow-4" style={{ width: '100px', height: '100px' }}>
              <i className="pi pi-lock text-5xl text-red-500"></i>
            </div>
            <h1 className="text-6xl font-black text-white m-0 tracking-tighter">{t('exceptions.403_title')}</h1>
            <h2 className="text-2xl font-bold text-gray-200 mt-2 mb-4">{t('exceptions.403_subtitle')}</h2>
            <p className="text-gray-400 mb-6 line-height-3">
              {t('exceptions.403_desc')}
            </p>
            
            <div className="flex flex-column gap-3">
              <Button 
                label={t('exceptions.403_home')} 
                icon="pi pi-arrow-left" 
                className="p-button-primary py-3 font-bold text-lg" 
                onClick={() => navigate("/")} 
                data-testid="unauthorized-back-button"
              />
              <Button 
                label={t('exceptions.403_switch')} 
                icon="pi pi-user-edit" 
                text 
                className="text-gray-100 hover:bg-white-alpha-10 font-bold py-3 border-1 border-white-alpha-20" 
                onClick={handleSwitchAccount} 
                data-testid="unauthorized-switch-button"
              />
            </div>
          </div>
        </Card>
      </div>
    </Layout>
  );
}
