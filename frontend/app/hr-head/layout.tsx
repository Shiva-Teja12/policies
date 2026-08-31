"use client";

import { ReactNode } from "react";
import PortalLayout from "@/components/PortalLayout";
import ProtectedRoute from "@/components/ProtectedRoute";

interface HrHeadLayoutProps {
  children: ReactNode;
}

export default function HrHeadLayout({
  children,
}: HrHeadLayoutProps) {
  return (
    <ProtectedRoute allowedRoles={["HR_HEAD"]}>
      <PortalLayout role="HR_HEAD">
        {children}
      </PortalLayout>
    </ProtectedRoute>
  );
}