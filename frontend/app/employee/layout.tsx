"use client";

import { ReactNode } from "react";
import PortalLayout from "@/components/PortalLayout";
import ProtectedRoute from "@/components/ProtectedRoute";

interface EmployeeLayoutProps {
  children: ReactNode;
}

export default function EmployeeLayout({
  children,
}: EmployeeLayoutProps) {
  return (
    <ProtectedRoute allowedRoles={["EMPLOYEE"]}>
      <PortalLayout role="EMPLOYEE">
        {children}
      </PortalLayout>
    </ProtectedRoute>
  );
}