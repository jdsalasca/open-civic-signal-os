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

export function App() {
  const { isLoggedIn, role, logout, user } = useAuthStore();

  return (
    <BrowserRouter>
      <Toaster position="top-right" />
      <div className="min-h-screen flex flex-column bg-gray-900">
        <nav className="surface-card p-3 shadow-2 flex justify-content-between align-items-center border-bottom-1 border-gray-800">
          <div className="flex align-items-center gap-4">
            <Link to="/" className="text-2xl font-bold text-cyan-400 no-underline tracking-tight">
              SIGNAL OS
            </Link>
            <div className="hidden md:flex gap-3">
              <Link to="/" className="text-gray-300 no-underline hover:text-white font-medium">Dashboard</Link>
              <Link to="/report" className="text-gray-300 no-underline hover:text-white font-medium">Report Issue</Link>
            </div>
          </div>

          <div className="flex align-items-center gap-3">
            {isLoggedIn ? (
              <>
                <div className="flex flex-column align-items-end mr-2">
                  <span className="text-sm font-medium text-white">{user}</span>
                  <span className="text-xs text-cyan-500 font-bold">{role}</span>
                </div>
                <Button icon="pi pi-sign-out" label="Logout" size="small" outlined severity="danger" onClick={logout} />
              </>
            ) : (
              <div className="flex gap-2">
                <Link to="/login"><Button label="Login" size="small" text /></Link>
                <Link to="/register"><Button label="Register" size="small" outlined /></Link>
              </div>
            )}
          </div>
        </nav>

        <main className="flex-grow-1 p-4 md:p-6 overflow-auto">
          <Routes>
            {/* Public Routes */}
            <Route path="/login" element={isLoggedIn ? <Navigate to="/" /> : <Login />} />
            <Route path="/register" element={isLoggedIn ? <Navigate to="/" /> : <Register />} />
            <Route path="/" element={<Dashboard />} />
            <Route path="/unauthorized" element={<Unauthorized />} />
            
            {/* Protected Routes */}
            <Route element={<AuthGuard />}>
              <Route path="/report" element={<ReportSignal />} />
              <Route path="/signal/:id" element={<SignalDetail />} />
            </Route>
            
            <Route path="*" element={<NotFound />} />
          </Routes>
        </main>

        <footer className="surface-card p-3 text-center border-top-1 border-gray-800 text-gray-500 text-xs">
          &copy; 2026 Open Civic Signal OS. Empowering Communities Through Transparency.
        </footer>
      </div>
    </BrowserRouter>
  );
}
