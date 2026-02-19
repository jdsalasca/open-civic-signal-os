import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { UserRole } from '../types';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: string | null;
  role: UserRole;
  isLoggedIn: boolean; // P0-3: Consolidated login state
  setAuth: (data: { accessToken: string; refreshToken: string; user: string; role: string }) => void;
  logout: () => void;
  updateAccessToken: (token: string) => void;
  isHydrated: boolean;
  setHydrated: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      role: 'GUEST',
      isLoggedIn: false,
      isHydrated: false,
      setAuth: (data) => set({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        user: data.user,
        role: (data.role.replace('ROLE_', '') as UserRole),
        isLoggedIn: true
      }),
      logout: () => {
        set({ accessToken: null, refreshToken: null, user: null, role: 'GUEST', isLoggedIn: false });
        localStorage.removeItem('auth-storage');
      },
      updateAccessToken: (token) => set({ 
        accessToken: token,
        isLoggedIn: !!token 
      }),
      setHydrated: () => set({ isHydrated: true })
    }),
    {
      name: 'auth-storage',
      onRehydrateStorage: () => (state) => {
        state?.setHydrated();
      }
    }
  )
);
