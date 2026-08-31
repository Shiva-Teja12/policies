"use client";

import {
  ReactNode,
  useEffect,
  useState,
} from "react";
import PortalLayout from "@/components/PortalLayout";
import ProtectedRoute from "@/components/ProtectedRoute";
import {
  getSession,
  UserRole,
} from "@/lib/auth";

interface NotificationsLayoutProps {
  children: ReactNode;
}

const allowedRoles: UserRole[] = [
  "HR_ADMIN",
  "LEGAL_REVIEWER",
  "HR_HEAD",
  "MANAGING_DIRECTOR",
  "MANAGER",
  "EMPLOYEE",
];

export default function NotificationsLayout({
  children,
}: NotificationsLayoutProps) {
  const [role, setRole] =
    useState<UserRole | null>(null);

  useEffect(() => {
    const session = getSession();

    if (session) {
      setRole(session.role);
    }
  }, []);

  if (!role) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50">
        <div className="text-center">
          <div className="mx-auto h-10 w-10 animate-spin rounded-full border-4 border-blue-200 border-t-blue-700" />

          <p className="mt-4 text-sm text-slate-600">
            Loading your portal...
          </p>
        </div>
      </div>
    );
  }

  return (
    <ProtectedRoute allowedRoles={allowedRoles}>
      <PortalLayout role={role}>
        {children}
      </PortalLayout>
    </ProtectedRoute>
  );
}