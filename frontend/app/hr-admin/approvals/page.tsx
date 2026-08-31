"use client";

import Link from "next/link";
import {
  ArrowDown,
  ArrowLeft,
  ArrowUp,
  CheckCircle2,
  Clock3,
  RefreshCw,
  Search,
  Send,
  XCircle,
} from "lucide-react";
import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";
import ProtectedRoute from "@/components/ProtectedRoute";
import { api, PageData } from "@/lib/api";

interface Policy {
  id: number;
  name: string;
  code: string;
  categoryId: number;
  categoryName: string;
  categoryCode: string;
  applicability:
    | "ALL"
    | "GRADE_BASED"
    | "DEPT_BASED";
  mandatory: boolean;
  status:
    | "DRAFT"
    | "LEGAL_REVIEW"
    | "HR_HEAD_REVIEW"
    | "MD_REVIEW"
    | "APPROVED"
    | "PUBLISHED"
    | "REJECTED"
    | "RETIRED";
  updatedAt?: string;
}

interface Approval {
  approvalId: number;
  policyId: number;
  policyCode: string;
  policyName: string;
  policyStatus: string;
  stage: string;
  decision: string;
  sequenceNumber: number;
  approverId?: number;
  approverName?: string;
  approverEmail?: string;
  comments?: string;
  submittedAt?: string;
  decidedAt?: string;
}

const PAGE_SIZE = 10;

const WORKFLOW_STATUSES = [
  "LEGAL_REVIEW",
  "HR_HEAD_REVIEW",
  "MD_REVIEW",
  "APPROVED",
  "REJECTED",
];

function label(value: string): string {
  return value
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (letter) =>
      letter.toUpperCase()
    );
}

function formatDate(value?: string): string {
  if (!value) {
    return "—";
  }

  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString();
}

function statusClass(status: string): string {
  switch (status) {
    case "LEGAL_REVIEW":
      return "bg-blue-100 text-blue-800";

    case "HR_HEAD_REVIEW":
      return "bg-amber-100 text-amber-800";

    case "MD_REVIEW":
      return "bg-purple-100 text-purple-800";

    case "APPROVED":
      return "bg-green-100 text-green-800";

    case "REJECTED":
      return "bg-red-100 text-red-800";

    default:
      return "bg-slate-100 text-slate-700";
  }
}

function currentApprover(status: string): string {
  switch (status) {
    case "LEGAL_REVIEW":
      return "Legal Reviewer";

    case "HR_HEAD_REVIEW":
      return "HR Head";

    case "MD_REVIEW":
      return "Managing Director";

    case "APPROVED":
      return "Approval completed";

    case "REJECTED":
      return "Returned to HR Admin";

    default:
      return "—";
  }
}

function WorkflowProgress({
  status,
  applicability,
}: {
  status: Policy["status"];
  applicability: Policy["applicability"];
}) {
  if (status === "REJECTED") {
    return (
      <span className="flex items-center gap-2 font-semibold text-red-700">
        <XCircle size={17} />
        Rejected
      </span>
    );
  }

  const companyWide = applicability === "ALL";

  const stages = [
    {
      name: "Legal",
      active: status === "LEGAL_REVIEW",
      complete: [
        "HR_HEAD_REVIEW",
        "MD_REVIEW",
        "APPROVED",
      ].includes(status),
    },
    {
      name: "HR Head",
      active: status === "HR_HEAD_REVIEW",
      complete: [
        "MD_REVIEW",
        "APPROVED",
      ].includes(status),
    },
    ...(companyWide
      ? [
          {
            name: "MD",
            active: status === "MD_REVIEW",
            complete: status === "APPROVED",
          },
        ]
      : []),
  ];

  return (
    <div className="flex min-w-[260px] items-start">
      {stages.map((stage, index) => (
        <div
          key={stage.name}
          className="flex flex-1 items-center"
        >
          <div className="flex flex-col items-center">
            <span
              className={`flex h-8 w-8 items-center justify-center rounded-full text-xs font-bold ${
                stage.complete
                  ? "bg-green-600 text-white"
                  : stage.active
                    ? "bg-blue-700 text-white"
                    : "bg-slate-200 text-slate-500"
              }`}
            >
              {stage.complete ? "✓" : index + 1}
            </span>

            <span className="mt-1 whitespace-nowrap text-[11px] text-slate-600">
              {stage.name}
            </span>
          </div>

          {index < stages.length - 1 && (
            <span
              className={`mx-1 mb-5 h-1 flex-1 rounded ${
                stage.complete
                  ? "bg-green-500"
                  : "bg-slate-200"
              }`}
            />
          )}
        </div>
      ))}
    </div>
  );
}

