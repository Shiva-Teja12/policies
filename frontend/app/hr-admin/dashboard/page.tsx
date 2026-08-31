"use client";

import Link from "next/link";
import {
  BellRing,
  CheckCircle2,
  FileCheck2,
  FilePlus2,
  FileText,
  FolderTree,
  RefreshCw,
  Rocket,
  ShieldCheck,
  Users,
} from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import ProtectedRoute from "@/components/ProtectedRoute";
import { api, PageData } from "@/lib/api";

interface Policy {
  id: number;
  name: string;
  code: string;
  categoryName: string;
  status: string;
  mandatory: boolean;
  applicability: string;
  updatedAt?: string;
}

interface Compliance {
  policyId: number;
  policyName: string;
  policyCode: string;
  totalApplicableEmployees: number;
  acknowledgedEmployees: number;
  pendingEmployees: number;
  overdueEmployees: number;
  completionPercentage: number;
}

interface DashboardData {
  totalPolicies: number;
  drafts: number;
  inReview: number;
  approved: number;
  published: number;
  retired: number;
  overallCompletion: number;
  overdueAssignments: number;
}

const emptyDashboard: DashboardData = {
  totalPolicies: 0,
  drafts: 0,
  inReview: 0,
  approved: 0,
  published: 0,
  retired: 0,
  overallCompletion: 0,
  overdueAssignments: 0,
};

async function getPolicyCount(
  status?: string
): Promise<number> {
  const params = new URLSearchParams({
    page: "0",
    size: "1",
    sortBy: "updatedAt",
    direction: "desc",
  });

  if (status) {
    params.set("status", status);
  }

  const data = await api<PageData<Policy>>(
    `/api/policies?${params.toString()}`
  );

  return data.totalElements || 0;
}

