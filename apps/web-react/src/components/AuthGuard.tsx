import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuthStore } from "../store/useAuthStore";
import { ProgressBar } from "primereact/progressbar";

type Props = {
  allowedRoles?: string[];
};

export function AuthGuard({ allowedRoles }: Props) {
  const { isLoggedIn, activeRole, isHydrated } = useAuthStore();
  const location = useLocation();

  // Wait for store to rehydrate from LocalStorage before making routing decisions
  if (!isHydrated) {
    return (
      <div className="fixed top-0 left-0 w-full z-5" data-testid="auth-loading">
        <ProgressBar mode="indeterminate" style={{ height: '3px' }} />
      </div>
    );
  }

  if (!isLoggedIn) {
    // Redirect to login but save current location to return after auth
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && !allowedRoles.includes(activeRole)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
}
