import { BrowserRouter, Routes, Route, Link, Navigate } from "react-router-dom";
import { Toaster } from "react-hot-toast";
import { Dashboard } from "./views/Dashboard";
import { ReportSignal } from "./views/ReportSignal";
import { SignalDetail } from "./views/SignalDetail";
import { Register } from "./views/Register";
import { Login } from "./views/Login";
import { NotFound } from "./views/NotFound";
import { Unauthorized } from "./views/Unauthorized";
import { useAuthStore } from "./store/useAuthStore";
import { AuthGuard } from "./components/AuthGuard";
import { Button } from "primereact/button";
import { Avatar } from "primereact/avatar";

export function App() {
  const { isLoggedIn, role, logout, user } = useAuthStore();

  return (
    <BrowserRouter>
      <Toaster 
        position="top-center"
        toastOptions={{
          style: {
            background: '#111827',
            color: '#fff',
            border: '1px solid rgba(255,255,255,0.1)',
            borderRadius: '12px',
            fontSize: '14px'
          },
        }}
      />
      <div className="min-h-screen flex flex-column bg-gray-900 overflow-x-hidden">
        <nav className="surface-section px-4 md:px-6 py-3 shadow-4 flex justify-content-between align-items-center border-bottom-1 border-white-alpha-10 z-5 sticky top-0">
          <div className="flex align-items-center gap-5">
            <Link to="/" className="flex align-items-center gap-2 no-underline">
              <div className="bg-cyan-500 border-round-md flex align-items-center justify-content-center" style={{ width: '32px', height: '32px' }}>
                <i className="pi pi-signal text-gray-900 font-bold"></i>
              </div>
              <span className="text-xl font-black text-white tracking-tighter uppercase">Signal<span className="text-cyan-500">OS</span></span>
            </Link>
            
            {isLoggedIn && (
              <div className="hidden lg:flex align-items-center gap-4 ml-4">
                <Link to="/" className="text-gray-400 no-underline hover:text-white font-medium text-sm transition-colors transition-duration-200">Insights</Link>
                <Link to="/report" className="text-gray-400 no-underline hover:text-white font-medium text-sm transition-colors transition-duration-200">Public Report</Link>
              </div>
            )}
          </div>

          <div className="flex align-items-center gap-3">
            {isLoggedIn ? (
              <div className="flex align-items-center gap-3 bg-gray-800 py-1 pl-3 pr-1 border-round-right-3xl border-round-left-xl border-1 border-white-alpha-10">
                <div className="flex flex-column align-items-end mr-1">
                  <span className="text-xs font-bold text-white line-height-1 mb-1">{user}</span>
                  <span className="text-min font-bold text-cyan-500 uppercase tracking-tighter" style={{ fontSize: '9px' }}>{role}</span>
                </div>
                <Avatar label={user?.[0].toUpperCase()} shape="circle" className="bg-cyan-600 text-white font-bold" />
                <Button 
                  icon="pi pi-power-off" 
                  rounded 
                  text 
                  className="text-gray-500 hover:text-red-400"
                  onClick={logout} 
                />
              </div>
            ) : (
              <div className="flex gap-2">
                <Link to="/login" className="no-underline">
                  <Button label="Sign In" size="small" text className="text-white hover:text-cyan-400 font-bold" />
                </Link>
                <Link to="/register" className="no-underline">
                  <Button label="Join Now" size="small" className="bg-cyan-600 border-none px-3 font-bold" />
                </Link>
              </div>
            )}
          </div>
        </nav>

        <main className="flex-grow-1 p-4 md:p-6 bg-gray-950">
          <Routes>
            {/* Login is the landing if not authenticated */}
            <Route path="/login" element={isLoggedIn ? <Navigate to="/" /> : <Login />} />
            <Route path="/register" element={isLoggedIn ? <Navigate to="/" /> : <Register />} />
            
            {/* Main views protected by AuthGuard */}
            <Route element={<AuthGuard />}>
              <Route path="/" element={<Dashboard />} />
              <Route path="/report" element={<ReportSignal />} />
              <Route path="/signal/:id" element={<SignalDetail />} />
            </Route>
            
            <Route path="/unauthorized" element={<Unauthorized />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </main>

        <footer className="bg-gray-950 p-6 flex flex-column md:flex-row justify-content-between align-items-center border-top-1 border-white-alpha-10 gap-4">
          <div className="flex align-items-center gap-2">
            <i className="pi pi-globe text-gray-700"></i>
            <span className="text-gray-700 text-sm font-medium uppercase tracking-widest">Global Governance Standard</span>
          </div>
          <div className="text-gray-700 text-xs">
            &copy; 2026 Open Civic Signal OS. Protocol v0.2.4-stable
          </div>
          <div className="flex gap-4">
            <i className="pi pi-github text-gray-700 hover:text-gray-400 cursor-pointer"></i>
            <i className="pi pi-twitter text-gray-700 hover:text-gray-400 cursor-pointer"></i>
          </div>
        </footer>
      </div>
    </BrowserRouter>
  );
}