export default function HrAdminDashboard() {
  const [dashboard, setDashboard] =
    useState<DashboardData>(emptyDashboard);

  const [recentPolicies, setRecentPolicies] = useState<
    Policy[]
  >([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const [
        totalPolicies,
        drafts,
        legalReview,
        hrHeadReview,
        mdReview,
        approved,
        published,
        retired,
        recentPage,
        compliancePage,
      ] = await Promise.all([
        getPolicyCount(),
        getPolicyCount("DRAFT"),
        getPolicyCount("LEGAL_REVIEW"),
        getPolicyCount("HR_HEAD_REVIEW"),
        getPolicyCount("MD_REVIEW"),
        getPolicyCount("APPROVED"),
        getPolicyCount("PUBLISHED"),
        getPolicyCount("RETIRED"),

        api<PageData<Policy>>(
          "/api/policies?page=0&size=5&sortBy=updatedAt&direction=desc"
        ),

        api<PageData<Compliance>>(
          "/api/policies/compliance-dashboard?page=0&size=500&sortBy=policyName&direction=asc"
        ).catch(() => ({
          content: [],
          page: 0,
          size: 500,
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: true,
          hasNext: false,
          hasPrevious: false,
        })),
      ]);

      const complianceRecords =
        compliancePage.content || [];

      const totalApplicable = complianceRecords.reduce(
        (sum, record) =>
          sum + record.totalApplicableEmployees,
        0
      );

      const totalAcknowledged = complianceRecords.reduce(
        (sum, record) =>
          sum + record.acknowledgedEmployees,
        0
      );

      const overdueAssignments = complianceRecords.reduce(
        (sum, record) =>
          sum + record.overdueEmployees,
        0
      );

      const overallCompletion =
        totalApplicable === 0
          ? 0
          : (totalAcknowledged / totalApplicable) * 100;

      setDashboard({
        totalPolicies,
        drafts,
        inReview:
          legalReview + hrHeadReview + mdReview,
        approved,
        published,
        retired,
        overallCompletion,
        overdueAssignments,
      });

      setRecentPolicies(recentPage.content || []);
    } catch (requestError) {
      setDashboard(emptyDashboard);
      setRecentPolicies([]);

      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load HR Admin dashboard."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadDashboard();
  }, [loadDashboard]);

  const cards = [
    {
      label: "Total Policies",
      value: dashboard.totalPolicies,
      icon: FileText,
      href: "/hr-admin/policies",
      color: "border-blue-200 bg-blue-50 text-blue-900",
    },
    {
      label: "Draft Policies",
      value: dashboard.drafts,
      icon: FilePlus2,
      href: "/hr-admin/policies?status=DRAFT",
      color:
        "border-slate-200 bg-white text-slate-900",
    },
    {
      label: "In Approval",
      value: dashboard.inReview,
      icon: ShieldCheck,
      href: "/hr-admin/approvals",
      color:
        "border-amber-200 bg-amber-50 text-amber-900",
    },
    {
      label: "Ready to Publish",
      value: dashboard.approved,
      icon: Rocket,
      href: "/hr-admin/ready-to-publish",
      color:
        "border-green-200 bg-green-50 text-green-900",
    },
    {
      label: "Published",
      value: dashboard.published,
      icon: FileCheck2,
      href: "/hr-admin/policies?status=PUBLISHED",
      color:
        "border-purple-200 bg-purple-50 text-purple-900",
    },
    {
      label: "Overdue",
      value: dashboard.overdueAssignments,
      icon: BellRing,
      href: "/hr-admin/compliance",
      color: "border-red-200 bg-red-50 text-red-900",
    },
  ];

  return (
    <ProtectedRoute allowedRoles={["HR_ADMIN"]}>
      <main className="min-h-screen bg-slate-50">
        <header className="bg-[#082b5c] text-white">
          <div className="mx-auto flex max-w-7xl items-center justify-between gap-5 px-5 py-7">
            <div>
              <p className="text-sm text-blue-200">
                HRMS Policies Module
              </p>

              <h1 className="mt-1 text-3xl font-bold">
                HR Admin Dashboard
              </h1>

              <p className="mt-2 text-blue-100">
                Create, manage, publish and monitor policy
                compliance.
              </p>
            </div>

            <div className="flex gap-3">
              <button
                type="button"
                disabled={loading}
                onClick={() => void loadDashboard()}
                className="flex items-center gap-2 rounded-xl border border-blue-300/40 px-4 py-2.5 font-semibold hover:bg-white/10 disabled:opacity-50"
              >
                <RefreshCw
                  size={18}
                  className={loading ? "animate-spin" : ""}
                />
                Refresh
              </button>

              <Link
                href="/hr-admin/policies/create"
                className="flex items-center gap-2 rounded-xl bg-blue-500 px-4 py-2.5 font-bold hover:bg-blue-400"
              >
                <FilePlus2 size={18} />
                Create Policy
              </Link>
            </div>
          </div>
        </header>

        <div className="mx-auto max-w-7xl px-5 py-8">
          {error && (
            <div className="mb-6 rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">
              {error}
            </div>
          )}

          <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {cards.map((card) => {
              const Icon = card.icon;

              return (
                <Link
                  key={card.label}
                  href={card.href}
                  className={`rounded-2xl border p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md ${card.color}`}
                >
                  <div className="flex items-center justify-between">
                    <Icon size={25} />

                    <span className="text-3xl font-bold">
                      {loading ? "…" : card.value}
                    </span>
                  </div>

                  <p className="mt-4 font-semibold">
                    {card.label}
                  </p>
                </Link>
              );
            })}
          </section>

          <section className="mt-6 grid gap-6 lg:grid-cols-[2fr_1fr]">
            <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
              <div className="flex items-center justify-between border-b px-5 py-4">
                <div>
                  <h2 className="text-lg font-bold">
                    Recent Policies
                  </h2>

                  <p className="mt-1 text-sm text-slate-500">
                    Latest created or updated records.
                  </p>
                </div>

                <Link
                  href="/hr-admin/policies"
                  className="font-semibold text-blue-700 hover:underline"
                >
                  View all
                </Link>
              </div>

              <div className="divide-y divide-slate-100">
                {loading ? (
                  <div className="p-10 text-center text-slate-500">
                    Loading policies...
                  </div>
                ) : recentPolicies.length === 0 ? (
                  <div className="p-10 text-center text-slate-500">
                    No policies found.
                  </div>
                ) : (
                  recentPolicies.map((policy) => (
                    <Link
                      key={policy.id}
                      href={`/hr-admin/policies/${policy.id}`}
                      className="flex items-center justify-between gap-5 p-5 hover:bg-slate-50"
                    >
                      <div>
                        <p className="font-semibold text-slate-900">
                          {policy.name}
                        </p>

                        <p className="mt-1 text-sm text-slate-500">
                          {policy.code} ·{" "}
                          {policy.categoryName}
                        </p>
                      </div>

                      <span
                        className={`rounded-full px-3 py-1 text-xs font-bold ${
                          policy.status === "PUBLISHED"
                            ? "bg-blue-100 text-blue-800"
                            : policy.status === "APPROVED"
                              ? "bg-green-100 text-green-800"
                              : policy.status === "REJECTED"
                                ? "bg-red-100 text-red-800"
                                : "bg-amber-100 text-amber-800"
                        }`}
                      >
                        {policy.status.replaceAll("_", " ")}
                      </span>
                    </Link>
                  ))
                )}
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <h2 className="text-lg font-bold">
                Compliance Overview
              </h2>

              <div className="mt-6 flex justify-center">
                <div className="flex h-40 w-40 items-center justify-center rounded-full border-[14px] border-blue-100">
                  <div className="text-center">
                    <p className="text-3xl font-bold text-blue-900">
                      {dashboard.overallCompletion.toFixed(1)}%
                    </p>

                    <p className="mt-1 text-xs text-slate-500">
                      completed
                    </p>
                  </div>
                </div>
              </div>

              <Link
                href="/hr-admin/compliance"
                className="mt-6 flex items-center justify-center gap-2 rounded-xl bg-blue-700 px-4 py-3 font-bold text-white"
              >
                <Users size={18} />
                View Compliance
              </Link>
            </div>
          </section>

          <section className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Link
              href="/hr-admin/categories"
              className="flex items-center gap-3 rounded-2xl border bg-white p-5 font-semibold hover:border-blue-300"
            >
              <FolderTree className="text-blue-700" />
              Categories
            </Link>

            <Link
              href="/hr-admin/approvals"
              className="flex items-center gap-3 rounded-2xl border bg-white p-5 font-semibold hover:border-blue-300"
            >
              <ShieldCheck className="text-blue-700" />
              Approval Workflow
            </Link>

            <Link
              href="/hr-admin/reminders"
              className="flex items-center gap-3 rounded-2xl border bg-white p-5 font-semibold hover:border-blue-300"
            >
              <BellRing className="text-blue-700" />
              Reminder History
            </Link>

            <Link
              href="/notifications"
              className="flex items-center gap-3 rounded-2xl border bg-white p-5 font-semibold hover:border-blue-300"
            >
              <CheckCircle2 className="text-blue-700" />
              Notifications
            </Link>
          </section>
        </div>
      </main>
    </ProtectedRoute>
  );
}