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

interface RoleOption {
  label: string;
  value: string;
  code: string;
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
    const nextRole = e.value as string | undefined;
    if (!nextRole || nextRole === activeRole) {
      return;
    }
    switchRole(nextRole);
    toast.success(
      t('settings.role_switched', {
        role: t(`settings.roles.${nextRole}`, { defaultValue: nextRole.replace(/_/g, ' ') }),
      })
    );
  };

  const roleOptions: RoleOption[] = rawRoles.map((role) => ({
    label: t(`settings.roles.${role}`, { defaultValue: role.replace(/_/g, ' ') }),
    value: role,
    code: role,
  }));

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
                      <i className="pi pi-globe mr-2 text-cyan-500"></i>{t('settings.language')}
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
                      <i className="pi pi-palette mr-2 text-purple-500"></i>{t('settings.theme')}
                    </label>
                    <SelectButton
                      value={theme}
                      options={themeOptions}
                      onChange={handleThemeChange}
                      className="w-full"
                      itemTemplate={(option: ThemeOption) => (
                        <div className="flex align-items-center justify-content-center gap-2 w-full">
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
                        <div data-testid="role-switch-dropdown">
                          <Dropdown
                            value={activeRole}
                            options={roleOptions}
                            optionLabel="label"
                            optionValue="value"
                            onChange={handleRoleChange}
                            className="w-full bg-card p-inputtext-lg"
                            placeholder={t('settings.role')}
                            itemTemplate={(option: RoleOption) => (
                              <div className="flex flex-column py-1">
                                <span className="font-bold text-main">{option.label}</span>
                                <small className="text-muted font-mono">{option.code}</small>
                              </div>
                            )}
                          />
                        </div>
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
