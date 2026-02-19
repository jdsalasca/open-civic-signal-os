import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { UserRole } from '../types';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: string | null;
  role: UserRole;
  isLoggedIn: boolean;
  setAuth: (data: { accessToken: string; refreshToken: string; user: string; role: string }) => void;
  logout: () => void;
  updateAccessToken: (token: string) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      role: 'GUEST',
      isLoggedIn: false,
      setAuth: (data) => set({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        user: data.user,
        role: (data.role.replace('ROLE_', '') as UserRole),
        isLoggedIn: true
      }),
      logout: () => {
        localStorage.removeItem('auth-storage');
        set({ accessToken: null, refreshToken: null, user: null, role: 'GUEST', isLoggedIn: false });
      },
      updateAccessToken: (token) => set({ accessToken: token })
    }),
    {
      name: 'auth-storage',
    }
  )
);