export default function HrAdminApprovalsPage() {
  const [policies, setPolicies] = useState<Policy[]>([]);
  const [history, setHistory] = useState<
    Record<number, Approval[]>
  >({});

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] =
    useState("");
  const [applicabilityFilter, setApplicabilityFilter] =
    useState("");

  const [sortBy, setSortBy] =
    useState<"name" | "status" | "updatedAt">(
      "updatedAt"
    );

  const [direction, setDirection] =
    useState<"asc" | "desc">("desc");

  const [page, setPage] = useState(0);
  const [selectedPolicy, setSelectedPolicy] =
    useState<Policy | null>(null);

  const [loading, setLoading] = useState(true);
  const [historyLoading, setHistoryLoading] =
    useState(false);

  const [actionId, setActionId] = useState<
    number | null
  >(null);

  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadApprovals = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const data = await api<PageData<Policy>>(
        "/api/policies?page=0&size=500&sortBy=updatedAt&direction=desc"
      );

      setPolicies(
        (data.content || []).filter((policy) =>
          WORKFLOW_STATUSES.includes(policy.status)
        )
      );
    } catch (requestError) {
      setPolicies([]);

      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load approval workflow."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadApprovals();
  }, [loadApprovals]);

  const filteredPolicies = useMemo(() => {
    const query = search.trim().toLowerCase();

    return [...policies]
      .filter((policy) => {
        const matchesSearch =
          !query ||
          policy.name.toLowerCase().includes(query) ||
          policy.code.toLowerCase().includes(query) ||
          policy.categoryName
            .toLowerCase()
            .includes(query);

        const matchesStatus =
          !statusFilter ||
          policy.status === statusFilter;

        const matchesApplicability =
          !applicabilityFilter ||
          policy.applicability ===
            applicabilityFilter;

        return (
          matchesSearch &&
          matchesStatus &&
          matchesApplicability
        );
      })
      .sort((first, second) => {
        const firstValue = String(
          first[sortBy] || ""
        ).toLowerCase();

        const secondValue = String(
          second[sortBy] || ""
        ).toLowerCase();

        const comparison =
          firstValue.localeCompare(secondValue);

        return direction === "asc"
          ? comparison
          : -comparison;
      });
  }, [
    applicabilityFilter,
    direction,
    policies,
    search,
    sortBy,
    statusFilter,
  ]);

  const totalPages = Math.ceil(
    filteredPolicies.length / PAGE_SIZE
  );

  const displayedPolicies = filteredPolicies.slice(
    page * PAGE_SIZE,
    page * PAGE_SIZE + PAGE_SIZE
  );

  const pendingCount = policies.filter((policy) =>
    [
      "LEGAL_REVIEW",
      "HR_HEAD_REVIEW",
      "MD_REVIEW",
    ].includes(policy.status)
  ).length;

  const approvedCount = policies.filter(
    (policy) => policy.status === "APPROVED"
  ).length;

  const rejectedCount = policies.filter(
    (policy) => policy.status === "REJECTED"
  ).length;

  function changeSort(
    field: "name" | "status" | "updatedAt"
  ) {
    setPage(0);

    if (sortBy === field) {
      setDirection((current) =>
        current === "asc" ? "desc" : "asc"
      );
    } else {
      setSortBy(field);
      setDirection("asc");
    }
  }

  function resetFilters() {
    setSearch("");
    setStatusFilter("");
    setApplicabilityFilter("");
    setSortBy("updatedAt");
    setDirection("desc");
    setPage(0);
  }

  async function showHistory(policy: Policy) {
    setSelectedPolicy(policy);
    setHistoryLoading(true);
    setError("");

    try {
      const data = await api<Approval[]>(
        `/api/policies/${policy.id}/approval-history`
      );

      setHistory((current) => ({
        ...current,
        [policy.id]: data || [],
      }));
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load approval history."
      );
    } finally {
      setHistoryLoading(false);
    }
  }

  async function resubmitPolicy(policyId: number) {
    const confirmed = window.confirm(
      "Resubmit this rejected policy to the Legal Reviewer?"
    );

    if (!confirmed) {
      return;
    }

    setActionId(policyId);
    setError("");
    setMessage("");

    try {
      await api(
        `/api/policies/${policyId}/submit-for-review`,
        {
          method: "PATCH",
        }
      );

      setMessage(
        "Policy resubmitted to the Legal Reviewer."
      );

      await loadApprovals();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to resubmit policy."
      );
    } finally {
      setActionId(null);
    }
  }

  return (
    <ProtectedRoute allowedRoles={["HR_ADMIN"]}>
      <main className="min-h-screen bg-slate-50">
        <header className="bg-[#082b5c] text-white">
          <div className="mx-auto flex max-w-7xl items-center gap-4 px-5 py-6">
            <Link
              href="/hr-admin/dashboard"
              className="rounded-lg border border-blue-300/40 p-2 hover:bg-white/10"
              aria-label="Back to dashboard"
            >
              <ArrowLeft size={20} />
            </Link>

            <div>
              <p className="text-sm text-blue-200">
                HR Admin
              </p>

              <h1 className="text-2xl font-bold">
                Approval Workflow
              </h1>

              <p className="mt-1 text-sm text-blue-100">
                Monitor Legal, HR Head and Managing
                Director decisions.
              </p>
            </div>
          </div>
        </header>

        <div className="mx-auto max-w-7xl px-5 py-8">
          {message && (
            <div className="mb-5 rounded-xl border border-green-200 bg-green-50 p-4 text-green-800">
              {message}
            </div>
          )}

          {error && !selectedPolicy && (
            <div className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">
              {error}
            </div>
          )}

          <section className="grid gap-4 sm:grid-cols-3">
            <div className="rounded-2xl border border-blue-200 bg-blue-50 p-5">
              <Clock3 className="text-blue-700" />

              <p className="mt-4 text-3xl font-bold text-blue-950">
                {pendingCount}
              </p>

              <p className="text-sm font-semibold text-blue-700">
                Pending approval
              </p>
            </div>

            <div className="rounded-2xl border border-green-200 bg-green-50 p-5">
              <CheckCircle2 className="text-green-700" />

              <p className="mt-4 text-3xl font-bold text-green-950">
                {approvedCount}
              </p>

              <p className="text-sm font-semibold text-green-700">
                Approved
              </p>
            </div>

            <div className="rounded-2xl border border-red-200 bg-red-50 p-5">
              <XCircle className="text-red-700" />

              <p className="mt-4 text-3xl font-bold text-red-950">
                {rejectedCount}
              </p>

              <p className="text-sm font-semibold text-red-700">
                Rejected
              </p>
            </div>
          </section>

          <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="grid gap-4 lg:grid-cols-[2fr_1fr_1fr_1fr_auto]">
              <div className="relative">
                <Search
                  size={18}
                  className="absolute left-4 top-3.5 text-slate-400"
                />

                <input
                  value={search}
                  onChange={(event) => {
                    setSearch(event.target.value);
                    setPage(0);
                  }}
                  placeholder="Search name, code or category"
                  className="w-full rounded-xl border border-slate-300 py-3 pl-11 pr-4"
                />
              </div>

              <select
                value={statusFilter}
                onChange={(event) => {
                  setStatusFilter(event.target.value);
                  setPage(0);
                }}
                className="rounded-xl border border-slate-300 px-3 py-3"
              >
                <option value="">All stages</option>
                <option value="LEGAL_REVIEW">
                  Legal Review
                </option>
                <option value="HR_HEAD_REVIEW">
                  HR Head Review
                </option>
                <option value="MD_REVIEW">
                  MD Review
                </option>
                <option value="APPROVED">
                  Approved
                </option>
                <option value="REJECTED">
                  Rejected
                </option>
              </select>

              <select
                value={applicabilityFilter}
                onChange={(event) => {
                  setApplicabilityFilter(
                    event.target.value
                  );
                  setPage(0);
                }}
                className="rounded-xl border border-slate-300 px-3 py-3"
              >
                <option value="">All applicability</option>
                <option value="ALL">
                  All employees
                </option>
                <option value="DEPT_BASED">
                  Department based
                </option>
                <option value="GRADE_BASED">
                  Grade based
                </option>
              </select>

              <select
                value={`${sortBy},${direction}`}
                onChange={(event) => {
                  const [field, order] =
                    event.target.value.split(",");

                  setSortBy(
                    field as
                      | "name"
                      | "status"
                      | "updatedAt"
                  );

                  setDirection(
                    order as "asc" | "desc"
                  );

                  setPage(0);
                }}
                className="rounded-xl border border-slate-300 px-3 py-3"
              >
                <option value="updatedAt,desc">
                  Recently updated
                </option>
                <option value="name,asc">
                  Name A–Z
                </option>
                <option value="name,desc">
                  Name Z–A
                </option>
                <option value="status,asc">
                  Status A–Z
                </option>
              </select>

              <button
                type="button"
                onClick={resetFilters}
                className="flex items-center justify-center gap-2 rounded-xl border px-4 py-3 font-semibold"
              >
                <RefreshCw size={17} />
                Reset
              </button>
            </div>
          </section>

          <section className="mt-6 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[1150px] text-left">
                <thead className="bg-slate-50 text-sm">
                  <tr>
                    <th className="px-5 py-3">
                      <button
                        type="button"
                        onClick={() =>
                          changeSort("name")
                        }
                        className="flex items-center gap-1 font-semibold"
                      >
                        Policy

                        {sortBy === "name" &&
                          (direction === "asc" ? (
                            <ArrowUp size={14} />
                          ) : (
                            <ArrowDown size={14} />
                          ))}
                      </button>
                    </th>

                    <th className="px-5 py-3">
                      Category
                    </th>

                    <th className="px-5 py-3">
                      Applicability
                    </th>

                    <th className="px-5 py-3">
                      Current stage
                    </th>

                    <th className="px-5 py-3">
                      Current approver
                    </th>

                    <th className="px-5 py-3">
                      Workflow
                    </th>

                    <th className="px-5 py-3 text-right">
                      Actions
                    </th>
                  </tr>
                </thead>

                <tbody className="divide-y">
                  {loading ? (
                    <tr>
                      <td
                        colSpan={7}
                        className="p-12 text-center text-slate-500"
                      >
                        Loading approval workflow...
                      </td>
                    </tr>
                  ) : displayedPolicies.length === 0 ? (
                    <tr>
                      <td
                        colSpan={7}
                        className="p-12 text-center text-slate-500"
                      >
                        No approval records found.
                      </td>
                    </tr>
                  ) : (
                    displayedPolicies.map((policy) => (
                      <tr
                        key={policy.id}
                        className="hover:bg-slate-50"
                      >
                        <td className="px-5 py-4">
                          <p className="font-semibold">
                            {policy.name}
                          </p>

                          <p className="mt-1 text-sm text-slate-500">
                            {policy.code}
                          </p>
                        </td>

                        <td className="px-5 py-4">
                          {policy.categoryName}
                        </td>

                        <td className="px-5 py-4 text-sm">
                          {label(policy.applicability)}
                        </td>

                        <td className="px-5 py-4">
                          <span
                            className={`rounded-full px-3 py-1 text-xs font-bold ${statusClass(
                              policy.status
                            )}`}
                          >
                            {label(policy.status)}
                          </span>
                        </td>

                        <td className="px-5 py-4 font-semibold text-slate-700">
                          {currentApprover(
                            policy.status
                          )}
                        </td>

                        <td className="px-5 py-4">
                          <WorkflowProgress
                            status={policy.status}
                            applicability={
                              policy.applicability
                            }
                          />
                        </td>

                        <td className="px-5 py-4">
                          <div className="flex justify-end gap-2">
                            <button
                              type="button"
                              onClick={() =>
                                showHistory(policy)
                              }
                              className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold"
                            >
                              History
                            </button>

                            {policy.status ===
                              "REJECTED" && (
                              <button
                                type="button"
                                disabled={
                                  actionId === policy.id
                                }
                                onClick={() =>
                                  resubmitPolicy(policy.id)
                                }
                                className="flex items-center gap-2 rounded-lg bg-blue-700 px-3 py-2 text-sm font-bold text-white disabled:bg-blue-400"
                              >
                                <Send size={16} />

                                {actionId === policy.id
                                  ? "Submitting..."
                                  : "Resubmit"}
                              </button>
                            )}

                            {policy.status ===
                              "APPROVED" && (
                              <Link
                                href={`/hr-admin/ready-to-publish?policyId=${policy.id}`}
                                className="rounded-lg bg-green-700 px-3 py-2 text-sm font-bold text-white"
                              >
                                Publish
                              </Link>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            <div className="flex items-center justify-between border-t p-4">
              <span className="text-sm text-slate-500">
                Page {totalPages === 0 ? 0 : page + 1} of{" "}
                {totalPages}
              </span>

              <div className="flex gap-2">
                <button
                  type="button"
                  disabled={page === 0}
                  onClick={() =>
                    setPage((current) =>
                      Math.max(0, current - 1)
                    )
                  }
                  className="rounded-lg border px-4 py-2 text-sm font-semibold disabled:opacity-40"
                >
                  Previous
                </button>

                <button
                  type="button"
                  disabled={
                    totalPages === 0 ||
                    page + 1 >= totalPages
                  }
                  onClick={() =>
                    setPage((current) => current + 1)
                  }
                  className="rounded-lg border px-4 py-2 text-sm font-semibold disabled:opacity-40"
                >
                  Next
                </button>
              </div>
            </div>
          </section>
        </div>

        {selectedPolicy && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
            <div className="max-h-[85vh] w-full max-w-3xl overflow-y-auto rounded-2xl bg-white shadow-2xl">
              <div className="sticky top-0 flex items-start justify-between border-b bg-white p-6">
                <div>
                  <h2 className="text-xl font-bold">
                    Approval History
                  </h2>

                  <p className="mt-1 text-sm text-slate-500">
                    {selectedPolicy.name} (
                    {selectedPolicy.code})
                  </p>
                </div>

                <button
                  type="button"
                  onClick={() =>
                    setSelectedPolicy(null)
                  }
                  className="rounded-lg p-2 hover:bg-slate-100"
                >
                  <XCircle size={20} />
                </button>
              </div>

              <div className="p-6">
                {error && (
                  <div className="mb-5 rounded-xl bg-red-50 p-4 text-red-700">
                    {error}
                  </div>
                )}

                {historyLoading ? (
                  <p className="py-10 text-center text-slate-500">
                    Loading approval history...
                  </p>
                ) : (
                  <div className="space-y-4">
                    {(history[selectedPolicy.id] || [])
                      .length === 0 ? (
                      <p className="py-10 text-center text-slate-500">
                        No approval history found.
                      </p>
                    ) : (
                      (
                        history[selectedPolicy.id] || []
                      ).map((approval) => (
                        <article
                          key={approval.approvalId}
                          className="rounded-xl border p-4"
                        >
                          <div className="flex flex-wrap items-center justify-between gap-3">
                            <div>
                              <p className="font-bold">
                                {label(
                                  approval.stage
                                )}
                              </p>

                              <p className="mt-1 text-sm text-slate-500">
                                {approval.approverName ||
                                  approval.approverEmail ||
                                  "Approver not assigned"}
                              </p>
                            </div>

                            <span
                              className={`rounded-full px-3 py-1 text-xs font-bold ${
                                approval.decision ===
                                "APPROVED"
                                  ? "bg-green-100 text-green-800"
                                  : approval.decision ===
                                      "REJECTED"
                                    ? "bg-red-100 text-red-800"
                                    : "bg-amber-100 text-amber-800"
                              }`}
                            >
                              {approval.decision}
                            </span>
                          </div>

                          {approval.comments && (
                            <p className="mt-3 rounded-lg bg-slate-50 p-3 text-sm text-slate-700">
                              {approval.comments}
                            </p>
                          )}

                          <div className="mt-3 flex flex-wrap gap-4 text-xs text-slate-500">
                            <span>
                              Submitted:{" "}
                              {formatDate(
                                approval.submittedAt
                              )}
                            </span>

                            <span>
                              Decided:{" "}
                              {formatDate(
                                approval.decidedAt
                              )}
                            </span>
                          </div>
                        </article>
                      ))
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </main>
    </ProtectedRoute>
  );
}