import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import api from "@/lib/api-client";
import { setTokens, getRefreshToken } from "@/lib/auth";
import { useAuthStore, persistUser } from "@/stores/auth-store";
import { ENDPOINTS } from "@/lib/constants";
import type {
  LoginRequest,
  RegisterRequest,
  AuthResponse,
} from "@/types/api";

export function useLogin() {
  const router = useRouter();
  const setUser = useAuthStore((s) => s.setUser);

  return useMutation({
    mutationFn: async (data: LoginRequest) => {
      const res = await api.post<AuthResponse>(ENDPOINTS.auth.login, data);
      return res.data;
    },
    onSuccess: (data) => {
      setTokens(data.accessToken, data.refreshToken);
      const user = { id: data.id, email: data.email, profession: data.profession };
      setUser(user);
      persistUser(user);
      router.push("/dashboard");
    },
  });
}

export function useRegister() {
  const router = useRouter();
  const setUser = useAuthStore((s) => s.setUser);

  return useMutation({
    mutationFn: async (data: RegisterRequest) => {
      const res = await api.post<AuthResponse>(ENDPOINTS.auth.register, data);
      return res.data;
    },
    onSuccess: (data) => {
      setTokens(data.accessToken, data.refreshToken);
      const user = { id: data.id, email: data.email, profession: data.profession };
      setUser(user);
      persistUser(user);
      router.push("/dashboard");
    },
  });
}

export function useLogout() {
  const logout = useAuthStore((s) => s.logout);
  const router = useRouter();

  return useMutation({
    mutationFn: async () => {
      const refreshToken = getRefreshToken();
      if (refreshToken) {
        await api.post(ENDPOINTS.auth.logout, { refreshToken }).catch(() => {
          // Logout even if server call fails
        });
      }
    },
    onSettled: () => {
      logout();
      localStorage.removeItem("dociq_user");
      router.push("/login");
    },
  });
}
