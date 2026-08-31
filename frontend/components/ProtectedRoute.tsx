"use client";

import { useRouter } from "next/navigation";
import {
  ReactNode,
  useEffect,
  useState,
} from "react";
import {
  dashboardForRole,
  getSession,
  getToken,
  UserRole,
} from "@/lib/auth";

interface ProtectedRouteProps {
  allowedRoles: UserRole[];
  children: ReactNode;
}

export default function ProtectedRoute({
  allowedRoles,
  children,
}: ProtectedRouteProps) {
  const router = useRouter();

  const [authorized, setAuthorized] =
    useState(false);

  useEffect(() => {
    const token = getToken();
    const session = getSession();

    if (!token || !session) {
      router.replace("/login");
      return;
    }

    if (!allowedRoles.includes(session.role)) {
      router.replace(
        `/unauthorized?dashboard=${encodeURIComponent(
          dashboardForRole(session.role)
        )}`
      );

      return;
    }

    setAuthorized(true);
  }, [allowedRoles, router]);

  if (!authorized) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50">
        <div className="text-center">
          <div className="mx-auto h-10 w-10 animate-spin rounded-full border-4 border-blue-200 border-t-blue-700" />

          <p className="mt-4 text-sm text-slate-600">
            Verifying your account permissions...
          </p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}