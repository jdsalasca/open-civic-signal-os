import { Suspense, lazy, useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { Toaster } from "react-hot-toast";
import { ProgressBar } from "primereact/progressbar";
import { useAuthStore } from "./store/useAuthStore";
import { useSettingsStore } from "./store/useSettingsStore";
import { AuthGuard } from "./components/AuthGuard";
import { ChunkErrorBoundary } from "./components/ChunkErrorBoundary";

// P2-16: Code-splitting
const Dashboard = lazy(() => import("./views/Dashboard").then(m => ({ default: m.Dashboard })));
const ReportSignal = lazy(() => import("./views/ReportSignal").then(m => ({ default: m.ReportSignal })));
const SignalDetail = lazy(() => import("./views/SignalDetail").then(m => ({ default: m.SignalDetail })));
const MySignals = lazy(() => import("./views/MySignals").then(m => ({ default: m.MySignals })));
const Settings = lazy(() => import("./views/Settings").then(m => ({ default: m.Settings })));
const Register = lazy(() => import("./views/Register").then(m => ({ default: m.Register })));
const Verify = lazy(() => import("./views/Verify").then(m => ({ default: m.Verify })));
const Login = lazy(() => import("./views/Login").then(m => ({ default: m.Login })));
const Moderation = lazy(() => import("./views/Moderation").then(m => ({ default: m.Moderation })));
const NotFound = lazy(() => import("./views/NotFound").then(m => ({ default: m.NotFound })));
const Unauthorized = lazy(() => import("./views/Unauthorized").then(m => ({ default: m.Unauthorized })));

const LoadingFallback = () => (
  <div className="fixed top-0 left-0 w-full z-5">
    <ProgressBar mode="indeterminate" style={{ height: '3px' }} />
  </div>
);

export function App() {
  const { isLoggedIn } = useAuthStore();
  const { theme } = useSettingsStore();

  useEffect(() => {
    const html = document.documentElement;
    if (theme === 'dark') {
      html.classList.add('dark-theme');
      html.classList.remove('light-theme');
    } else {
      html.classList.add('light-theme');
      html.classList.remove('dark-theme');
    }
  }, [theme]);

  return (
    <BrowserRouter>
      <Toaster 
        position="top-center"
        toastOptions={{
          style: {
            background: theme === 'dark' ? '#111827' : '#fff',
            color: theme === 'dark' ? '#fff' : '#111827',
            border: '1px solid rgba(255,255,255,0.1)',
            borderRadius: '12px',
            fontSize: '14px'
          },
        }}
      />
      <ChunkErrorBoundary>
        <Suspense fallback={<LoadingFallback />}>
          <Routes>
            <Route path="/login" element={isLoggedIn ? <Navigate to="/" /> : <Login />} />
            <Route path="/register" element={isLoggedIn ? <Navigate to="/" /> : <Register />} />
            <Route path="/verify" element={<Verify />} />
            
            <Route element={<AuthGuard />}>
              <Route path="/" element={<Dashboard />} />
              <Route path="/report" element={<ReportSignal />} />
              <Route path="/signal/:id" element={<SignalDetail />} />
              <Route path="/mine" element={<MySignals />} />
              <Route path="/settings" element={<Settings />} />
            </Route>

            <Route element={<AuthGuard allowedRoles={["PUBLIC_SERVANT", "SUPER_ADMIN"]} />}>
              <Route path="/moderation" element={<Moderation />} />
            </Route>
            
            <Route path="/unauthorized" element={<Unauthorized />} />
            <Route path="/forbidden" element={<Unauthorized />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </Suspense>
      </ChunkErrorBoundary>
    </BrowserRouter>
  );
}
