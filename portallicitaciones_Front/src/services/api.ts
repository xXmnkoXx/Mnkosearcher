// src/infra/http/api.ts

// Si usas proxy en Vite (recomendado), pon VITE_API_BASE vacío o no lo definas,
// y llamarás a "/api/..." directamente.
// Si NO usas proxy, VITE_API_BASE="http://localhost:8080"
export const API_BASE = import.meta.env.VITE_API_BASE ?? "";

// ⚠️ Pon aquí EXACTAMENTE la key donde guardas el JWT en localStorage.
// Si en tu login haces localStorage.setItem("token", jwt) -> déjalo como "token".
const TOKEN_KEY = "token";

function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

type ApiError = {
  status: number;
  statusText: string;
  url: string;
  bodyText?: string;
};

function buildUrl(path: string) {
  // API_BASE puede ser "" o "http://localhost:8080"
  if (!API_BASE) return path; // "/api/..."
  return `${API_BASE}${path}`; // "http://localhost:8080/api/..."
}

async function readErrorBody(res: Response): Promise<string> {
  try {
    const ct = res.headers.get("content-type") ?? "";
    if (ct.includes("application/json")) {
      const j = await res.json();
      // intenta sacar mensaje típico
      if (typeof j === "string") return j;
      if (j?.message) return String(j.message);
      if (j?.error) return String(j.error);
      return JSON.stringify(j);
    }
    return await res.text();
  } catch {
    return "";
  }
}

/**
 * Wrapper fetch con:
 * - Authorization Bearer automático (si hay token)
 * - Content-Type JSON automático si hay body
 * - manejo de errores con body incluido
 */
export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();

  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string> | undefined),
  };

  // JSON por defecto si hay body y no se define Content-Type
  if (!headers["Content-Type"] && options.body != null) {
    headers["Content-Type"] = "application/json; charset=utf-8";
  }

  // Añade token si existe y no viene ya Authorization
  if (token && !headers["Authorization"]) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const url = buildUrl(path);

  const res = await fetch(url, {
    ...options,
    headers,
  });

  if (!res.ok) {
    const bodyText = await readErrorBody(res);
    const err: ApiError = {
      status: res.status,
      statusText: res.statusText,
      url,
      bodyText,
    };
    // Lanza un Error con info útil
    throw new Error(
      `HTTP ${err.status} ${err.statusText} (${err.url})${err.bodyText ? " - " + err.bodyText : ""}`
    );
  }

  // 204 No Content
  if (res.status === 204) return undefined as unknown as T;

  // Si el backend devuelve vacío (raro), evita romper
  const text = await res.text();
  if (!text) return undefined as unknown as T;

  return JSON.parse(text) as T;
}
