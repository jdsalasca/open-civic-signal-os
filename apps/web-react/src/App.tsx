import { BrowserRouter, Routes, Route, Link, Navigate } from "react-router-dom";
import { Toaster } from "react-hot-toast";
import { Dashboard } from "./views/Dashboard";
import { ReportSignal } from "./views/ReportSignal";
import { SignalDetail } from "./views/SignalDetail";
import { Register } from "./views/Register";
import { Login } from "./views/Login";
import { Moderation } from "./views/Moderation";
import { NotFound } from "./views/NotFound";
import { Unauthorized } from "./views/Unauthorized";
import { useAuthStore } from "./store/useAuthStore";
import { AuthGuard } from "./components/AuthGuard";

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
    </BrowserRouter>
  );
}
