"use client";

import { ReactNode } from "react";
import PortalLayout from "@/components/PortalLayout";
import ProtectedRoute from "@/components/ProtectedRoute";

interface HrAdminLayoutProps {
  children: ReactNode;
}

export default function HrAdminLayout({
  children,
}: HrAdminLayoutProps) {
  return (
    <ProtectedRoute allowedRoles={["HR_ADMIN"]}>
      <PortalLayout role="HR_ADMIN">
        {children}
      </PortalLayout>
    </ProtectedRoute>
  );
}