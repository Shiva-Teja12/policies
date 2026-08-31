"use client";

import { ReactNode } from "react";
import PortalLayout from "@/components/PortalLayout";
import ProtectedRoute from "@/components/ProtectedRoute";

interface ManagingDirectorLayoutProps {
  children: ReactNode;
}

export default function ManagingDirectorLayout({
  children,
}: ManagingDirectorLayoutProps) {
  return (
    <ProtectedRoute
      allowedRoles={["MANAGING_DIRECTOR"]}
    >
      <PortalLayout role="MANAGING_DIRECTOR">
        {children}
      </PortalLayout>
    </ProtectedRoute>
  );
}