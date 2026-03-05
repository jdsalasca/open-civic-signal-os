import { useTranslation } from "react-i18next";
import { useEffect, useMemo, useState } from "react";
import { useSettingsStore } from "../store/useSettingsStore";
import { useAuthStore } from "../store/useAuthStore";
import { SelectButton, SelectButtonChangeEvent } from "primereact/selectbutton";
import { DropdownChangeEvent } from "primereact/dropdown";
import { Divider } from "primereact/divider";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
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
import { CivicMetaRow } from "../components/ui/CivicMetaRow";
import { CivicActionBar } from "../components/ui/CivicActionBar";
import { InterfaceMode, ProfileVisibility, UserProfile } from "../types";

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

interface Option {
  label: string;
  value: string;
}

interface InterfaceModeOption {
  label: string;
  value: 'simple' | 'advanced';
  icon: string;
}

interface ProfileFormState {
  displayName: string;
  civicRole: string;
  bio: string;
  affiliationsText: string;
  profileVisibility: ProfileVisibility;
  affiliationVisibility: ProfileVisibility;
  interfaceMode: 'simple' | 'advanced';
}

const EMPTY_PROFILE_FORM: ProfileFormState = {
  displayName: '',
  civicRole: 'NEIGHBOR',
  bio: '',
  affiliationsText: '',
  profileVisibility: 'PUBLIC',
  affiliationVisibility: 'COMMUNITY',
  interfaceMode: 'simple'
};

