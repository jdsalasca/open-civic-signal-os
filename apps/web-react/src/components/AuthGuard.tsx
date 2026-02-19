import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { UserRole } from '../types';

type Props = {
  allowedRoles?: UserRole[];
};

export function AuthGuard({ allowedRoles }: Props) {
  const { isLoggedIn, role } = useAuthStore();

  if (!isLoggedIn) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(role)) {
    return <Navigate to="/" replace />; // Or an unauthorized page
  }

  return <Outlet />;
}
