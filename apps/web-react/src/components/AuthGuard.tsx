import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { UserRole } from '../types';

type Props = {
  allowedRoles?: UserRole[];
};

export function AuthGuard({ allowedRoles }: Props) {
  const { accessToken, role, isHydrated } = useAuthStore();
  const location = useLocation();

  // Wait for store to load from localStorage
  if (!isHydrated) {
    return null; // Or a loading spinner
  }

  if (!accessToken) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && !allowedRoles.includes(role)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
}
