import { ReactNode, useEffect, useRef, useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { Button } from "primereact/button";
import { Avatar } from "primereact/avatar";
import { Sidebar } from "primereact/sidebar";
import { useAuthStore } from "../store/useAuthStore";
import { useCommunityStore } from "../store/useCommunityStore";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { CivicSelect } from "./ui/CivicSelect";
import { toRoleLabel } from "../constants/roleLabels";
import { useSettingsStore } from "../store/useSettingsStore";

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
  const mainContentId = "main-content";
  const mobileNavId = "mobile-navigation-drawer";
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const interfaceMode = useSettingsStore((state) => state.interfaceMode);
  const { isLoggedIn, activeRole, userName, logout } = useAuthStore();
  const {
    memberships,
    activeCommunityId,
    setMemberships,
    setActiveCommunityId,
    shouldRefreshMemberships,
  } = useCommunityStore();
  const [mobileMenuVisible, setMobileMenuVisible] = useState(false);
  const [desktopMoreVisible, setDesktopMoreVisible] = useState(interfaceMode === "advanced");
  const mainContentRef = useRef<HTMLElement | null>(null);

  const focusMainContentTarget = () => {
    const mainElement = mainContentRef.current;
    if (!mainElement) {
      return;
    }
    const firstFocusable = mainElement.querySelector<HTMLElement>(
      'a[href], button:not([disabled]), input:not([disabled]), textarea:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'
    );
    (firstFocusable ?? mainElement).focus();
  };

  useEffect(() => {
    setDesktopMoreVisible(interfaceMode === "advanced");
  }, [interfaceMode]);

  useEffect(() => {
    let cancelled = false;
    const loadMemberships = async () => {
      if (!isLoggedIn) return;
      if (!shouldRefreshMemberships(MEMBERSHIP_CACHE_TTL_MS)) return;
      try {
        const res = await apiClient.get("communities/my");
        if (!cancelled && res.status === 200) {
          setMemberships(res.data || []);
        }
      } catch (err) {
        console.warn("Failed to load community memberships", err);
      }
    };
    loadMemberships();
    return () => {
      cancelled = true;
    };
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
    if (communityId === activeCommunityId) {
      return;
    }
    try {
      await apiClient.post(`communities/${communityId}/switch`);
      setActiveCommunityId(communityId);
    } catch (err) {
      console.warn("Community switch failed", err);
    }
  };

  const isStaff = activeRole === "PUBLIC_SERVANT" || activeRole === "SUPER_ADMIN";

  const primaryNav: NavItem[] = [
    { label: t('nav.insights'), to: '/', icon: 'pi pi-th-large', visible: isLoggedIn },
    { label: t('nav.report'), to: '/report', icon: 'pi pi-plus-circle', visible: isLoggedIn, testId: 'report-issue-button' },
    { label: t('nav.my_contributions_short'), to: '/mine', icon: 'pi pi-user', visible: isLoggedIn },
  ];

  const collaborationNav: NavItem[] = [
    { label: t('nav.live_feed'), to: '/communities/feed', icon: 'pi pi-bolt', visible: isLoggedIn },
    { label: t('nav.public_blog'), to: '/communities/blog', icon: 'pi pi-megaphone', visible: isLoggedIn },
    { label: t('nav.dialogues'), to: '/communities/threads', icon: 'pi pi-comments', visible: isLoggedIn },
  ];

  const advancedNav: NavItem[] = [
    { label: t('nav.moderation'), to: '/moderation', icon: 'pi pi-shield', visible: isLoggedIn && isStaff },
    { label: t('nav.communities'), to: '/communities', icon: 'pi pi-globe', visible: isLoggedIn },
    { label: t('nav.settings'), to: '/settings', icon: 'pi pi-cog', visible: isLoggedIn },
  ];

  const communityOptions = memberships.map((m) => ({
    label: m.communityName,
    value: m.communityId,
    role: m.role
  }));
  const activeMembership =
    memberships.find((membership) => membership.communityId === activeCommunityId) ??
    memberships[0] ??
    null;
  const activeBreadcrumb = activeMembership?.breadcrumb ?? [];
  const activeSection =
    [...primaryNav, ...collaborationNav, ...advancedNav].find((item) => item.to === location.pathname)?.label ??
    t("nav.insights");

  const visibleMoreCount = [...collaborationNav, ...advancedNav].filter((item) => item.visible).length;
  const mobileNav = primaryNav.filter((item) => item.visible);

  if (authMode) return <div className="auth-page min-h-screen">{children}</div>;

  const NavGroup = ({ title, items }: { title: string, items: NavItem[] }) => (
    <div className="mb-6">
      <div className="text-muted text-xs font-black uppercase tracking-widest mb-3 ml-4 nav-group-title">{title}</div>
      <div className="flex flex-column gap-1">
        {items.filter(l => l.visible).map(link => (
          <Link
            key={link.to}
            to={link.to}
            className={`app-nav-link font-bold ${location.pathname === link.to ? 'is-active' : ''}`}
            data-testid={link.testId}
            aria-label={link.label}
            aria-current={location.pathname === link.to ? "page" : undefined}
            onClick={() => setMobileMenuVisible(false)}
          >
            <div className="flex align-items-center gap-3">
              <i className={`${link.icon} app-nav-link-icon text-base`}></i>
              <span className="text-sm tracking-tight">{link.label}</span>
            </div>
            {location.pathname === link.to && <div className="app-nav-link-dot"></div>}
          </Link>
        ))}
      </div>
    </div>
  );

  return (
    <div className={`app-shell flex h-screen overflow-hidden bg-app interface-mode-shell-${interfaceMode}`}>
      <a
        href={`#${mainContentId}`}
        className="skip-link"
        data-testid="skip-to-content-link"
        onClick={() => window.requestAnimationFrame(focusMainContentTarget)}
      >
        {t('nav.skip_to_content')}
      </a>
      {/* SIDEBAR */}
      <aside className="app-sidebar hidden lg:flex flex-column w-21rem border-right-1 border-white-alpha-5 z-2">
        <div className="app-sidebar-brand">
          <div className="app-brand-panel motion-card">
            <div className="flex align-items-center gap-3 mb-4">
              <div className="u-logo-badge border-round-2xl flex align-items-center justify-content-center shadow-premium" style={{ width: '44px', height: '44px' }}>
                <i className="pi pi-signal u-logo-icon text-xl"></i>
              </div>
              <div className="flex flex-column">
                <span className="app-brand-kicker">{t("nav.brand_kicker")}</span>
                <span className="app-brand-title text-2xl">SignalOS</span>
              </div>
            </div>
            <p className="app-brand-copy m-0">
              {t("dashboard.guided_home.citizen.hero_subtitle")}
            </p>
          </div>
        </div>

        <nav className="flex-grow-1 px-3 py-4 overflow-y-auto" aria-label={t('nav.main_navigation')}>
          {activeMembership && (
            <div className="app-context-card mb-5 mx-3 motion-card" data-testid="layout-community-context-card">
              <span className="app-context-label">{t("nav.active_community")}</span>
              <div className="app-context-value mt-2">{activeMembership.communityName}</div>
              {activeBreadcrumb.length > 1 && (
                <p className="text-xs text-muted mt-2 mb-0 line-height-3">
                  {activeBreadcrumb.map((item) => item.name).join(" / ")}
                </p>
              )}
              <div className="flex align-items-center gap-2 mt-3">
                <span className="u-pill">
                  <i className="pi pi-users text-brand-primary"></i>
                  {toRoleLabel(activeMembership.role, t)}
                </span>
                <span className="u-pill" data-testid="interface-mode-badge">
                  <i className={`pi ${interfaceMode === "simple" ? "pi-sparkles" : "pi-sliders-h"} text-brand-primary`}></i>
                  {interfaceMode === "simple" ? t("settings.interface_modes.simple") : t("settings.interface_modes.advanced")}
                </span>
              </div>
            </div>
          )}
          <NavGroup title={t('nav.group_primary')} items={primaryNav} />
          <div className="mb-6">
            <div className="px-2">
              <button
                type="button"
                className="w-full flex align-items-center justify-content-between px-4 py-3 border-round-xl border-1 border-subtle bg-surface text-main font-bold cursor-pointer"
                onClick={() => setDesktopMoreVisible((current) => !current)}
                aria-expanded={desktopMoreVisible}
                data-testid="desktop-more-toggle"
              >
                <span className="flex align-items-center gap-2">
                  <i className="pi pi-compass text-brand-primary"></i>
                  {desktopMoreVisible ? t('nav.hide_more_tools') : t('nav.show_more_tools')}
                </span>
                <span className="text-xs text-muted">{visibleMoreCount}</span>
              </button>
              <p className="text-xs text-muted mt-2 mb-0 px-2" data-testid="desktop-more-hint">
                {t('nav.more_tools_hint')}
              </p>
            </div>
            {desktopMoreVisible && (
              <div className="mt-4" data-testid="desktop-more-panel">
                <NavGroup title={t('nav.group_collaboration')} items={collaborationNav} />
                <NavGroup title={t('nav.group_tools')} items={advancedNav} />
              </div>
            )}
          </div>
        </nav>

        <div className="mt-auto p-4 border-top-1 border-subtle bg-surface">
          <div className="flex flex-column gap-4">
            <div className="flex align-items-center gap-3 px-2">
              <Avatar label={userName?.[0].toUpperCase()} shape="circle" className="bg-brand-primary text-white font-bold" />
              <div className="flex flex-column overflow-hidden">
                <span className="text-xs font-black text-main truncate uppercase tracking-wider">{userName}</span>
                <span className="text-min font-bold text-muted uppercase nav-clearance-label">
                  {t('nav.clearance')}: {toRoleLabel(activeRole, t)}
                </span>
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
        <header className="app-topbar h-6rem flex align-items-center justify-content-between px-4 lg:px-6 border-bottom-1 border-subtle z-1">
          <div className="flex align-items-center gap-4 flex-grow-1">
            <Button
              icon="pi pi-bars"
              text
              className="lg:hidden text-main"
              onClick={() => setMobileMenuVisible(true)}
              aria-label={t('nav.open_navigation')}
              aria-expanded={mobileMenuVisible}
              aria-controls={mobileNavId}
              data-testid="mobile-menu-toggle"
            />
            <div className="app-topbar-intro">
              <span className="app-topbar-label">{t('dashboard.focus_today')}</span>
              <span className="app-topbar-title">{activeSection}</span>
              {activeMembership && (
                <span className="text-xs text-muted line-height-3">
                  {activeMembership.breadcrumb.map((item) => item.name).join(' / ')} · {toRoleLabel(activeMembership.role, t)}
                </span>
              )}
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
                      <small className="text-muted text-min font-mono mt-1">{toRoleLabel(option.role, t)}</small>
                    </div>
                  )}
                />
              </div>
            )}

            {interfaceMode === "advanced" && (
              <div className="hidden xl:flex align-items-center gap-3 u-pill">
                <div className="w-8px h-8px border-circle bg-status-resolved animate-pulse"></div>
                <span className="text-xs font-black text-main uppercase tracking-widest">{t('nav.core_active')}</span>
              </div>
            )}

            <Button
              type="button"
              icon="pi pi-plus-circle"
              label={t('nav.report')}
              text
              className="hidden lg:flex u-surface-chip u-surface-chip-compact px-3 py-2 text-xs font-black"
              onClick={() => navigate('/report')}
              data-testid="header-primary-report"
            />

            {interfaceMode === "advanced" && (
              <Button icon="pi pi-bell" text rounded className="text-muted hover:text-main" badge="3" />
            )}
          </div>
        </header>

        <main
          id={mainContentId}
          ref={mainContentRef}
          tabIndex={-1}
          className="app-main flex-grow-1 overflow-y-auto p-4 md:p-6 lg:p-10 bg-app"
          data-testid="main-content"
        >
          <div className="page-container mx-auto" style={{ maxWidth: '1300px' }}>
            {children}
          </div>
        </main>

        <nav className="app-mobile-nav lg:hidden flex justify-content-around align-items-center border-top-1 border-subtle h-5rem px-2 sticky bottom-0 z-5" aria-label={t('nav.main_navigation')}>
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
          <button
            type="button"
            className={`flex flex-column align-items-center gap-1 border-none bg-transparent ${mobileMenuVisible ? 'text-brand-primary' : 'text-muted'}`}
            onClick={() => setMobileMenuVisible(true)}
            aria-label={t('nav.open_navigation')}
            aria-expanded={mobileMenuVisible}
            aria-controls={mobileNavId}
            data-testid="mobile-more-toggle"
          >
            <i className="pi pi-ellipsis-h text-xl"></i>
            <span style={{ fontSize: '9px' }} className="font-bold uppercase tracking-widest">{t('nav.more_short')}</span>
          </button>
        </nav>
      </div>

      <Sidebar
        visible={mobileMenuVisible}
        onHide={() => setMobileMenuVisible(false)}
        className="w-20rem bg-app"
        id={mobileNavId}
      >
        <div className="p-4 flex flex-column gap-6">
          <div className="app-brand-panel">
            <div className="flex align-items-center gap-3 mb-3">
              <div className="u-logo-badge border-round-2xl p-3 shadow-lg"><i className="pi pi-signal u-logo-icon"></i></div>
              <div className="flex flex-column">
                <span className="app-brand-kicker">{t("nav.brand_kicker")}</span>
                <span className="app-brand-title text-xl">SignalOS</span>
              </div>
            </div>
            <p className="app-brand-copy m-0">{t('nav.mobile_drawer_guidance')}</p>
          </div>
          <nav className="flex flex-column gap-4" aria-label={t('nav.main_navigation')}>
            <NavGroup title={t('nav.group_primary')} items={primaryNav} />
            <NavGroup title={t('nav.group_collaboration')} items={collaborationNav} />
            <NavGroup title={t('nav.group_tools')} items={advancedNav} />
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
