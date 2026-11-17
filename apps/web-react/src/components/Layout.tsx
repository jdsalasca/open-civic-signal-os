import { ReactNode, useEffect, useRef, useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { Button } from "primereact/button";
import { Avatar } from "primereact/avatar";
import { Sidebar } from "primereact/sidebar";
import { useAuthStore } from "../store/useAuthStore";
import { useCommunityStore } from "../store/useCommunityStore";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { NotificationBell } from "./ui/NotificationBell";
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

/**
 * Routes that are a destination rather than a navigation item, mapped to the label the topbar should
 * announce for them. Kept as data so adding a detail screen is one line instead of another branch in
 * the component.
 */
const SECTION_FOR_DETAIL_ROUTES: Record<string, string> = {
  "/signal/": "nav.case_detail",
};

const MEMBERSHIP_CACHE_TTL_MS = 5 * 60 * 1000;

export function Layout({ children, authMode = false }: Props) {
  const mainContentId = "main-content";
  const mobileNavId = "mobile-navigation-drawer";
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const interfaceMode = useSettingsStore((state) => state.interfaceMode);
  const theme = useSettingsStore((state) => state.theme);
  const setTheme = useSettingsStore((state) => state.setTheme);
  const textScale = useSettingsStore((state) => state.textScale);
  const setTextScale = useSettingsStore((state) => state.setTextScale);
  const language = useSettingsStore((state) => state.language);
  const setLanguage = useSettingsStore((state) => state.setLanguage);
  const { isLoggedIn, activeRole, userName, logout } = useAuthStore();
  const {
    memberships,
    activeCommunityId,
    setMemberships,
    setActiveCommunityId,
    shouldRefreshMemberships,
  } = useCommunityStore();
  const [mobileMenuVisible, setMobileMenuVisible] = useState(false);
  const [desktopMoreVisible, setDesktopMoreVisible] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [isOnline, setIsOnline] = useState(() =>
    typeof navigator === "undefined" ? true : navigator.onLine
  );
  const mainContentRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    const goOnline = () => setIsOnline(true);
    const goOffline = () => setIsOnline(false);
    window.addEventListener("online", goOnline);
    window.addEventListener("offline", goOffline);
    return () => {
      window.removeEventListener("online", goOnline);
      window.removeEventListener("offline", goOffline);
    };
  }, []);

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

  const activeMembership =
    memberships.find((membership) => membership.communityId === activeCommunityId) ??
    memberships[0] ??
    null;
  const isStaff = activeRole === "PUBLIC_SERVANT" || activeRole === "SUPER_ADMIN";
  // Citizens in simple mode get the community essentials; the administrative and analytical tools
  // stay behind the progressive "More options" panel or the advanced mode.
  const showAdvancedTools = isStaff || interfaceMode === "advanced";
  const canAccessModeration =
    isStaff ||
    activeMembership?.role === "MODERATOR" ||
    activeMembership?.role === "COORDINATOR" ||
    activeMembership?.role === "PUBLIC_SERVANT_LIAISON";
  const canAccessOpenData =
    isStaff ||
    activeMembership?.role === "COORDINATOR" ||
    activeMembership?.role === "PUBLIC_SERVANT_LIAISON";
  const primaryNav: NavItem[] = [
    { label: t('nav.insights'), to: '/', icon: 'pi pi-th-large', visible: isLoggedIn },
    { label: t('nav.chat'), to: '/communities/rooms', icon: 'pi pi-comments', visible: isLoggedIn },
    { label: t('nav.report'), to: '/report', icon: 'pi pi-plus-circle', visible: isLoggedIn, testId: 'report-issue-button' },
  ];
  const collaborationNav: NavItem[] = [
      { label: t('nav.community_trust'), to: '/communities/trust', icon: 'pi pi-chart-line', visible: isLoggedIn && showAdvancedTools },
      { label: t('nav.community_merge_review'), to: '/communities/merge-review', icon: 'pi pi-clone', visible: isLoggedIn && showAdvancedTools },
      { label: t('nav.community_weekly_digest'), to: '/communities/weekly-digest', icon: 'pi pi-envelope', visible: isLoggedIn && showAdvancedTools },
    { label: t('nav.community_decisions'), to: '/communities/decisions', icon: 'pi pi-sitemap', visible: isLoggedIn && showAdvancedTools },
    { label: t('nav.community_projects'), to: '/communities/projects', icon: 'pi pi-briefcase', visible: isLoggedIn && showAdvancedTools },
    { label: t('nav.community_governance'), to: '/communities/governance', icon: 'pi pi-book', visible: isLoggedIn && showAdvancedTools },
    { label: t('nav.community_open_data'), to: '/communities/open-data', icon: 'pi pi-database', visible: isLoggedIn && showAdvancedTools && Boolean(canAccessOpenData) },
    { label: t('nav.public_blog'), to: '/communities/blog', icon: 'pi pi-megaphone', visible: isLoggedIn && showAdvancedTools },
    { label: t('nav.dialogues'), to: '/communities/threads', icon: 'pi pi-comments', visible: isLoggedIn && showAdvancedTools },
    { label: t('nav.community_activities'), to: '/communities/activities', icon: 'pi pi-calendar', visible: isLoggedIn && showAdvancedTools },
    { label: t('nav.community_resources'), to: '/communities/resources', icon: 'pi pi-building', visible: isLoggedIn && showAdvancedTools },
    { label: t('nav.community_integrations'), to: '/communities/integrations', icon: 'pi pi-send', visible: isLoggedIn && showAdvancedTools && Boolean(canAccessOpenData) },
  ];
  const advancedNav: NavItem[] = [
    { label: t('nav.overview'), to: '/dashboard', icon: 'pi pi-chart-bar', visible: isLoggedIn && showAdvancedTools },
    { label: t('nav.moderation'), to: '/moderation', icon: 'pi pi-shield', visible: isLoggedIn && Boolean(canAccessModeration) },
    { label: t('nav.communities'), to: '/communities', icon: 'pi pi-globe', visible: isLoggedIn },
    { label: t('nav.help_center'), to: '/help', icon: 'pi pi-question-circle', visible: isLoggedIn },
    { label: t('nav.settings'), to: '/settings', icon: 'pi pi-cog', visible: isLoggedIn },
  ];
  const navItems = [...primaryNav, ...collaborationNav, ...advancedNav];
  const matchedNavItem = navItems.find((item) => item.to === location.pathname);
  /*
   * Detail routes are not navigation destinations, so an exact match can never find them and the
   * old fallback rendered nav.insights, whose value is literally "Home", on every case screen.
   * Longest-prefix keeps a nested route under the section that contains it, and the explicit map
   * covers routes with no nav ancestor at all.
   */
  const sectionFromPrefix = navItems
    .filter((item) => item.to !== "/" && location.pathname.startsWith(`${item.to}/`))
    .sort((a, b) => b.to.length - a.to.length)[0];
  const detailRoute = Object.keys(SECTION_FOR_DETAIL_ROUTES).find((prefix) =>
    location.pathname.startsWith(prefix),
  );
  const activeSection =
    matchedNavItem?.label ??
    sectionFromPrefix?.label ??
    (detailRoute ? t(SECTION_FOR_DETAIL_ROUTES[detailRoute]) : t("nav.insights"));
  const isSectionRoute = Boolean(matchedNavItem);
  /*
   * "What needs attention today" is a dashboard framing. Showing it above a single case described
   * nothing, so the framing line is rendered only where it actually frames something.
   */
  const topbarEyebrow = isSectionRoute
    ? location.pathname === "/" || location.pathname === "/communities/feed"
      ? t("community_feed.kicker")
      : t("dashboard.focus_today")
    : null;

  const mobileNav = [
    ...primaryNav,
    { label: t('nav.my_contributions_short'), to: '/mine', icon: 'pi pi-user', visible: isLoggedIn },
  ].filter((item) => item.visible);

  useEffect(() => {
    setDesktopMoreVisible(false);
    setUserMenuOpen(false);
  }, [location.pathname]);

  /*
   * Public pages (landing, login, register, verify, backlog) skip the full shell, but the keyboard
   * contract must not skip with it: the first Tab lands on the skip link and Enter moves focus into
   * the main landmark, exactly as it does inside the authenticated layout.
   */
  if (authMode) {
    return (
      <div className="auth-page min-h-screen">
        <a
          href={`#${mainContentId}`}
          className="skip-link"
          data-testid="skip-to-content-link"
          onClick={() => window.requestAnimationFrame(() => document.getElementById(mainContentId)?.focus())}
        >
          {t('nav.skip_to_content')}
        </a>
        <main id={mainContentId} tabIndex={-1} data-testid="main-content">
          {children}
        </main>
      </div>
    );
  }

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
      {/* TOP NAVIGATION */}
      <div className="flex flex-column flex-grow-1 overflow-hidden relative">
        <header className="app-topbar py-2 flex align-items-center justify-content-between gap-3 px-4 lg:px-6 border-bottom-1 border-subtle z-5">
          <div className="app-topbar-left">
            <Button
              type="button"
              icon="pi pi-bars"
              text
              className="lg:hidden text-main"
              onClick={() => setMobileMenuVisible(true)}
              aria-label={t('nav.open_navigation')}
              aria-expanded={mobileMenuVisible}
              aria-controls={mobileNavId}
              data-testid="mobile-menu-toggle"
            />
            <Link to="/" className="app-topbar-brand" aria-label="SignalOS">
              <span className="u-logo-badge border-round-lg flex align-items-center justify-content-center">
                <i className="pi pi-signal u-logo-icon"></i>
              </span>
              <span className="app-topbar-brand-name">SignalOS</span>
            </Link>
            <nav className="app-topnav hidden lg:flex" aria-label={t('nav.main_navigation')}>
              {primaryNav.filter((link) => link.visible).map((link) => (
                <Link
                  key={link.to}
                  to={link.to}
                  className={`app-topnav-link ${location.pathname === link.to ? 'is-active' : ''}`}
                  data-testid={link.testId}
                  aria-current={location.pathname === link.to ? "page" : undefined}
                >
                  {link.label}
                </Link>
              ))}
              <div className="app-topnav-more-wrap">
                <button
                  type="button"
                  className="app-topnav-link"
                  onClick={() => setDesktopMoreVisible((current) => !current)}
                  aria-expanded={desktopMoreVisible}
                  data-testid="desktop-more-toggle"
                >
                  {desktopMoreVisible ? t('nav.hide_more_tools') : t('nav.more_short')}
                  <i className="pi pi-chevron-down text-xs" aria-hidden="true" />
                </button>
                {desktopMoreVisible && (
                  <div className="app-topnav-more" data-testid="desktop-more-panel">
                    {[...collaborationNav, ...advancedNav].filter((item) => item.visible).map((item) => (
                      <Link key={item.to} to={item.to} className="app-topnav-more-link" onClick={() => setDesktopMoreVisible(false)}>
                        <i className={item.icon} aria-hidden="true" />
                        {item.label}
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            </nav>
          </div>

          <div className="app-topbar-actions">
            {memberships.length > 0 && (
              <label className="app-community-switch">
                <span className="sr-only">{t('nav.active_community')}</span>
                <select
                  value={activeCommunityId ?? memberships[0].communityId}
                  onChange={(event) => void handleCommunitySwitch(event.target.value)}
                  data-testid="community-switch-dropdown"
                >
                  {memberships.map((membership) => (
                    <option key={membership.communityId} value={membership.communityId}>
                      {membership.communityName}
                    </option>
                  ))}
                </select>
              </label>
            )}

            <NotificationBell />
            <div className="app-user-menu-wrap">
              <button
                type="button"
                className="app-user-menu-toggle"
                onClick={() => setUserMenuOpen((current) => !current)}
                aria-expanded={userMenuOpen}
                aria-label={userName ?? t('nav.profile')}
                data-testid="user-menu-toggle"
              >
                <Avatar label={userName?.[0]?.toUpperCase() ?? '?'} shape="circle" className="bg-brand-primary text-on-brand font-bold" />
              </button>
              {userMenuOpen && (
                <div className="app-user-menu" data-testid="user-menu">
                  <div className="app-user-menu-head">
                    <strong>{userName}</strong>
                    <span>{t('nav.clearance')}: {toRoleLabel(activeRole, t)}</span>
                  </div>
                  <span className="u-pill" data-testid="interface-mode-badge">
                    <i className={`pi ${interfaceMode === "simple" ? "pi-sparkles" : "pi-sliders-h"}`} aria-hidden="true"></i>
                    {interfaceMode === "simple" ? t("settings.interface_modes.simple") : t("settings.interface_modes.advanced")}
                  </span>
                  <Link to="/mine" className="app-user-menu-link" onClick={() => setUserMenuOpen(false)}>
                    <i className="pi pi-user" aria-hidden="true" />
                    {t('nav.my_contributions')}
                  </Link>
                  <div className="app-user-menu-theme">
                    <span>{t('settings.theme')}</span>
                    <div>
                      <button
                        type="button"
                        className={theme === 'light' ? 'is-active' : ''}
                        onClick={() => setTheme('light')}
                      >
                        {t('settings.light')}
                      </button>
                      <button
                        type="button"
                        className={theme === 'dark' ? 'is-active' : ''}
                        onClick={() => setTheme('dark')}
                      >
                        {t('settings.dark')}
                      </button>
                    </div>
                  </div>
                  <div className="app-user-menu-theme">
                    <span>{t('settings.text_scale')}</span>
                    <div>
                      <button
                        type="button"
                        className={textScale === 'normal' ? 'is-active' : ''}
                        onClick={() => setTextScale('normal')}
                        data-testid="text-scale-normal"
                      >
                        A
                      </button>
                      <button
                        type="button"
                        className={textScale === 'large' ? 'is-active' : ''}
                        onClick={() => setTextScale('large')}
                        data-testid="text-scale-large"
                      >
                        A+
                      </button>
                    </div>
                  </div>
                  <div className="app-user-menu-theme">
                    <span>{t('settings.language')}</span>
                    <div>
                      <button
                        type="button"
                        className={language === 'es' ? 'is-active' : ''}
                        onClick={() => {
                          setLanguage('es');
                          i18n.changeLanguage('es');
                        }}
                        data-testid="language-wheel-es"
                      >
                        ES
                      </button>
                      <button
                        type="button"
                        className={language === 'en' ? 'is-active' : ''}
                        onClick={() => {
                          setLanguage('en');
                          i18n.changeLanguage('en');
                        }}
                        data-testid="language-wheel-en"
                      >
                        EN
                      </button>
                    </div>
                  </div>
                  <Link to="/settings" className="app-user-menu-link" onClick={() => setUserMenuOpen(false)}>
                    <i className="pi pi-cog" aria-hidden="true" />
                    {t('nav.settings')}
                  </Link>
                  <button type="button" className="app-user-menu-link" onClick={handleLogout} data-testid="logout-button-desktop">
                    <i className="pi pi-power-off" aria-hidden="true" />
                    {t('nav.sign_out')}
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        {!isOnline && (
          <div className="offline-banner" role="status" data-testid="offline-banner">
            <i className="pi pi-cloud" aria-hidden="true" />
            <span>{t("offline.banner")}</span>
          </div>
        )}

        <main
          id={mainContentId}
          ref={mainContentRef}
          tabIndex={-1}
          className="app-main flex-grow-1 overflow-y-auto p-4 md:p-6 lg:p-8 bg-app"
          data-testid="main-content"
        >
          <div className="page-container mx-auto" style={{ maxWidth: '1300px' }}>
            <div className="app-page-context">
              {topbarEyebrow && (
                <span className="app-topbar-label" data-testid="app-topbar-label">
                  {topbarEyebrow}
                </span>
              )}
              <span className="app-topbar-title" data-testid="app-topbar-title">
                {activeSection}
              </span>
            </div>
            {children}
          </div>
        </main>

        <nav className="app-mobile-nav lg:hidden flex justify-content-around align-items-center border-top-1 border-subtle min-h-5rem py-2 px-2 sticky bottom-0 z-5" aria-label={t('nav.main_navigation')}>
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
              <span className="font-bold tracking-wide mobile-nav-label">{link.label}</span>
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
            <span className="font-bold tracking-wide mobile-nav-label">{t('nav.more_short')}</span>
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
