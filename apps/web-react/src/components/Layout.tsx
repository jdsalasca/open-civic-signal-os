import { ReactNode, useState, useEffect } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { Button } from "primereact/button";
import { Avatar } from "primereact/avatar";
import { Sidebar } from "primereact/sidebar";
import { InputText } from "primereact/inputtext";
import { useAuthStore } from "../store/useAuthStore";
import { useCommunityStore } from "../store/useCommunityStore";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { CivicSelect } from "./ui/CivicSelect";

type Props = {
  children: ReactNode;
  authMode?: boolean;
};

type NavItem = {
  label: string;
  to: string;
  icon: string;
  visible: boolean;
  testId?: string;
};

export function Layout({ children, authMode = false }: Props) {
  const MEMBERSHIP_CACHE_TTL_MS = 5 * 60 * 1000;
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { isLoggedIn, activeRole, userName, logout } = useAuthStore();
  const {
    memberships,
    activeCommunityId,
    setMemberships,
    setActiveCommunityId,
    shouldRefreshMemberships,
  } = useCommunityStore();
  const [mobileMenuVisible, setMobileMenuVisible] = useState(false);

  useEffect(() => {
    const loadMemberships = async () => {
      if (!isLoggedIn) return;
      if (!shouldRefreshMemberships(MEMBERSHIP_CACHE_TTL_MS)) return;
      try {
        const res = await apiClient.get("communities/my");
        if (res.status === 200) {
          setMemberships(res.data || []);
        }
      } catch (err) {
        console.warn("Failed to load community memberships", err);
      }
    };
    loadMemberships();
  }, [isLoggedIn, setMemberships, shouldRefreshMemberships]);

  const handleLogout = async () => {
    try {
      await apiClient.post("auth/logout");
    } catch (err) {
      console.warn(t('auth.logout_warn'));
    } finally {
      logout();
      navigate("/login");
    }
  };

  const handleCommunitySwitch = async (communityId: string) => {
    try {
      await apiClient.post(`communities/${communityId}/switch`);
      setActiveCommunityId(communityId);
    } catch (err) {
      console.warn("Community switch failed", err);
    }
  };

  const isStaff = activeRole === "PUBLIC_SERVANT" || activeRole === "SUPER_ADMIN";

  const mainNav: NavItem[] = [
    { label: t('nav.insights'), to: '/', icon: 'pi pi-th-large', visible: isLoggedIn },
    { label: t('nav.live_feed'), to: '/communities/feed', icon: 'pi pi-bolt', visible: isLoggedIn },
    { label: t('nav.report'), to: '/report', icon: 'pi pi-plus-circle', visible: isLoggedIn, testId: 'report-issue-button' },
  ];

  const socialNav: NavItem[] = [
    { label: t('nav.public_blog'), to: '/communities/blog', icon: 'pi pi-megaphone', visible: isLoggedIn },
    { label: t('nav.dialogues'), to: '/communities/threads', icon: 'pi pi-comments', visible: isLoggedIn },
  ];

  const personalNav: NavItem[] = [
    { label: t('nav.my_contributions_short'), to: '/mine', icon: 'pi pi-user', visible: isLoggedIn },
    { label: t('nav.moderation'), to: '/moderation', icon: 'pi pi-shield', visible: isLoggedIn && isStaff },
    { label: t('nav.communities'), to: '/communities', icon: 'pi pi-globe', visible: isLoggedIn },
    { label: t('nav.settings'), to: '/settings', icon: 'pi pi-cog', visible: isLoggedIn },
  ];

  const communityOptions = memberships.map((m) => ({
    label: m.communityName,
    value: m.communityId,
    role: m.role
  }));

  const quickActions = [
    { label: t('nav.report'), to: '/report', icon: 'pi pi-plus-circle', visible: isLoggedIn },
    { label: t('nav.my_contributions_short'), to: '/mine', icon: 'pi pi-user', visible: isLoggedIn },
  ];

  const mobileNav = [mainNav[0], mainNav[2], personalNav[0], socialNav[0], personalNav[3]].filter((item) => item?.visible);

  if (authMode) return <div className="auth-page min-h-screen">{children}</div>;

  const NavGroup = ({ title, items }: { title: string, items: any[] }) => (
    <div className="mb-6">
      <div className="text-muted text-xs font-black uppercase tracking-widest mb-3 ml-4 opacity-40">{title}</div>
      <div className="flex flex-column gap-1">
        {items.filter(l => l.visible).map(link => (
          <Link
            key={link.to}
            to={link.to}
            className={`flex align-items-center justify-content-between px-4 py-3 border-round-xl no-underline transition-all font-bold ${location.pathname === link.to ? 'bg-elevated text-main shadow-sm' : 'text-secondary hover:text-main hover:bg-surface'}`}
            data-testid={link.testId}
            aria-label={link.label}
            aria-current={location.pathname === link.to ? "page" : undefined}
            onClick={() => setMobileMenuVisible(false)}
          >
            <div className="flex align-items-center gap-3">
              <i className={`${link.icon} text-base ${location.pathname === link.to ? 'text-brand-primary' : 'opacity-70'}`}></i>
              <span className="text-sm tracking-tight">{link.label}</span>
            </div>
            {location.pathname === link.to && <div className="w-4px h-4px border-circle bg-brand-primary shadow-lg"></div>}
          </Link>
        ))}
      </div>
    </div>
  );

  return (
    <div className="flex h-screen overflow-hidden bg-app">
      <a href="#main-content" className="skip-link">{t('nav.skip_to_content')}</a>
      {/* SIDEBAR */}
      <aside className="hidden lg:flex flex-column w-18rem border-right-1 border-white-alpha-5 bg-card z-2">
        <div className="p-6 flex align-items-center gap-3">
          <div className="bg-white border-round-xl flex align-items-center justify-content-center shadow-premium" style={{ width: '36px', height: '36px' }}>
            <i className="pi pi-signal text-black text-lg"></i>
          </div>
          <span className="text-xl font-black tracking-tighter uppercase text-main">Signal<span className="text-brand-primary">OS</span></span>
        </div>

        <nav className="flex-grow-1 px-3 py-4 overflow-y-auto" aria-label={t('nav.main_navigation')}>
          <NavGroup title={t('nav.group_intelligence')} items={mainNav} />
          <NavGroup title={t('nav.group_collaboration')} items={socialNav} />
          <NavGroup title={t('nav.group_personal')} items={personalNav} />
        </nav>

        <div className="mt-auto p-4 border-top-1 border-subtle bg-surface">
          <div className="flex flex-column gap-4">
            <div className="flex align-items-center gap-3 px-2">
              <Avatar label={userName?.[0].toUpperCase()} shape="circle" className="bg-brand-primary text-white font-bold" />
              <div className="flex flex-column overflow-hidden">
                <span className="text-xs font-black text-main truncate uppercase tracking-wider">{userName}</span>
                <span className="text-min font-bold text-muted uppercase" style={{ fontSize: '8px' }}>{t('nav.clearance')}: {activeRole}</span>
              </div>
            </div>
            <Button
              label={t('nav.sign_out')}
              icon="pi pi-power-off"
              text
              className="w-full justify-content-start text-xs font-black text-muted hover:text-danger py-3"
              onClick={handleLogout}
              data-testid="logout-button-desktop"
            />
          </div>
        </div>
      </aside>

      {/* VIEW AREA */}
      <div className="flex flex-column flex-grow-1 overflow-hidden relative">
        <header className="h-5rem flex align-items-center justify-content-between px-6 border-bottom-1 border-subtle bg-nav backdrop-blur-xl z-1">
          <div className="flex align-items-center gap-6 flex-grow-1">
            <Button
              icon="pi pi-bars"
              text
              className="lg:hidden text-main"
              onClick={() => setMobileMenuVisible(true)}
              aria-label={t('nav.open_navigation')}
              data-testid="mobile-menu-toggle"
            />

            <div className="hidden md:flex align-items-center flex-grow-1 max-w-30rem">
              <span className="p-input-icon-left w-full relative group">
                <i className="pi pi-search text-muted group-focus-within:text-brand-primary transition-colors" />
                <InputText
                  placeholder="Search intelligence, commands, or signals... (Ctrl + K)"
                  className="w-full bg-surface border-subtle border-round-xl py-2 pl-5 text-xs font-bold uppercase tracking-wider hover:bg-elevated transition-all"
                />
                <div className="absolute right-0 top-0 bottom-0 flex align-items-center pr-3 pointer-events-none">
                  <kbd className="bg-white-alpha-10 px-2 py-1 border-round text-min font-mono opacity-40">CTRL K</kbd>
                </div>
              </span>
            </div>
          </div>

          <div className="flex align-items-center gap-4">
            {communityOptions.length > 0 && (
              <div className="hidden sm:flex align-items-center gap-3 bg-surface border-round-xl px-4 py-2 border-1 border-subtle hover:border-brand-primary transition-colors cursor-pointer">
                <i className="pi pi-map-marker text-brand-primary text-sm"></i>
                <CivicSelect
                  value={activeCommunityId || communityOptions[0].value}
                  options={communityOptions}
                  onChange={(e) => handleCommunitySwitch(e.value)}
                  placeholder="Sector"
                  className="w-10rem border-none bg-transparent font-bold text-sm"
                  data-testid="community-switch-dropdown"
                  itemTemplate={(option) => (
                    <div className="flex flex-column py-1">
                      <span className="font-black text-xs uppercase tracking-widest">{option.label}</span>
                      <small className="text-muted text-min font-mono mt-1">{option.role}</small>
                    </div>
                  )}
                />
              </div>
            )}

            <div className="hidden xl:flex align-items-center gap-3 u-pill">
              <div className="w-8px h-8px border-circle bg-status-resolved animate-pulse"></div>
              <span className="text-xs font-black text-main uppercase tracking-widest">{t('nav.core_active')}</span>
            </div>

            <div className="hidden lg:flex align-items-center gap-2">
              {quickActions.map((action) => (
                <Button
                  key={action.to}
                  type="button"
                  icon={action.icon}
                  label={action.label}
                  text
                  className="u-surface-chip px-3 py-2 text-xs font-black"
                  onClick={() => navigate(action.to)}
                />
              ))}
            </div>

            <Button icon="pi pi-bell" text rounded className="text-muted hover:text-main" badge="3" />
          </div>
        </header>

        <main id="main-content" className="flex-grow-1 overflow-y-auto p-6 lg:p-10 bg-app">
          <div className="page-container mx-auto" style={{ maxWidth: '1300px' }}>
            {children}
          </div>
        </main>

        <nav className="lg:hidden flex justify-content-around align-items-center bg-card border-top-1 border-subtle h-5rem px-2 sticky bottom-0 z-5" aria-label={t('nav.main_navigation')}>
          {mobileNav.map(link => (
            <Link
              key={link.to}
              to={link.to}
              className={`flex flex-column align-items-center gap-1 no-underline ${location.pathname === link.to ? 'text-brand-primary' : 'text-muted'}`}
              data-testid={link.testId}
              aria-label={link.label}
              aria-current={location.pathname === link.to ? "page" : undefined}
            >
              <i className={`${link.icon} text-xl`}></i>
              <span style={{ fontSize: '9px' }} className="font-bold uppercase tracking-widest">{link.label.split(' ')[0]}</span>
            </Link>
          ))}
        </nav>
      </div>

      <Sidebar visible={mobileMenuVisible} onHide={() => setMobileMenuVisible(false)} className="w-20rem bg-app">
        <div className="p-4 flex flex-column gap-6">
          <div className="flex align-items-center gap-3">
            <div className="bg-white border-round-xl p-2 shadow-lg"><i className="pi pi-signal text-black"></i></div>
            <span className="text-xl font-black text-main">SignalOS</span>
          </div>
          <nav className="flex flex-column gap-4" aria-label={t('nav.main_navigation')}>
            <NavGroup title={t('nav.group_main')} items={mainNav} />
            <NavGroup title={t('nav.group_social')} items={socialNav} />
            <NavGroup title={t('nav.group_system')} items={personalNav} />
          </nav>
          <Button
            label={t('nav.sign_out')}
            icon="pi pi-power-off"
            text
            className="w-full justify-content-start text-xs font-black text-muted hover:text-danger py-3"
            onClick={handleLogout}
            data-testid="logout-button-mobile"
          />
        </div>
      </Sidebar>
    </div>
  );
}
