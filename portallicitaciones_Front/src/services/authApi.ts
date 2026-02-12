// src/services/authApi.ts
import { apiFetch } from "@/services/api";

export type LoginRequest = {
  username: string;
  password: string;
};

export type LoginResponse = {
  token: string;
  username: string;
  rol: string;
  idUsuario: number;
};

/**
 * Login contra el backend.
 * Endpoint esperado: POST /api/auth/login
 * Recomendado que devuelva:
 * { token, username, rol, idUsuario }
 *
 * Aun así, normalizamos si el backend devuelve otros nombres.
 */
export async function login(payload: LoginRequest): Promise<LoginResponse> {
  const data = await apiFetch<any>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });

  const token =
    data?.token ??
    data?.accessToken ??
    data?.jwt ??
    data?.jwtToken ??
    data?.value;

  if (!token || typeof token !== "string") {
    throw new Error("Login OK pero respuesta sin token");
  }

  return {
    token,
    username: data?.username ?? payload.username,
    rol: data?.rol ?? data?.role ?? "USER",
    idUsuario: Number(data?.idUsuario ?? data?.userId ?? 0),
  };
}

/**
 * Register (si lo usas).
 * Endpoint esperado: POST /api/auth/register
 */
export async function register(payload: any): Promise<any> {
  return apiFetch<any>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
