export type UserRole =
  | "HR_ADMIN"
  | "LEGAL_REVIEWER"
  | "HR_HEAD"
  | "MANAGING_DIRECTOR"
  | "MANAGER"
  | "EMPLOYEE";

export interface UserSession {
  userId?: number;
  name?: string;
  email?: string;
  role: UserRole;
  dashboardPath?: string;
}

const dashboardPaths: Record<UserRole, string> = {
  HR_ADMIN: "/hr-admin/dashboard",
  LEGAL_REVIEWER: "/legal-reviewer/dashboard",
  HR_HEAD: "/hr-head/dashboard",
  MANAGING_DIRECTOR:
    "/managing-director/dashboard",
  MANAGER: "/manager/dashboard",
  EMPLOYEE: "/employee/dashboard",
};

export function getToken(): string {
  if (typeof window === "undefined") {
    return "";
  }

  return localStorage.getItem("token") || "";
}

export function getSession(): UserSession | null {
  if (typeof window === "undefined") {
    return null;
  }

  const rawUser = localStorage.getItem("user");

  if (rawUser) {
    try {
      const parsed = JSON.parse(rawUser);

      if (parsed?.role) {
        return parsed as UserSession;
      }
    } catch {
      localStorage.removeItem("user");
    }
  }

  const role = localStorage.getItem(
    "role"
  ) as UserRole | null;

  if (!role) {
    return null;
  }

  const userIdValue =
    localStorage.getItem("userId");

  return {
    userId: userIdValue
      ? Number(userIdValue)
      : undefined,
    name:
      localStorage.getItem("name") || undefined,
    email:
      localStorage.getItem("email") || undefined,
    role,
    dashboardPath: dashboardPaths[role],
  };
}

export function saveSession(
  token: string,
  session: UserSession
): void {
  if (typeof window === "undefined") {
    return;
  }

  localStorage.setItem("token", token);
  localStorage.setItem(
    "user",
    JSON.stringify(session)
  );

  if (session.userId !== undefined) {
    localStorage.setItem(
      "userId",
      String(session.userId)
    );
  }

  if (session.name) {
    localStorage.setItem("name", session.name);
  }

  if (session.email) {
    localStorage.setItem("email", session.email);
  }

  localStorage.setItem("role", session.role);
}

export function clearSession(): void {
  if (typeof window === "undefined") {
    return;
  }

  localStorage.removeItem("token");
  localStorage.removeItem("user");
  localStorage.removeItem("userId");
  localStorage.removeItem("name");
  localStorage.removeItem("email");
  localStorage.removeItem("role");
}

export function logout(): void {
  clearSession();

  if (typeof window !== "undefined") {
    window.location.href = "/login";
  }
}

export function dashboardForRole(
  role: UserRole
): string {
  return dashboardPaths[role];
}

export function roleLabel(role: UserRole): string {
  const labels: Record<UserRole, string> = {
    HR_ADMIN: "HR Admin",
    LEGAL_REVIEWER: "Legal Reviewer",
    HR_HEAD: "HR Head",
    MANAGING_DIRECTOR: "Managing Director",
    MANAGER: "Manager",
    EMPLOYEE: "Employee",
  };

  return labels[role];
}