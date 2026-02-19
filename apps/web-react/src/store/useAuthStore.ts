import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { UserRole } from '../types';

interface AuthState {
  accessToken: string | null;
  userName: string | null;
  activeRole: UserRole; // Current active role for UI and Permissions
  rawRoles: string[];   // All authorized roles from BE
  isLoggedIn: boolean;
  setAuth: (data: { accessToken: string; username: string; roles: string }) => void;
  switchRole: (role: string) => void;
  logout: () => void;
  updateAccessToken: (token: string) => void;
  isHydrated: boolean;
  setHydrated: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      userName: null,
      activeRole: 'GUEST',
      rawRoles: [],
      isLoggedIn: false,
      isHydrated: false,
      setAuth: (data) => {
        const roles = data.roles.split(',').map(r => r.replace('ROLE_', '') as UserRole);
        set({
          accessToken: data.accessToken,
          userName: data.username,
          activeRole: roles[0], // Default to first role
          rawRoles: roles,
          isLoggedIn: true
        });
      },
      switchRole: (role) => set({ activeRole: role as UserRole }),
      logout: () => {
        set({ accessToken: null, userName: null, activeRole: 'GUEST', rawRoles: [], isLoggedIn: false });
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
