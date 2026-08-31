"use client";

import { ReactNode } from "react";
import PortalLayout from "@/components/PortalLayout";
import ProtectedRoute from "@/components/ProtectedRoute";

interface LegalReviewerLayoutProps {
  children: ReactNode;
}

export default function LegalReviewerLayout({
  children,
}: LegalReviewerLayoutProps) {
  return (
    <ProtectedRoute
      allowedRoles={["LEGAL_REVIEWER"]}
    >
      <PortalLayout role="LEGAL_REVIEWER">
        {children}
      </PortalLayout>
    </ProtectedRoute>
  );
}