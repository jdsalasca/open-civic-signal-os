import { useTranslation } from "react-i18next";
import { useSettingsStore } from "../store/useSettingsStore";
import { useAuthStore } from "../store/useAuthStore";
import { SelectButton, SelectButtonChangeEvent } from "primereact/selectbutton";
import { Dropdown, DropdownChangeEvent } from "primereact/dropdown";
import { Divider } from "primereact/divider";
import { Layout } from "../components/Layout";
import { toast } from "react-hot-toast";
import { Avatar } from "primereact/avatar";
import apiClient from "../api/axios";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicField } from "../components/ui/CivicField";
import { CivicBadge } from "../components/ui/CivicBadge";

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
    { label: 'English', value: 'en' },
    { label: 'Español', value: 'es' }
  ];

  const themeOptions: ThemeOption[] = [
    { label: 'Dark Mode', value: 'dark', icon: 'pi pi-moon' },
    { label: 'Light Mode', value: 'light', icon: 'pi pi-sun' }
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
    switchRole(nextRole);
    toast.success(`Active authorization level updated to ${nextRole}`);
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
      link.setAttribute('download', `civic_intelligence_export_${new Date().getTime()}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      toast.success("Intelligence dataset exported successfully");
    } catch (err) {
      toast.error("Data export failed: Insufficient clearance");
    }
  };

  return (
    <Layout>
      <div className="animate-fade-up max-w-50rem mx-auto">
        <div className="mb-8">
          <h1 className="text-5xl font-black mb-2 text-main tracking-tighter">System Configuration</h1>
          <p className="text-secondary text-lg font-medium">Manage your digital identity and operational preferences.</p>
        </div>

        <div className="grid">
          <div className="col-12 lg:col-5">
            <CivicCard title="Identity Profile" variant="brand" className="h-full">
              <div className="flex flex-column align-items-center text-center py-4">
                <div className="relative mb-4">
                  <Avatar label={userName?.[0].toUpperCase()} shape="circle" size="xlarge" className="bg-brand-primary text-white font-black shadow-xl" style={{ width: '80px', height: '80px', fontSize: '2rem' }} />
                  <div className="absolute bottom-0 right-0 bg-status-resolved border-circle border-2 border-surface-0" style={{ width: '20px', height: '20px' }}></div>
                </div>
                <h2 className="text-2xl font-black text-main m-0 tracking-tight">{userName}</h2>
                <div className="mt-2 flex gap-2 justify-content-center">
                  <CivicBadge label={activeRole} severity="progress" />
                  <CivicBadge label="Verified User" severity="resolved" />
                </div>
                
                <Divider className="my-6 opacity-10" />
                
                <div className="w-full text-left">
                  <div className="flex justify-content-between mb-3">
                    <span className="text-xs font-bold text-muted uppercase">Clearance Level</span>
                    <span className="text-xs font-mono text-main">L-04 ADMIN</span>
                  </div>
                  <div className="flex justify-content-between mb-3">
                    <span className="text-xs font-bold text-muted uppercase">Encryption</span>
                    <span className="text-xs font-mono text-main">AES-256 Enabled</span>
                  </div>
                  <div className="flex justify-content-between">
                    <span className="text-xs font-bold text-muted uppercase">Protocol</span>
                    <span className="text-xs font-mono text-main">SignalOS v2.4</span>
                  </div>
                </div>
              </div>
            </CivicCard>
          </div>

          <div className="col-12 lg:col-7">
            <CivicCard title="Interface & Protocol" className="mb-6">
              <div className="flex flex-column gap-6">
                <CivicField label="Global Language" helpText="Localization for all system notifications and UI elements.">
                  <SelectButton
                    value={language}
                    options={languageOptions}
                    onChange={handleLanguageChange}
                    className="w-full"
                  />
                </CivicField>

                <CivicField label="Visual Environment" helpText="Adjust the interface contrast for your current workspace.">
                  <SelectButton
                    value={theme}
                    options={themeOptions}
                    onChange={handleThemeChange}
                    className="w-full"
                    itemTemplate={(option: ThemeOption) => (
                      <div className="flex align-items-center justify-content-center gap-3 w-full py-1">
                        <i className={option.icon}></i>
                        <span className="font-bold">{option.label}</span>
                      </div>
                    )}
                  />
                </CivicField>

                {rawRoles.length > 1 && (
                  <CivicField label="Active Authority" helpText="Switch between your authorized roles for this session.">
                    <Dropdown
                      value={activeRole}
                      options={roleOptions}
                      optionLabel="label"
                      optionValue="value"
                      onChange={handleRoleChange}
                      className="w-full bg-black-alpha-20 p-inputtext-lg"
                      itemTemplate={(option: RoleOption) => (
                        <div className="flex flex-column py-1">
                          <span className="font-bold text-main">{option.label}</span>
                          <small className="text-muted font-mono uppercase text-min">{option.code}</small>
                        </div>
                      )}
                    />
                  </CivicField>
                )}
              </div>
            </CivicCard>

            {activeRole === 'SUPER_ADMIN' && (
              <CivicCard title="Administrative Arsenal" variant="danger">
                <div className="flex flex-column gap-4">
                  <p className="text-secondary text-sm m-0 leading-relaxed">
                    Access high-level data exports and system-wide integrity tools.
                  </p>
                  <CivicButton
                    label="Download Intelligence Dataset"
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
            Open Civic Signal OS — Professional Governance Protocol
          </p>
        </div>
      </div>
    </Layout>
  );
}
