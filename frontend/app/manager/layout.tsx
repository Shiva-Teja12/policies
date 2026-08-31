"use client";

import { ReactNode } from "react";
import PortalLayout from "@/components/PortalLayout";
import ProtectedRoute from "@/components/ProtectedRoute";

interface ManagerLayoutProps {
  children: ReactNode;
}

export default function ManagerLayout({
  children,
}: ManagerLayoutProps) {
  return (
    <ProtectedRoute allowedRoles={["MANAGER"]}>
      <PortalLayout role="MANAGER">
        {children}
      </PortalLayout>
    </ProtectedRoute>
  );
}