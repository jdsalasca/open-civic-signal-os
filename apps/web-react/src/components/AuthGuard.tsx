import { Navigate, Outlet } from "react-router-dom";
import { useAuthStore } from "../store/useAuthStore";
import { ProgressBar } from "primereact/progressbar";

type Props = {
  allowedRoles?: string[];
};

export function AuthGuard({ allowedRoles }: Props) {
  const { isLoggedIn, activeRole, isHydrated } = useAuthStore();

  if (!isHydrated) {
    return (
      <div className="fixed top-0 left-0 w-full z-5">
        <ProgressBar mode="indeterminate" style={{ height: '3px' }} />
      </div>
    );
  }

  if (!isLoggedIn) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(activeRole)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
}
