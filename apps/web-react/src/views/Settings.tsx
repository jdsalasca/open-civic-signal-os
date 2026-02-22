import { useTranslation } from "react-i18next";
import { useMemo } from "react";
import { useSettingsStore } from "../store/useSettingsStore";
import { useAuthStore } from "../store/useAuthStore";
import { SelectButton, SelectButtonChangeEvent } from "primereact/selectbutton";
import { DropdownChangeEvent } from "primereact/dropdown";
import { Divider } from "primereact/divider";
import { Layout } from "../components/Layout";
import { toast } from "react-hot-toast";
import { Avatar } from "primereact/avatar";
import apiClient from "../api/axios";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicField } from "../components/ui/CivicField";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicSelect } from "../components/ui/CivicSelect";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";

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
  const { activeRole, rawRoles, switchRole, userName } = useAuthStore();

  const languageOptions = [
    { label: t('settings.languages.en'), value: 'en' },
    { label: t('settings.languages.es'), value: 'es' }
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
    if (nextTheme) {
      setTheme(nextTheme);
      document.documentElement.classList.remove('dark-theme', 'light-theme');
      document.documentElement.classList.add(`${nextTheme}-theme`);
    }
  };

  const handleRoleChange = (e: DropdownChangeEvent) => {
    const nextRole = e.value as string | undefined;
    if (!nextRole || nextRole === activeRole) return;
    const roleLabel = roleOptions.find((option) => option.value === nextRole)?.label ?? nextRole;
    switchRole(nextRole);
    toast.success(t('settings.role_switched', { role: roleLabel }));
  };

  const roleOptions: RoleOption[] = useMemo(
    () =>
      rawRoles
        .map((role) => ({
          label: t(`settings.roles.${role}`, { defaultValue: role.replace(/_/g, ' ') }),
          value: role,
          code: role,
        }))
        .sort((a, b) => a.label.localeCompare(b.label)),
    [rawRoles, t]
  );

  const handleExportCsv = async () => {
    try {
      const response = await apiClient.get("signals/export/csv", { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `civic_intelligence_export_${new Date().getTime()}.csv`);
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
      <div className="animate-fade-up max-w-50rem mx-auto">
        <CivicPageHeader title={t('settings.title')} description={t('settings.desc')} />

        <div className="grid">
          <div className="col-12 lg:col-5">
            <CivicCard title={t('settings.identity_profile')} variant="brand" className="h-full">
              <div className="flex flex-column align-items-center text-center py-4">
                <div className="relative mb-4">
                  <Avatar label={userName?.[0].toUpperCase()} shape="circle" size="xlarge" className="bg-brand-primary text-white font-black shadow-xl" style={{ width: '80px', height: '80px', fontSize: '2rem' }} />
                  <div className="absolute bottom-0 right-0 bg-status-resolved border-circle border-2 border-surface-0" style={{ width: '20px', height: '20px' }}></div>
                </div>
                <h2 className="text-2xl font-black text-main m-0 tracking-tight">{userName}</h2>
                <div className="mt-2 flex gap-2 justify-content-center">
                  <CivicBadge label={activeRole} severity="progress" />
                  <CivicBadge label={t('settings.verified_user')} severity="resolved" />
                </div>
                
                <Divider className="my-6 opacity-10" />
                
                <div className="w-full text-left">
                  <div className="flex justify-content-between mb-3">
                    <span className="text-xs font-bold text-muted uppercase">{t('settings.clearance_level')}</span>
                    <span className="text-xs font-mono text-main">{t('settings.clearance_level_value')}</span>
                  </div>
                  <div className="flex justify-content-between mb-3">
                    <span className="text-xs font-bold text-muted uppercase">{t('settings.encryption')}</span>
                    <span className="text-xs font-mono text-main">{t('settings.encryption_value')}</span>
                  </div>
                  <div className="flex justify-content-between">
                    <span className="text-xs font-bold text-muted uppercase">{t('settings.protocol')}</span>
                    <span className="text-xs font-mono text-main">{t('settings.protocol_value')}</span>
                  </div>
                </div>
              </div>
            </CivicCard>
          </div>

          <div className="col-12 lg:col-7">
            <CivicCard title={t('settings.interface_protocol')} className="mb-6">
              <div className="flex flex-column gap-6">
                <CivicField label={t('settings.language')} helpText={t('settings.language_help')}>
                  <SelectButton
                    value={language}
                    options={languageOptions}
                    onChange={handleLanguageChange}
                    className="w-full"
                    data-testid="language-select"
                  />
                </CivicField>

                <CivicField label={t('settings.theme')} helpText={t('settings.theme_help')}>
                  <SelectButton
                    value={theme}
                    options={themeOptions}
                    onChange={handleThemeChange}
                    className="w-full"
                    data-testid="theme-select"
                    itemTemplate={(option: ThemeOption) => (
                      <div className="flex align-items-center justify-content-center gap-3 w-full py-1">
                        <i className={option.icon}></i>
                        <span className="font-bold">{option.label}</span>
                      </div>
                    )}
                  />
                </CivicField>

                <CivicField label={t('settings.role')} helpText={t('settings.role_desc')}>
                  <div data-testid="role-switch-dropdown">
                    <CivicSelect
                      value={activeRole}
                      options={roleOptions}
                      optionLabel="label"
                      optionValue="value"
                      onChange={handleRoleChange}
                      className="w-full bg-black-alpha-20 p-inputtext-lg"
                      disabled={rawRoles.length <= 1}
                      placeholder={roleOptions[0]?.label ?? activeRole}
                      itemTemplate={(option: RoleOption) => (
                        <div className="flex flex-column py-1">
                          <span className="font-bold text-main">{option.label}</span>
                          <small className="text-muted font-mono uppercase text-min">{option.code}</small>
                        </div>
                      )}
                    />
                  </div>
                </CivicField>
              </div>
            </CivicCard>

            {activeRole === 'SUPER_ADMIN' && (
              <CivicCard title={t('settings.admin_tools')} variant="danger">
                <div className="flex flex-column gap-4">
                  <p className="text-secondary text-sm m-0 leading-relaxed">
                    {t('settings.admin_desc')}
                  </p>
                  <CivicButton
                    label={t('settings.export_button')}
                    icon="pi pi-download" 
                    variant="danger"
                    className="w-full py-4 text-sm"
                    onClick={handleExportCsv}
                  />
                </div>
              </CivicCard>
            )}
          </div>
        </div>

        <div className="text-center mt-8 p-6 bg-white-alpha-5 border-round-3xl border-1 border-white-alpha-10 mb-8">
          <p className="text-muted text-xs font-bold uppercase tracking-widest m-0">
            {t('settings.footer')}
          </p>
        </div>
      </div>
    </Layout>
  );
}
