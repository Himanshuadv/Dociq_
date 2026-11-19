import { create } from "zustand";
import type { Profession } from "@/types/api";
import { getAccessToken, clearTokens } from "@/lib/auth";

interface AuthState {
  isAuthenticated: boolean;
  user: {
    id: string;
    email: string;
    profession: Profession;
  } | null;

  setUser: (user: AuthState["user"]) => void;
  logout: () => void;
  hydrate: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  isAuthenticated: false,
  user: null,

  setUser: (user) =>
    set({ user, isAuthenticated: true }),

  logout: () => {
    clearTokens();
    set({ user: null, isAuthenticated: false });
  },

  // Call on app mount to check if tokens exist
  hydrate: () => {
    const token = getAccessToken();
    if (token) {
      // We have a token — try to decode the user from localStorage
      try {
        const stored = localStorage.getItem("dociq_user");
        if (stored) {
          set({ user: JSON.parse(stored), isAuthenticated: true });
        }
      } catch {
        // Corrupted, clear everything
        clearTokens();
        localStorage.removeItem("dociq_user");
      }
    }
  },
}));

// Helper to persist user alongside tokens
export function persistUser(user: AuthState["user"]) {
  if (user) {
    localStorage.setItem("dociq_user", JSON.stringify(user));
  }
}
