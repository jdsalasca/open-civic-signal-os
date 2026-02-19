import { ReactNode, useRef, useState, useEffect } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { Button } from "primereact/button";
import { Avatar } from "primereact/avatar";
import { Toast } from "primereact/toast";
import { Sidebar } from "primereact/sidebar";
import { useAuthStore } from "../store/useAuthStore";

type Props = {
  children: ReactNode;
  authMode?: boolean;
};

export function Layout({ children, authMode = false }: Props) {
  const navigate = useNavigate();
  const location = useLocation();
  const { isLoggedIn, role, user, logout } = useAuthStore();
  const toastRef = useRef<Toast>(null);
  const [mobileMenuVisible, setMobileMenuVisible] = useState(false);

  // UX-003: Close drawer on route change
  useEffect(() => {
    setMobileMenuVisible(false);
  }, [location.pathname]);

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const isStaff = role === "PUBLIC_SERVANT" || role === "SUPER_ADMIN";

  const navLinks = [
    { label: 'Insights', to: '/', icon: 'pi pi-chart-line', visible: isLoggedIn },
    { label: 'Public Report', to: '/report', icon: 'pi pi-plus-circle', visible: isLoggedIn },
    { label: 'Moderation', to: '/moderation', icon: 'pi pi-shield', visible: isLoggedIn && isStaff },
  ];

  return (
    <div className={`min-h-screen flex flex-column bg-gray-900 ${authMode ? 'auth-page' : ''}`}>
      <Toast ref={toastRef} />
      
      {/* UX-003: Hardened Mobile Drawer */}
      <Sidebar 
        visible={mobileMenuVisible} 
        onHide={() => setMobileMenuVisible(false)} 
        position="left" 
        className="bg-gray-900 border-right-1 border-white-alpha-10 w-20rem"
        aria-label="Main Navigation"
      >
        <div className="flex flex-column h-full p-3">
          <div className="flex align-items-center gap-2 mb-5">
            <div className="bg-cyan-500 border-round flex align-items-center justify-content-center shadow-4" style={{ width: '32px', height: '32px' }}>
              <i className="pi pi-signal text-gray-900 font-bold"></i>
            </div>
            <span className="text-xl font-black text-white tracking-tighter uppercase">Signal<span className="text-cyan-500">OS</span></span>
          </div>

          <nav className="flex flex-column gap-2 flex-grow-1">
            {navLinks.filter(l => l.visible).map(link => (
              <Link 
                key={link.to} 
                to={link.to} 
                className={`flex align-items-center gap-3 p-3 border-round-lg no-underline transition-colors font-bold ${location.pathname === link.to ? 'bg-cyan-900 text-cyan-400' : 'text-gray-400 hover:text-white hover:bg-white-alpha-5'}`}
              >
                <i className={link.icon}></i>
                <span>{link.label}</span>
              </Link>
            ))}
          </nav>

          {isLoggedIn && (
            <div className="mt-auto border-top-1 border-white-alpha-10 pt-4">
              <div className="flex align-items-center gap-3 mb-4 px-2">
                <Avatar label={user?.[0].toUpperCase()} shape="circle" className="bg-cyan-600 text-white font-bold" />
                <div className="flex flex-column">
                  <span className="text-sm font-bold text-white line-height-1 mb-1">{user}</span>
                  <span className="text-xs text-cyan-500 font-bold uppercase tracking-widest" style={{fontSize: '9px'}}>{role}</span>
                </div>
              </div>
              <Button label="Sign Out" icon="pi pi-power-off" severity="danger" text className="w-full justify-content-start font-bold py-3" onClick={handleLogout} />
            </div>
          )}
        </div>
      </Sidebar>
      
      {/* Header */}
      <nav className="surface-section px-4 md:px-6 py-3 shadow-4 flex justify-content-between align-items-center border-bottom-1 border-white-alpha-10 z-5 sticky top-0">
        <div className="flex align-items-center gap-5">
          <Link to="/" className="flex align-items-center gap-2 no-underline">
            <div className="bg-cyan-500 border-round-md flex align-items-center justify-content-center" style={{ width: '32px', height: '32px' }}>
              <i className="pi pi-signal text-gray-900 font-bold"></i>
            </div>
            <span className="text-xl font-black text-white tracking-tighter uppercase">Signal<span className="text-cyan-500">OS</span></span>
          </Link>
          
          {!authMode && isLoggedIn && (
            <div className="hidden lg:flex align-items-center gap-4 ml-4">
              {navLinks.filter(l => l.visible).map(link => (
                <Link 
                  key={link.to} 
                  to={link.to} 
                  className={`no-underline font-bold text-sm transition-colors transition-duration-200 ${location.pathname === link.to ? 'text-cyan-400' : 'text-gray-400 hover:text-white'}`}
                >
                  {link.label}
                </Link>
              ))}
            </div>
          )}
        </div>

        <div className="flex align-items-center gap-3">
          {!authMode && isLoggedIn && (
            <Button 
              icon="pi pi-bars" 
              text 
              className="lg:hidden text-white p-0" 
              onClick={() => setMobileMenuVisible(true)} 
              aria-label="Open Navigation Menu"
            />
          )}

          {!authMode && !isLoggedIn && (
            <div className="flex gap-2">
              <Link to="/login" className="no-underline">
                <Button label="Sign In" size="small" text className="text-white hover:text-cyan-400 font-bold" />
              </Link>
              <Link to="/register" className="no-underline">
                <Button label="Join Now" size="small" className="p-button-primary px-3 font-bold" />
              </Link>
            </div>
          )}

          {isLoggedIn && (
            <div className="hidden lg:flex align-items-center gap-3 bg-gray-800 py-1 pl-3 pr-1 border-round-right-3xl border-round-left-xl border-1 border-white-alpha-10">
              <div className="flex flex-column align-items-end mr-1">
                <span className="text-xs font-bold text-white line-height-1 mb-1">{user}</span>
                <span className="text-min font-bold text-cyan-500 uppercase tracking-tighter" style={{ fontSize: '9px' }}>{role}</span>
              </div>
              <Avatar label={user?.[0].toUpperCase()} shape="circle" className="bg-cyan-600 text-white font-bold" />
              <Button 
                icon="pi pi-power-off" 
                rounded 
                text 
                className="text-gray-500 hover:text-red-400 ml-1"
                onClick={handleLogout} 
                aria-label="Logout"
              />
            </div>
          )}
        </div>
      </nav>

      <main className="flex-grow-1 p-4 md:p-6 bg-gray-950">
        <div className="page-container h-full">
          {children}
        </div>
      </main>

      <footer className="bg-gray-950 p-6 flex flex-column md:flex-row justify-content-between align-items-center border-top-1 border-white-alpha-10 gap-4">
        <div className="flex align-items-center gap-2">
          <i className="pi pi-globe text-gray-500"></i>
          <span className="text-gray-400 text-sm font-bold uppercase tracking-widest text-xs">Global Governance Standard</span>
        </div>
        <div className="text-gray-400 text-xs text-center font-bold">
          &copy; 2026 Open Civic Signal OS. Protocol v0.2.7-hardened
        </div>
        <div className="flex gap-4">
          <i className="pi pi-github text-gray-500 hover:text-white cursor-pointer transition-colors text-xl"></i>
          <i className="pi pi-twitter text-gray-500 hover:text-white cursor-pointer transition-colors text-xl"></i>
        </div>
      </footer>
    </div>
  );
}
