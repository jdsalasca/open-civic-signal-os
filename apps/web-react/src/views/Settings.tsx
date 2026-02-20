import { useTranslation } from "react-i18next";
import { useSettingsStore } from "../store/useSettingsStore";
import { useAuthStore } from "../store/useAuthStore";
import { Card } from "primereact/card";
import { SelectButton, SelectButtonChangeEvent } from "primereact/selectbutton";
import { Dropdown, DropdownChangeEvent } from "primereact/dropdown";
import { Button } from "primereact/button";
import { Divider } from "primereact/divider";
import { Layout } from "../components/Layout";
import { toast } from "react-hot-toast";
import apiClient from "../api/axios";

interface ThemeOption {
  label: string;
  value: 'dark' | 'light';
  icon: string;
}

export function Settings() {
  const { t, i18n } = useTranslation();
  const { language, setLanguage, theme, setTheme } = useSettingsStore();
  const { activeRole, rawRoles, switchRole } = useAuthStore();

  const languageOptions = [
    { label: 'English', value: 'en' },
    { label: 'Español', value: 'es' }
  ];

  const themeOptions: ThemeOption[] = [
    { label: t('settings.dark'), value: 'dark', icon: 'pi pi-moon' },
    { label: t('settings.light'), value: 'light', icon: 'pi pi-sun' }
  ];

  const handleLanguageChange = (e: SelectButtonChangeEvent) => {
    const lang = e.value as 'en' | 'es';
    if (lang) {
      setLanguage(lang);
      i18n.changeLanguage(lang);
    }
  };

  const handleThemeChange = (e: SelectButtonChangeEvent) => {
    const nextTheme = e.value as 'dark' | 'light';
    if (nextTheme) setTheme(nextTheme);
  };

  const handleRoleChange = (e: DropdownChangeEvent) => {
    switchRole(e.value);
  };

  const handleExportCsv = async () => {
    try {
      const response = await apiClient.get("signals/export/csv", { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `signalos_export_${new Date().getTime()}.csv`);
            document.body.appendChild(link);
            link.click();
            link.remove();
            toast.success(t('settings.export_success'));
          } catch (err) {
            toast.error(t('settings.export_error'));
          }
        };
      
        return (
          <Layout>
            <div className="animate-fade-in max-w-30rem mx-auto mt-4">
              <div className="mb-5 text-center">
                <h1 className="text-4xl font-black text-main m-0 tracking-tight">{t('settings.title')}</h1>
                <p className="text-muted mt-2">{t('settings.desc')}</p>
              </div>
      
              <Card className="shadow-8 border-1 border-white-alpha-10 mb-4 bg-surface">
                <div className="flex flex-column gap-4">
      
                  <div className="flex flex-column gap-2">
                    <label className="font-bold text-sm uppercase tracking-widest text-muted">
                      <i className="pi pi-globe mr-2"></i>{t('settings.language')}
                    </label>
                    <SelectButton
                      value={language}
                      options={languageOptions}
                      onChange={handleLanguageChange}
                      className="w-full"
                    />
                  </div>
      
                  <Divider className="my-2 opacity-10" />
      
                  <div className="flex flex-column gap-2">
                    <label className="font-bold text-sm uppercase tracking-widest text-muted">
                      <i className="pi pi-palette mr-2"></i>{t('settings.theme')}
                    </label>
                    <SelectButton
                      value={theme}
                      options={themeOptions}
                      onChange={handleThemeChange}
                      itemTemplate={(option: ThemeOption) => (
                        <div className="flex align-items-center gap-2">
                          <i className={option.icon}></i>
                          <span>{option.label}</span>
                        </div>
                      )}
                    />
                  </div>
      
                  {rawRoles.length > 1 && (
                    <>
                      <Divider className="my-2 opacity-10" />
                      <div className="flex flex-column gap-2">
                        <label className="font-bold text-sm uppercase tracking-widest text-cyan-500">
                          <i className="pi pi-shield mr-2"></i>{t('settings.role')}
                        </label>
                        <p className="text-xs text-muted m-0 mb-2">{t('settings.role_desc')}</p>
                        <Dropdown
                          value={activeRole}
                          options={rawRoles}
                          onChange={handleRoleChange}
                          className="w-full bg-card"
                        />
                      </div>
                    </>
                  )}
      
                  {/* OCS-P2-011: Admin Audit Tools */}
                  {activeRole === 'SUPER_ADMIN' && (
                    <>
                      <Divider className="my-2 opacity-10" />
                      <div className="flex flex-column gap-2">
                        <label className="font-bold text-sm uppercase tracking-widest text-red-400">
                          <i className="pi pi-database mr-2"></i>{t('settings.admin_tools')}
                        </label>
                        <p className="text-xs text-muted m-0 mb-3">{t('settings.admin_desc')}</p> 
                        <Button
                          label={t('settings.export_button')}
                          icon="pi pi-download" 
                          className="p-button-danger outlined w-full"
                          onClick={handleExportCsv}
                        />
                      </div>
                    </>
                  )}
      
                </div>
              </Card>
      
              <div className="text-center p-4 bg-white-alpha-5 border-round-xl border-1 border-white-alpha-10">
                <span className="text-xs text-muted font-mono">{t('nav.protocol_version')} | Session: Persistent</span>
              </div>
            </div>
          </Layout>
        );
      }
