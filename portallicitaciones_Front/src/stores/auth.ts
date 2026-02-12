import { defineStore } from "pinia";

type LoginResponse = {
  token: string;
  rol?: string;   // a veces viene "rol"
  role?: string;  // a veces viene "role"
  username?: string;
};

const TOKEN_KEY = "token"; // ⚠️ CLAVE QUE USA api.ts

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || "",
    username: localStorage.getItem("username") || "",
    rol: localStorage.getItem("rol") || "", // guardamos rol si viene
  }),

  getters: {
    isAuthenticated: (s) => !!s.token,
  },

  actions: {
    setSession(payload: { token: string; username?: string; rol?: string }) {
      this.token = payload.token;
      this.username = payload.username ?? this.username;
      this.rol = payload.rol ?? this.rol;

      localStorage.setItem(TOKEN_KEY, this.token);

      if (this.username) localStorage.setItem("username", this.username);
      if (this.rol) localStorage.setItem("rol", this.rol);
    },

    clearSession() {
      this.token = "";
      this.username = "";
      this.rol = "";

      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem("username");
      localStorage.removeItem("rol");
    },

    // ✅ Login: llama a tu endpoint y guarda token
    async login(username: string, password: string) {
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json; charset=utf-8" },
        body: JSON.stringify({ username, password }),
      });

      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        throw new Error(`Login error HTTP ${res.status}: ${txt}`);
      }

      const data = (await res.json()) as LoginResponse;

      const role = (data.rol ?? data.role ?? "").toString();

      this.setSession({
        token: data.token,
        username: data.username ?? username,
        rol: role,
      });

      return data;
    },

    logout() {
      this.clearSession();
    },
  },
});
