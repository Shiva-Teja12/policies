"use client";

import ApprovalPortal from "@/components/ApprovalPortal";

export default function LegalReviewerDashboard() {
  return (
    <ApprovalPortal
      role="LEGAL_REVIEWER"
      title="Legal Reviewer Portal"
      description="Review policy language, legal risks and regulatory compliance."
    />
  );
}