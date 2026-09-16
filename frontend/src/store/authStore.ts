import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

export interface User {
  id: string;
  fullName: string;
  email: string;
  phone: string;
  role: 'OWNER' | 'TENANT' | 'ADMIN';
  emailVerified?: boolean;
  phoneVerified?: boolean;
  identityVerified?: boolean;
}

interface AuthState {
  accessToken: string | null;
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  setToken: (token: string | null) => void;
  setUser: (user: User | null) => void;
  setAuthenticated: (status: boolean) => void;
  setLoading: (loading: boolean) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      user: null,
      isAuthenticated: false,
      isLoading: false,
      setToken: (accessToken) => set({ accessToken }),
      setUser: (user) => set({ user }),
      setAuthenticated: (isAuthenticated) => set({ isAuthenticated }),
      setLoading: (isLoading) => set({ isLoading }),
      logout: () => {
        try {
          localStorage.removeItem('refreshToken');
          localStorage.removeItem('roomwallah-auth');
          sessionStorage.removeItem('roomwallah-auth');
        } catch {
          // ignore storage errors
        }
        set({ accessToken: null, user: null, isAuthenticated: false, isLoading: false });
      },
    }),
    {
      name: 'roomwallah-auth',
      storage: createJSONStorage(() => localStorage),
    }
  )
);
