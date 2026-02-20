import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { UserRole } from '../types';
import { useCommunityStore } from './useCommunityStore';

interface AuthState {
  accessToken: string | null;
  userName: string | null;
  activeRole: UserRole;
  rawRoles: string[];
  isLoggedIn: boolean;
  setAuth: (data: { accessToken: string; username: string; role: string }) => void;
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
        // Aligned with Backend AuthResponse record: accessToken, username, role
        const roles = (data.role || 'ROLE_CITIZEN')
          .split(',')
          .map(r => r.trim().replace('ROLE_', '') as UserRole);
        
        set({
          accessToken: data.accessToken,
          userName: data.username, // mapping lowercase 'username' from BE
          activeRole: roles[0],
          rawRoles: roles,
          isLoggedIn: true
        });
      },
      switchRole: (role) => set({ activeRole: role as UserRole }),
      logout: () => {
        useCommunityStore.getState().clear();
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
