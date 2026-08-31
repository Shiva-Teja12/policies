import { clearSession, getToken } from "@/lib/auth";

const API_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export interface ApiEnvelope<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
}

export interface PageData<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}

function extractMessage(value: unknown): string {
  if (
    value &&
    typeof value === "object" &&
    "message" in value &&
    typeof value.message === "string"
  ) {
    return value.message;
  }

  return "Request failed.";
}

export async function api<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getToken();

  const headers = new Headers(options.headers);

  if (
    options.body &&
    !(options.body instanceof FormData) &&
    !headers.has("Content-Type")
  ) {
    headers.set("Content-Type", "application/json");
  }

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers,
    cache: "no-store",
  });

  const text = await response.text();

  let body: ApiEnvelope<T> | Record<string, unknown> | null =
    null;

  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      if (!response.ok) {
        throw new Error(text);
      }

      return text as T;
    }
  }

  if (response.status === 401) {
    clearSession();

    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }

    throw new Error("Your session has expired.");
  }

  if (response.status === 403) {
    throw new Error(
      "You do not have permission to perform this action."
    );
  }

  if (!response.ok) {
    throw new Error(extractMessage(body));
  }

  if (
    body &&
    typeof body === "object" &&
    "data" in body
  ) {
    return (body as ApiEnvelope<T>).data;
  }

  return body as T;
}

export async function downloadWithToken(
  path: string,
  filename: string
): Promise<void> {
  const token = getToken();

  if (!token) {
    clearSession();

    if (
      typeof window !==
      "undefined"
    ) {
      window.location.href =
        "/login";
    }

    throw new Error(
      "Authentication is required."
    );
  }

  const response =
    await fetch(
      `${API_URL}${path}`,
      {
        headers: {
          Authorization:
            `Bearer ${token}`,
          Accept:
            "text/csv",
        },
      }
    );

  // =========================================================
  // EXPIRED / INVALID JWT
  // =========================================================

  if (
    response.status ===
    401
  ) {
    clearSession();

    if (
      typeof window !==
      "undefined"
    ) {
      window.location.href =
        "/login";
    }

    throw new Error(
      "Your session has expired. Please login again."
    );
  }

  // =========================================================
  // FORBIDDEN
  // =========================================================

  if (
    response.status ===
    403
  ) {
    throw new Error(
      "You do not have permission to download this file."
    );
  }

  // =========================================================
  // OTHER ERRORS
  // =========================================================

  if (!response.ok) {
    const text =
      await response.text();

    if (text) {
      try {
        const body =
          JSON.parse(text);

        throw new Error(
          body.message ||
            "Download failed."
        );
      } catch (
        parseError
      ) {
        if (
          parseError instanceof Error &&
          parseError.message !==
            text
        ) {
          throw parseError;
        }
      }
    }

    throw new Error(
      text ||
        "Download failed."
    );
  }

  // =========================================================
  // DOWNLOAD FILE
  // =========================================================

  const blob =
    await response.blob();

  const url =
    URL.createObjectURL(
      blob
    );

  const anchor =
    document.createElement(
      "a"
    );

  anchor.href =
    url;

  anchor.download =
    filename;

  document.body.appendChild(
    anchor
  );

  anchor.click();

  anchor.remove();

  URL.revokeObjectURL(
    url
  );
}