import { Suspense, lazy } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { Toaster } from "react-hot-toast";
import { ProgressBar } from "primereact/progressbar";
import { useAuthStore } from "./store/useAuthStore";
import { AuthGuard } from "./components/AuthGuard";

// P2-16: Code-splitting for optimized bundle size and faster TTI
const Dashboard = lazy(() => import("./views/Dashboard").then(m => ({ default: m.Dashboard })));
const ReportSignal = lazy(() => import("./views/ReportSignal").then(m => ({ default: m.ReportSignal })));
const SignalDetail = lazy(() => import("./views/SignalDetail").then(m => ({ default: m.SignalDetail })));
const Register = lazy(() => import("./views/Register").then(m => ({ default: m.Register })));
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
      <Suspense fallback={<LoadingFallback />}>
        <Routes>
          <Route path="/login" element={isLoggedIn ? <Navigate to="/" /> : <Login />} />
          <Route path="/register" element={isLoggedIn ? <Navigate to="/" /> : <Register />} />
          
          <Route element={<AuthGuard />}>
            <Route path="/" element={<Dashboard />} />
            <Route path="/report" element={<ReportSignal />} />
            <Route path="/signal/:id" element={<SignalDetail />} />
          </Route>

          <Route element={<AuthGuard allowedRoles={["PUBLIC_SERVANT", "SUPER_ADMIN"]} />}>
            <Route path="/moderation" element={<Moderation />} />
          </Route>
          
          <Route path="/unauthorized" element={<Unauthorized />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
