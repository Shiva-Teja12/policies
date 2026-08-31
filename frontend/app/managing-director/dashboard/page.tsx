"use client";

import ApprovalPortal from "@/components/ApprovalPortal";

export default function ManagingDirectorDashboard() {
  return (
    <ApprovalPortal
      role="MANAGING_DIRECTOR"
      title="Managing Director Portal"
      description="Provide final approval for company-wide policies."
    />
  );
}