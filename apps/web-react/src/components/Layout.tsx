import { ReactNode, useState, useEffect } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { Button } from "primereact/button";
import { Avatar } from "primereact/avatar";
import { Sidebar } from "primereact/sidebar";
import { useAuthStore } from "../store/useAuthStore";
import { useCommunityStore } from "../store/useCommunityStore";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { Dropdown } from "primereact/dropdown";

type Props = {
  children: ReactNode;
  authMode?: boolean;
};

export function Layout({ children, authMode = false }: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { isLoggedIn, activeRole, userName, logout } = useAuthStore();
  const { memberships, activeCommunityId, setMemberships, setActiveCommunityId } = useCommunityStore();
  const [mobileMenuVisible, setMobileMenuVisible] = useState(false);

  useEffect(() => {
    setMobileMenuVisible(false);
  }, [location.pathname]);

  useEffect(() => {
    const loadMemberships = async () => {
      if (!isLoggedIn) return;
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
  }, [isLoggedIn, setMemberships]);

  const handleLogout = async () => {
    try {
      await apiClient.post("auth/logout");
    } catch (err) {
      console.warn(t('auth.logout_warn'));
    } finally {
      logout();
      setMobileMenuVisible(false);
      navigate("/login");
    }
  };

  const isStaff = activeRole === "PUBLIC_SERVANT" || activeRole === "SUPER_ADMIN";

  const navLinks = [
    { label: t('nav.insights'), to: '/', icon: 'pi pi-chart-line', visible: isLoggedIn },
    { label: t('nav.report'), to: '/report', icon: 'pi pi-plus-circle', visible: isLoggedIn },
    { label: t('nav.my_contributions'), to: '/mine', icon: 'pi pi-user', visible: isLoggedIn },
    { label: t('nav.moderation'), to: '/moderation', icon: 'pi pi-shield', visible: isLoggedIn && isStaff },
    { label: t('nav.settings'), to: '/settings', icon: 'pi pi-cog', visible: isLoggedIn },
    { label: 'Communities', to: '/communities', icon: 'pi pi-users', visible: isLoggedIn },
    { label: 'Threads', to: '/communities/threads', icon: 'pi pi-comments', visible: isLoggedIn },
    { label: 'Blog', to: '/communities/blog', icon: 'pi pi-megaphone', visible: isLoggedIn },
    { label: 'Feed', to: '/communities/feed', icon: 'pi pi-list', visible: isLoggedIn },
  ];

  const communityOptions = memberships.map((m) => ({
    label: `${m.communityName} (${m.role})`,
    value: m.communityId,
  }));

  const handleCommunitySwitch = async (communityId: string) => {
    try {
      await apiClient.post(`communities/${communityId}/switch`);
      setActiveCommunityId(communityId);
    } catch (err) {
      console.warn("Community switch failed", err);
    }
  };

  return (
    <div className={`min-h-screen flex flex-column ${authMode ? 'auth-page' : ''}`}>
      <Sidebar 
        visible={mobileMenuVisible} 
        onHide={() => setMobileMenuVisible(false)} 
        position="left" 
        className="w-20rem"
        aria-label={t('nav.main_navigation')}
      >
        <div className="flex flex-column h-full p-3">
          <div className="flex align-items-center gap-2 mb-5">
            <div className="bg-cyan-500 border-round flex align-items-center justify-content-center shadow-4" style={{ width: '32px', height: '32px' }}>
              <i className="pi pi-signal text-gray-900 font-bold"></i>
            </div>
            <span className="text-xl font-black tracking-tighter uppercase">Signal<span className="text-cyan-500">OS</span></span>
          </div>

          <nav className="flex flex-column gap-2 flex-grow-1">
            {navLinks.filter(l => l.visible).map(link => (
              <Link 
                key={link.to} 
                to={link.to} 
                className={`flex align-items-center gap-3 p-3 border-round-lg no-underline transition-colors font-bold ${location.pathname === link.to ? 'bg-cyan-900 text-cyan-400' : 'text-muted hover:text-main hover:bg-white-alpha-5'}`}
              >
                <i className={link.icon}></i>
                <span>{link.label}</span>
              </Link>
            ))}
          </nav>

          {isLoggedIn && (
            <div className="mt-auto border-top-1 border-white-alpha-10 pt-4">
              <div className="flex align-items-center gap-3 mb-4 px-2">
                <Avatar label={userName?.[0].toUpperCase()} shape="circle" className="bg-cyan-600 text-white font-bold" />
                <div className="flex flex-column">
                  <span className="text-sm font-bold line-height-1 mb-1">{userName}</span>
                  <span className="text-xs text-cyan-500 font-bold uppercase tracking-widest" style={{fontSize: '9px'}}>{activeRole}</span>
                </div>
              </div>
              <Button label={t('nav.sign_out')} icon="pi pi-power-off" severity="danger" text className="w-full justify-content-start font-bold py-3" onClick={handleLogout} data-testid="logout-button-mobile" />
            </div>
          )}
        </div>
      </Sidebar>
      
      <nav className="surface-section px-4 md:px-6 py-3 shadow-4 flex justify-content-between align-items-center border-bottom-1 border-white-alpha-10 z-5 sticky top-0" style={{ backgroundColor: 'var(--nav-bg)', backdropFilter: 'blur(12px)' }}>
        <div className="flex align-items-center gap-5">
          <Link to="/" className="flex align-items-center gap-2 no-underline">
            <div className="bg-cyan-500 border-round-md flex align-items-center justify-content-center" style={{ width: '32px', height: '32px' }}>
              <i className="pi pi-signal text-gray-900 font-bold"></i>
            </div>
            <span className="text-xl font-black tracking-tighter uppercase text-main">Signal<span className="text-cyan-500">OS</span></span>
          </Link>
          
          {!authMode && isLoggedIn && (
            <div className="hidden lg:flex align-items-center gap-4 ml-4">
              {navLinks.filter(l => l.visible).map(link => (
                <Link 
                  key={link.to} 
                  to={link.to} 
                  className={`no-underline font-bold text-sm transition-colors transition-duration-200 ${location.pathname === link.to ? 'text-cyan-400' : 'text-muted hover:text-main'}`}
                >
                  {link.label}
                </Link>
              ))}
            </div>
          )}
        </div>

        <div className="flex align-items-center gap-3">
          {!authMode && isLoggedIn && (
            <Button icon="pi pi-bars" text className="lg:hidden text-main p-0" onClick={() => setMobileMenuVisible(true)} aria-label={t('nav.open_navigation')} />
          )}

          {!authMode && isLoggedIn && communityOptions.length > 0 && (
            <Dropdown
              value={activeCommunityId || communityOptions[0].value}
              options={communityOptions}
              onChange={(e) => handleCommunitySwitch(e.value)}
              placeholder="Select Community"
              className="w-14rem hidden md:block"
            />
          )}

          {!authMode && !isLoggedIn && (
            <div className="flex gap-2">
              <Link to="/login" className="no-underline">
                <Button label={t('nav.sign_in')} size="small" text className="text-main hover:text-cyan-400 font-bold" />
              </Link>
              <Link to="/register" className="no-underline">
                <Button label={t('nav.join_now')} size="small" className="p-button-primary px-3 font-bold" />
              </Link>
            </div>
          )}

          {isLoggedIn && (
            <div className="hidden lg:flex align-items-center gap-3 bg-white-alpha-5 py-1 pl-3 pr-1 border-round-right-3xl border-round-left-xl border-1 border-white-alpha-10">
              <div className="flex flex-column align-items-end mr-1">
                <span className="text-xs font-bold line-height-1 mb-1">{userName}</span>
                <span className="text-min font-bold text-cyan-500 uppercase tracking-tighter" style={{ fontSize: '9px' }}>{activeRole}</span>
              </div>
              <Avatar label={userName?.[0].toUpperCase()} shape="circle" className="bg-cyan-600 text-white font-bold" />
              <Button icon="pi pi-power-off" rounded text className="text-muted hover:text-red-400 ml-1" onClick={handleLogout} aria-label={t('nav.sign_out')} data-testid="logout-button-desktop" />
            </div>
          )}
        </div>
      </nav>

      <main className="flex-grow-1 p-4 md:p-6">
        <div className="page-container h-full">{children}</div>
      </main>

      <footer className="app-footer p-6 flex flex-column md:flex-row justify-content-between align-items-center border-top-1 border-white-alpha-10 gap-4">
        <div className="flex align-items-center gap-2">
          <i className="pi pi-globe text-muted"></i>
          <span className="text-muted text-sm font-bold uppercase tracking-widest text-xs">{t('nav.global_standard')}</span>
        </div>
        <div className="text-muted text-xs text-center font-bold">
          &copy; 2026 Open Civic Signal OS. {t('nav.protocol_version')}
        </div>
        <div className="flex gap-4">
          <i className="pi pi-github social-link cursor-pointer transition-colors text-xl"></i>
          <i className="pi pi-twitter social-link cursor-pointer transition-colors text-xl"></i>
        </div>
      </footer>
    </div>
  );
}