export function Settings() {
  const { t, i18n } = useTranslation();
  const { language, setLanguage, theme, setTheme, interfaceMode, setInterfaceMode } = useSettingsStore();
  const { activeRole, rawRoles, switchRole, userName } = useAuthStore();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [profileForm, setProfileForm] = useState<ProfileFormState>(EMPTY_PROFILE_FORM);
  const [profileLoading, setProfileLoading] = useState(true);
  const [profileSaving, setProfileSaving] = useState(false);

  const languageOptions = [
    { label: t('settings.languages.en'), value: 'en' },
    { label: t('settings.languages.es'), value: 'es' }
  ];

  const themeOptions: ThemeOption[] = [
    { label: t('settings.dark'), value: 'dark', icon: 'pi pi-moon' },
    { label: t('settings.light'), value: 'light', icon: 'pi pi-sun' }
  ];
  const interfaceModeOptions: InterfaceModeOption[] = [
    { label: t('settings.interface_modes.simple'), value: 'simple', icon: 'pi pi-sparkles' },
    { label: t('settings.interface_modes.advanced'), value: 'advanced', icon: 'pi pi-sliders-h' }
  ];

  const civicRoleOptions: Option[] = [
    { label: t('settings.civic_roles.STUDENT'), value: 'STUDENT' },
    { label: t('settings.civic_roles.TEACHER'), value: 'TEACHER' },
    { label: t('settings.civic_roles.NEIGHBOR'), value: 'NEIGHBOR' },
    { label: t('settings.civic_roles.ADMINISTRATOR'), value: 'ADMINISTRATOR' },
    { label: t('settings.civic_roles.AUTHORITY'), value: 'AUTHORITY' }
  ];

  const visibilityOptions: Option[] = [
    { label: t('settings.visibility.PUBLIC'), value: 'PUBLIC' },
    { label: t('settings.visibility.COMMUNITY'), value: 'COMMUNITY' },
    { label: t('settings.visibility.ADMINS'), value: 'ADMINS' }
  ];

  useEffect(() => {
    let mounted = true;

    const loadProfile = async () => {
      try {
        setProfileLoading(true);
        const response = await apiClient.get<UserProfile>('auth/profile/me');
        if (!mounted) return;
        setProfile(response.data);
        setProfileForm({
          displayName: response.data.displayName ?? '',
          civicRole: response.data.civicRole ?? 'NEIGHBOR',
          bio: response.data.bio ?? '',
          affiliationsText: response.data.affiliations.join(', '),
          profileVisibility: response.data.profileVisibility,
          affiliationVisibility: response.data.affiliationVisibility,
          interfaceMode: response.data.interfaceMode === 'ADVANCED' ? 'advanced' : 'simple'
        });
        setInterfaceMode(response.data.interfaceMode === 'ADVANCED' ? 'advanced' : 'simple');
      } catch (error) {
        if (!mounted) return;
        toast.error(t('settings.profile_load_error'));
      } finally {
        if (mounted) {
          setProfileLoading(false);
        }
      }
    };

    loadProfile();
    return () => {
      mounted = false;
    };
  }, [setInterfaceMode, t]);

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

  const handleInterfaceModeChange = (e: SelectButtonChangeEvent) => {
    const nextMode = e.value as 'simple' | 'advanced';
    if (nextMode) {
      setInterfaceMode(nextMode);
      handleProfileField('interfaceMode', nextMode);
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

  const handleProfileField = <K extends keyof ProfileFormState>(key: K, value: ProfileFormState[K]) => {
    setProfileForm((current) => ({ ...current, [key]: value }));
  };

  const handleSaveProfile = async () => {
    try {
      setProfileSaving(true);
      const payload = {
        displayName: profileForm.displayName.trim() || null,
        civicRole: profileForm.civicRole,
        bio: profileForm.bio.trim() || null,
        affiliations: profileForm.affiliationsText
          .split(',')
          .map((value) => value.trim())
          .filter(Boolean),
        profileVisibility: profileForm.profileVisibility,
        affiliationVisibility: profileForm.affiliationVisibility,
        interfaceMode: profileForm.interfaceMode.toUpperCase() as InterfaceMode
      };
      const response = await apiClient.put<UserProfile>('auth/profile/me', payload);
      setProfile(response.data);
      setProfileForm({
        displayName: response.data.displayName ?? '',
        civicRole: response.data.civicRole ?? 'NEIGHBOR',
        bio: response.data.bio ?? '',
        affiliationsText: response.data.affiliations.join(', '),
        profileVisibility: response.data.profileVisibility,
        affiliationVisibility: response.data.affiliationVisibility,
        interfaceMode: response.data.interfaceMode === 'ADVANCED' ? 'advanced' : 'simple'
      });
      setInterfaceMode(response.data.interfaceMode === 'ADVANCED' ? 'advanced' : 'simple');
      toast.success(t('settings.profile_saved'));
    } catch {
      toast.error(t('settings.profile_save_error'));
    } finally {
      setProfileSaving(false);
    }
  };

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
    } catch {
      toast.error(t('settings.export_error'));
    }
  };

  const identityName = profile?.displayName || userName || 'User';
  const identityRole = profile?.civicRole ? t(`settings.civic_roles.${profile.civicRole}`, { defaultValue: profile.civicRole }) : t('settings.identity_role_fallback');
  const profileVisibilityLabel = t(`settings.visibility.${profile?.profileVisibility ?? 'PUBLIC'}`);
  const affiliationVisibilityLabel = t(`settings.visibility.${profile?.affiliationVisibility ?? 'COMMUNITY'}`);

  return (
    <Layout>
      <div className="animate-fade-up motion-page max-w-64rem mx-auto">
        <CivicPageHeader title={t('settings.title')} description={t('settings.desc')} />

        <div className="grid">
          <div className="col-12 lg:col-5">
            <CivicCard title={t('settings.identity_profile')} variant="brand" className="h-full">
              <div className="flex flex-column align-items-center text-center py-4">
                <div className="relative mb-4">
                  <Avatar label={identityName[0]?.toUpperCase() ?? 'U'} shape="circle" size="xlarge" className="bg-brand-primary text-on-brand font-black shadow-xl" style={{ width: '80px', height: '80px', fontSize: '2rem' }} />
                  <div className="absolute bottom-0 right-0 bg-status-resolved border-circle border-2 border-subtle" style={{ width: '20px', height: '20px' }}></div>
                </div>
                <h2 className="text-2xl font-black text-main m-0 tracking-tight">{identityName}</h2>
                <div className="mt-2 flex gap-2 justify-content-center flex-wrap">
                  <CivicBadge label={activeRole} severity="progress" />
                  <CivicBadge label={t('settings.verified_user')} severity="resolved" />
                  <CivicBadge label={identityRole} severity="neutral" />
                </div>

                <Divider className="my-6 opacity-10" />

                <div className="w-full text-left">
                  <CivicMetaRow label={t('settings.profile_visibility_label')} value={profileVisibilityLabel} />
                  <CivicMetaRow label={t('settings.affiliation_visibility_label')} value={affiliationVisibilityLabel} />
                  <CivicMetaRow label={t('settings.affiliations_preview')} value={profile?.affiliations.length ? profile.affiliations.join(', ') : t('settings.no_affiliations')} />
                </div>
              </div>
            </CivicCard>
          </div>

          <div className="col-12 lg:col-7">
            <CivicCard title={t('settings.public_identity')} className="mb-6">
              <div className="flex flex-column gap-5" data-testid="profile-settings-card">
                <p className="text-secondary text-sm m-0 leading-relaxed">{t('settings.public_identity_help')}</p>

                <CivicField label={t('settings.display_name')} helpText={t('settings.display_name_help')}>
                  <InputText
                    value={profileForm.displayName}
                    onChange={(e) => handleProfileField('displayName', e.target.value)}
                    className="w-full"
                    disabled={profileLoading}
                    data-testid="profile-display-name-input"
                  />
                </CivicField>

                <CivicField label={t('settings.civic_role_label')} helpText={t('settings.civic_role_help')}>
                  <CivicSelect
                    value={profileForm.civicRole}
                    options={civicRoleOptions}
                    optionLabel="label"
                    optionValue="value"
                    onChange={(e) => handleProfileField('civicRole', e.value as string)}
                    className="w-full"
                    disabled={profileLoading}
                    data-testid="profile-civic-role-select"
                  />
                </CivicField>

                <CivicField label={t('settings.affiliations_label')} helpText={t('settings.affiliations_help')}>
                  <InputText
                    value={profileForm.affiliationsText}
                    onChange={(e) => handleProfileField('affiliationsText', e.target.value)}
                    className="w-full"
                    disabled={profileLoading}
                    data-testid="profile-affiliations-input"
                  />
                </CivicField>

                <CivicField label={t('settings.bio_label')} helpText={t('settings.bio_help')}>
                  <InputTextarea
                    value={profileForm.bio}
                    onChange={(e) => handleProfileField('bio', e.target.value)}
                    className="w-full"
                    rows={4}
                    autoResize
                    disabled={profileLoading}
                    data-testid="profile-bio-input"
                  />
                </CivicField>

                <div className="grid">
                  <div className="col-12 md:col-6">
                    <CivicField label={t('settings.profile_visibility_label')} helpText={t('settings.profile_visibility_help')}>
                      <CivicSelect
                        value={profileForm.profileVisibility}
                        options={visibilityOptions}
                        optionLabel="label"
                        optionValue="value"
                        onChange={(e) => handleProfileField('profileVisibility', e.value as ProfileVisibility)}
                        className="w-full"
                        disabled={profileLoading}
                        data-testid="profile-visibility-select"
                      />
                    </CivicField>
                  </div>
                  <div className="col-12 md:col-6">
                    <CivicField label={t('settings.affiliation_visibility_label')} helpText={t('settings.affiliation_visibility_help')}>
                      <CivicSelect
                        value={profileForm.affiliationVisibility}
                        options={visibilityOptions}
                        optionLabel="label"
                        optionValue="value"
                        onChange={(e) => handleProfileField('affiliationVisibility', e.value as ProfileVisibility)}
                        className="w-full"
                        disabled={profileLoading}
                        data-testid="affiliation-visibility-select"
                      />
                    </CivicField>
                  </div>
                </div>

                <CivicActionBar>
                  <span className="text-xs text-muted font-medium">{profileLoading ? t('common.loading') : t('settings.identity_ready')}</span>
                  <CivicButton
                    label={t('settings.save_profile')}
                    icon="pi pi-save"
                    onClick={handleSaveProfile}
                    loading={profileSaving}
                    disabled={profileLoading}
                    data-testid="save-profile-button"
                  />
                </CivicActionBar>
              </div>
            </CivicCard>

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

                <CivicField label={t('settings.interface_mode')} helpText={t('settings.interface_mode_help')}>
                  <SelectButton
                    value={interfaceMode}
                    options={interfaceModeOptions}
                    onChange={handleInterfaceModeChange}
                    className="w-full"
                    data-testid="interface-mode-select"
                    itemTemplate={(option: InterfaceModeOption) => (
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
                      className="w-full p-inputtext-lg"
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

        <div className="text-center mt-8 mb-8">
          <CivicActionBar className="justify-content-center">
            <p className="text-muted text-xs font-bold uppercase tracking-widest m-0">
              {t('settings.footer')}
            </p>
          </CivicActionBar>
        </div>
      </div>
    </Layout>
  );
}
