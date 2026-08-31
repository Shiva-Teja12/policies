"use client";

import {
  AlertCircle,
  CheckCircle2,
  Clock3,
  Eye,
  FileText,
  RefreshCw,
  Search,
  X,
  XCircle,
} from "lucide-react";
import {
  FormEvent,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import ProtectedRoute from "@/components/ProtectedRoute";
import { api, PageData } from "@/lib/api";
import {
  getSession,
  roleLabel,
  UserRole,
} from "@/lib/auth";

interface ApprovalPortalProps {
  role?: UserRole;
  allowedRole?: UserRole;
  requiredRole?: UserRole;
  title?: string;
  description?: string;
  subtitle?: string;

  /*
   * Keeps this component compatible with dashboard pages
   * that may pass additional display properties.
   */
  [key: string]: unknown;
}

interface Approval {
  approvalId: number;
  policyId: number;
  policyCode: string;
  policyName: string;
  policyStatus: string;
  stage:
    | "LEGAL_REVIEW"
    | "HR_HEAD_REVIEW"
    | "MD_REVIEW";
  decision:
    | "PENDING"
    | "APPROVED"
    | "REJECTED";
  sequenceNumber: number;
  approverId?: number | null;
  approverName?: string | null;
  approverEmail?: string | null;
  comments?: string | null;
  submittedAt?: string | null;
  decidedAt?: string | null;
}

interface Policy {
  id: number;
  name: string;
  code: string;
  categoryId: number;
  categoryName: string;
  categoryCode: string;
  content: string;
  applicability:
    | "ALL"
    | "DEPT_BASED"
    | "GRADE_BASED";
  applicableDepartments: string[];
  applicableGrades: string[];
  mandatory: boolean;
  status: string;
  acknowledgementPeriodDays: number;
  onboardingPeriodDays: number;
  publishedOnce: boolean;
  effectiveDate?: string | null;
  createdById?: number | null;
  createdByName?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

type DecisionAction = "APPROVE" | "REJECT";

const PAGE_SIZE = 10;

const REVIEWER_ROLES: UserRole[] = [
  "LEGAL_REVIEWER",
  "HR_HEAD",
  "MANAGING_DIRECTOR",
];

function displayLabel(value?: string | null) {
  if (!value) {
    return "—";
  }

  return value
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (character) =>
      character.toUpperCase()
    );
}

function formatDate(value?: string | null) {
  if (!value) {
    return "—";
  }

  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString();
}

function defaultTitle(role: UserRole) {
  switch (role) {
    case "LEGAL_REVIEWER":
      return "Legal Reviewer Portal";

    case "HR_HEAD":
      return "HR Head Portal";

    case "MANAGING_DIRECTOR":
      return "Managing Director Portal";

    default:
      return "Policy Approval Portal";
  }
}

function defaultDescription(role: UserRole) {
  switch (role) {
    case "LEGAL_REVIEWER":
      return (
        "Review policy language, legal risks and " +
        "regulatory compliance."
      );

    case "HR_HEAD":
      return (
        "Review HR impact, applicability and " +
        "organisational requirements."
      );

    case "MANAGING_DIRECTOR":
      return (
        "Perform final approval for company-wide policies."
      );

    default:
      return "Review pending company policies.";
  }
}

export default function ApprovalPortal(
  props: ApprovalPortalProps
) {
  const session = getSession();

  const requestedRole =
    props.role ||
    props.allowedRole ||
    props.requiredRole ||
    session?.role ||
    "LEGAL_REVIEWER";

  const portalRole = REVIEWER_ROLES.includes(
    requestedRole
  )
    ? requestedRole
    : "LEGAL_REVIEWER";

  const portalTitle =
    props.title || defaultTitle(portalRole);

  const portalDescription =
    props.description ||
    props.subtitle ||
    defaultDescription(portalRole);

  const [approvals, setApprovals] = useState<
    Approval[]
  >([]);

  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");

  const [direction, setDirection] =
    useState<"asc" | "desc">("desc");

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] =
    useState(0);

  const [selectedApproval, setSelectedApproval] =
    useState<Approval | null>(null);

  const [selectedPolicy, setSelectedPolicy] =
    useState<Policy | null>(null);

  const [approvalHistory, setApprovalHistory] =
    useState<Approval[]>([]);

  const [comments, setComments] = useState("");

  const [loading, setLoading] = useState(true);
  const [detailsLoading, setDetailsLoading] =
    useState(false);

  const [processingAction, setProcessingAction] =
    useState<DecisionAction | null>(null);

  const [error, setError] = useState("");
  const [modalError, setModalError] = useState("");
  const [message, setMessage] = useState("");

  const loadApprovals = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const parameters = new URLSearchParams({
        page: String(page),
        size: String(PAGE_SIZE),
        direction,
      });

      const result = await api<PageData<Approval>>(
        `/api/policies/approvals/my-queue?${parameters.toString()}`
      );

      setApprovals(result.content || []);
      setTotalPages(result.totalPages || 0);
      setTotalElements(result.totalElements || 0);
    } catch (requestError) {
      setApprovals([]);
      setTotalPages(0);
      setTotalElements(0);

      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load the approval queue."
      );
    } finally {
      setLoading(false);
    }
  }, [direction, page]);

  useEffect(() => {
    void loadApprovals();
  }, [loadApprovals]);

  const visibleApprovals = useMemo(() => {
    const keyword = search.trim().toLowerCase();

    if (!keyword) {
      return approvals;
    }

    return approvals.filter((approval) => {
      return (
        approval.policyName
          .toLowerCase()
          .includes(keyword) ||
        approval.policyCode
          .toLowerCase()
          .includes(keyword)
      );
    });
  }, [approvals, search]);

  function applySearch() {
    setSearch(searchInput);
  }

  async function openPolicy(
    approval: Approval
  ) {
    setSelectedApproval(approval);
    setSelectedPolicy(null);
    setApprovalHistory([]);
    setComments("");
    setModalError("");
    setDetailsLoading(true);

    try {
      const [policy, history] = await Promise.all([
        api<Policy>(
          `/api/policies/${approval.policyId}`
        ),

        api<Approval[]>(
          `/api/policies/${approval.policyId}/approval-history`
        ),
      ]);

      setSelectedPolicy(policy);
      setApprovalHistory(history || []);
    } catch (requestError) {
      setModalError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load policy details."
      );
    } finally {
      setDetailsLoading(false);
    }
  }

  function closePolicy() {
    if (processingAction) {
      return;
    }

    setSelectedApproval(null);
    setSelectedPolicy(null);
    setApprovalHistory([]);
    setComments("");
    setModalError("");
  }

  async function processDecision(
    event: FormEvent<HTMLFormElement>,
    action: DecisionAction
  ) {
    event.preventDefault();

    if (!selectedApproval) {
      return;
    }

    if (
      action === "REJECT" &&
      !comments.trim()
    ) {
      setModalError(
        "A rejection reason is required."
      );
      return;
    }

    const actionText =
      action === "APPROVE"
        ? "approve"
        : "reject";

    const confirmed = window.confirm(
      `Are you sure you want to ${actionText} ` +
        `"${selectedApproval.policyName}"?`
    );

    if (!confirmed) {
      return;
    }

    setProcessingAction(action);
    setModalError("");

    try {
      const endpoint =
        action === "APPROVE"
          ? "approve"
          : "reject";

      await api(
        `/api/policies/${selectedApproval.policyId}/${endpoint}`,
        {
          method: "PATCH",
          body: JSON.stringify({
            comments: comments.trim() || null,
          }),
        }
      );

      setMessage(
        action === "APPROVE"
          ? `"${selectedApproval.policyName}" was approved successfully.`
          : `"${selectedApproval.policyName}" was rejected successfully.`
      );

      setSelectedApproval(null);
      setSelectedPolicy(null);
      setApprovalHistory([]);
      setComments("");

      /*
       * If the final item on a later page was processed,
       * return to the previous page.
       */
      if (approvals.length === 1 && page > 0) {
        setPage((current) =>
          Math.max(0, current - 1)
        );
      } else {
        await loadApprovals();
      }
    } catch (requestError) {
      setModalError(
        requestError instanceof Error
          ? requestError.message
          : `Unable to ${actionText} the policy.`
      );
    } finally {
      setProcessingAction(null);
    }
  }

  return (
    <ProtectedRoute allowedRoles={[portalRole]}>
      <main className="min-h-screen bg-slate-50">
        <header className="bg-[#0b3266] text-white">
          <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6">
            <p className="text-blue-200">
              HRMS Policies Module
            </p>

            <h1 className="mt-2 text-3xl font-bold sm:text-4xl">
              {portalTitle}
            </h1>

            <p className="mt-3 text-lg text-blue-100">
              {portalDescription}
            </p>
          </div>
        </header>

        <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6">
          {message && (
            <div
              role="status"
              className="mb-5 rounded-xl border border-green-200 bg-green-50 p-4 text-green-800"
            >
              {message}
            </div>
          )}

          {error && (
            <div
              role="alert"
              className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-red-700"
            >
              {error}
            </div>
          )}

          <section className="rounded-2xl border border-slate-300 bg-white p-5 shadow-sm">
            <div className="flex flex-col gap-4 lg:flex-row">
              <div className="flex min-w-0 flex-1">
                <input
                  type="search"
                  value={searchInput}
                  onChange={(event) =>
                    setSearchInput(event.target.value)
                  }
                  onKeyDown={(event) => {
                    if (event.key === "Enter") {
                      applySearch();
                    }
                  }}
                  placeholder="Search policy name or code"
                  className="min-w-0 flex-1 rounded-l-xl border border-slate-300 px-4 py-3 outline-none focus:border-blue-600"
                />

                <button
                  type="button"
                  onClick={applySearch}
                  className="rounded-r-xl bg-blue-700 px-5 text-white hover:bg-blue-800"
                  aria-label="Search approval queue"
                >
                  <Search size={19} />
                </button>
              </div>

              <select
                value={direction}
                onChange={(event) => {
                  setDirection(
                    event.target.value as
                      | "asc"
                      | "desc"
                  );

                  setPage(0);
                }}
                className="rounded-xl border border-slate-300 px-4 py-3"
              >
                <option value="desc">
                  Newest first
                </option>

                <option value="asc">
                  Oldest first
                </option>
              </select>

              <button
                type="button"
                disabled={loading}
                onClick={() => void loadApprovals()}
                className="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-300 px-5 py-3 font-semibold disabled:opacity-50"
              >
                <RefreshCw
                  size={18}
                  className={
                    loading ? "animate-spin" : ""
                  }
                />
                Refresh
              </button>
            </div>
          </section>

          <section className="mt-6 overflow-hidden rounded-2xl border border-slate-300 bg-white shadow-sm">
            <div className="border-b border-slate-300 px-5 py-4">
              <h2 className="text-lg font-bold">
                Pending approvals ({totalElements})
              </h2>
            </div>

            <div className="w-full overflow-x-auto">
              <table className="w-full min-w-[850px] text-left">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-5 py-4">
                      Policy
                    </th>

                    <th className="px-5 py-4">
                      Stage
                    </th>

                    <th className="px-5 py-4">
                      Submitted
                    </th>

                    <th className="px-5 py-4 text-right">
                      Action
                    </th>
                  </tr>
                </thead>

                <tbody className="divide-y divide-slate-200">
                  {loading ? (
                    <tr>
                      <td
                        colSpan={4}
                        className="px-5 py-14 text-center text-slate-500"
                      >
                        Loading approval queue...
                      </td>
                    </tr>
                  ) : visibleApprovals.length === 0 ? (
                    <tr>
                      <td
                        colSpan={4}
                        className="px-5 py-14 text-center"
                      >
                        <Clock3 className="mx-auto h-10 w-10 text-slate-300" />

                        <p className="mt-3 font-semibold text-slate-700">
                          No pending approvals found
                        </p>
                      </td>
                    </tr>
                  ) : (
                    visibleApprovals.map(
                      (approval) => (
                        <tr
                          key={approval.approvalId}
                          className="hover:bg-slate-50"
                        >
                          <td className="px-5 py-4">
                            <p className="font-bold text-slate-900">
                              {approval.policyName}
                            </p>

                            <p className="mt-1 text-sm text-slate-500">
                              {approval.policyCode}
                            </p>
                          </td>

                          <td className="px-5 py-4">
                            {displayLabel(
                              approval.stage
                            )}
                          </td>

                          <td className="px-5 py-4 text-sm text-slate-600">
                            {formatDate(
                              approval.submittedAt
                            )}
                          </td>

                          <td className="px-5 py-4 text-right">
                            <button
                              type="button"
                              onClick={() =>
                                void openPolicy(
                                  approval
                                )
                              }
                              className="inline-flex items-center gap-2 rounded-lg bg-blue-700 px-4 py-2 font-bold text-white hover:bg-blue-800"
                            >
                              <Eye size={18} />
                              View and Review
                            </button>
                          </td>
                        </tr>
                      )
                    )
                  )}
                </tbody>
              </table>
            </div>

            <div className="flex flex-col items-center justify-between gap-4 border-t border-slate-300 px-5 py-4 sm:flex-row">
              <span className="text-sm text-slate-500">
                Page {totalPages === 0 ? 0 : page + 1} of{" "}
                {totalPages}
              </span>

              <div className="flex gap-2">
                <button
                  type="button"
                  disabled={page === 0 || loading}
                  onClick={() =>
                    setPage((current) =>
                      Math.max(0, current - 1)
                    )
                  }
                  className="rounded-lg border border-slate-300 px-4 py-2 font-semibold disabled:opacity-40"
                >
                  Previous
                </button>

                <button
                  type="button"
                  disabled={
                    loading ||
                    totalPages === 0 ||
                    page + 1 >= totalPages
                  }
                  onClick={() =>
                    setPage((current) => current + 1)
                  }
                  className="rounded-lg border border-slate-300 px-4 py-2 font-semibold disabled:opacity-40"
                >
                  Next
                </button>
              </div>
            </div>
          </section>
        </div>

        {selectedApproval && (
          <div
            className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/60 p-4 sm:p-8"
            role="dialog"
            aria-modal="true"
            aria-labelledby="review-policy-title"
          >
            <div className="my-auto w-full max-w-5xl overflow-hidden rounded-2xl bg-white shadow-2xl">
              <header className="flex items-start justify-between gap-5 border-b border-slate-200 bg-[#0b3266] p-6 text-white">
                <div>
                  <p className="text-sm text-blue-200">
                    {displayLabel(
                      selectedApproval.stage
                    )}
                  </p>

                  <h2
                    id="review-policy-title"
                    className="mt-1 text-2xl font-bold"
                  >
                    {selectedApproval.policyName}
                  </h2>

                  <p className="mt-1 text-blue-100">
                    {selectedApproval.policyCode}
                  </p>
                </div>

                <button
                  type="button"
                  disabled={
                    processingAction !== null
                  }
                  onClick={closePolicy}
                  className="rounded-lg p-2 hover:bg-white/10 disabled:opacity-50"
                  aria-label="Close policy review"
                >
                  <X size={25} />
                </button>
              </header>

              <div className="max-h-[75vh] overflow-y-auto p-6">
                {detailsLoading ? (
                  <div className="flex items-center justify-center py-20">
                    <RefreshCw className="mr-3 h-6 w-6 animate-spin text-blue-700" />
                    Loading policy details...
                  </div>
                ) : modalError &&
                  !selectedPolicy ? (
                  <div className="rounded-xl border border-red-200 bg-red-50 p-5 text-red-700">
                    <div className="flex items-center gap-2 font-bold">
                      <AlertCircle size={20} />
                      Unable to load policy
                    </div>

                    <p className="mt-2">
                      {modalError}
                    </p>
                  </div>
                ) : selectedPolicy ? (
                  <>
                    {modalError && (
                      <div className="mb-6 rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">
                        {modalError}
                      </div>
                    )}

                    <section className="grid grid-cols-1 gap-4 rounded-xl border border-slate-200 bg-slate-50 p-5 sm:grid-cols-2 lg:grid-cols-4">
                      <div>
                        <p className="text-sm font-semibold text-slate-500">
                          Category
                        </p>

                        <p className="mt-1 font-bold text-slate-900">
                          {selectedPolicy.categoryName}
                        </p>

                        <p className="text-xs text-slate-500">
                          {selectedPolicy.categoryCode}
                        </p>
                      </div>

                      <div>
                        <p className="text-sm font-semibold text-slate-500">
                          Applicability
                        </p>

                        <p className="mt-1 font-bold text-slate-900">
                          {displayLabel(
                            selectedPolicy.applicability
                          )}
                        </p>
                      </div>

                      <div>
                        <p className="text-sm font-semibold text-slate-500">
                          Mandatory
                        </p>

                        <p className="mt-1 font-bold text-slate-900">
                          {selectedPolicy.mandatory
                            ? "Yes"
                            : "No"}
                        </p>
                      </div>

                      <div>
                        <p className="text-sm font-semibold text-slate-500">
                          Created by
                        </p>

                        <p className="mt-1 font-bold text-slate-900">
                          {selectedPolicy.createdByName ||
                            "—"}
                        </p>
                      </div>

                      <div>
                        <p className="text-sm font-semibold text-slate-500">
                          Existing employee deadline
                        </p>

                        <p className="mt-1 font-bold text-slate-900">
                          {
                            selectedPolicy.acknowledgementPeriodDays
                          }{" "}
                          days
                        </p>
                      </div>

                      <div>
                        <p className="text-sm font-semibold text-slate-500">
                          New joiner deadline
                        </p>

                        <p className="mt-1 font-bold text-slate-900">
                          {
                            selectedPolicy.onboardingPeriodDays
                          }{" "}
                          days
                        </p>
                      </div>

                      {selectedPolicy.applicability ===
                        "DEPT_BASED" && (
                        <div className="sm:col-span-2">
                          <p className="text-sm font-semibold text-slate-500">
                            Departments
                          </p>

                          <p className="mt-1 font-bold text-slate-900">
                            {selectedPolicy
                              .applicableDepartments
                              ?.length
                              ? selectedPolicy.applicableDepartments.join(
                                  ", "
                                )
                              : "—"}
                          </p>
                        </div>
                      )}

                      {selectedPolicy.applicability ===
                        "GRADE_BASED" && (
                        <div className="sm:col-span-2">
                          <p className="text-sm font-semibold text-slate-500">
                            Grades
                          </p>

                          <p className="mt-1 font-bold text-slate-900">
                            {selectedPolicy
                              .applicableGrades?.length
                              ? selectedPolicy.applicableGrades.join(
                                  ", "
                                )
                              : "—"}
                          </p>
                        </div>
                      )}
                    </section>

                    <section className="mt-6 rounded-xl border border-slate-200">
                      <div className="flex items-center gap-2 border-b border-slate-200 px-5 py-4">
                        <FileText
                          size={21}
                          className="text-blue-700"
                        />

                        <h3 className="text-lg font-bold">
                          Complete Policy Content
                        </h3>
                      </div>

                      <div className="max-h-96 overflow-y-auto whitespace-pre-wrap break-words p-5 leading-7 text-slate-700">
                        {selectedPolicy.content}
                      </div>
                    </section>

                    <section className="mt-6 rounded-xl border border-slate-200">
                      <div className="border-b border-slate-200 px-5 py-4">
                        <h3 className="text-lg font-bold">
                          Approval History
                        </h3>
                      </div>

                      {approvalHistory.length === 0 ? (
                        <p className="p-5 text-slate-500">
                          No earlier approval actions.
                        </p>
                      ) : (
                        <div className="divide-y divide-slate-200">
                          {approvalHistory.map(
                            (history) => (
                              <div
                                key={history.approvalId}
                                className="grid gap-3 p-5 md:grid-cols-4"
                              >
                                <div>
                                  <p className="text-xs font-semibold text-slate-500">
                                    Stage
                                  </p>

                                  <p className="mt-1 font-semibold">
                                    {displayLabel(
                                      history.stage
                                    )}
                                  </p>
                                </div>

                                <div>
                                  <p className="text-xs font-semibold text-slate-500">
                                    Decision
                                  </p>

                                  <p className="mt-1 font-semibold">
                                    {displayLabel(
                                      history.decision
                                    )}
                                  </p>
                                </div>

                                <div>
                                  <p className="text-xs font-semibold text-slate-500">
                                    Reviewer
                                  </p>

                                  <p className="mt-1 font-semibold">
                                    {history.approverName ||
                                      "Pending"}
                                  </p>
                                </div>

                                <div>
                                  <p className="text-xs font-semibold text-slate-500">
                                    Date
                                  </p>

                                  <p className="mt-1 text-sm">
                                    {formatDate(
                                      history.decidedAt ||
                                        history.submittedAt
                                    )}
                                  </p>
                                </div>

                                {history.comments && (
                                  <div className="md:col-span-4">
                                    <p className="text-xs font-semibold text-slate-500">
                                      Comments
                                    </p>

                                    <p className="mt-1 text-slate-700">
                                      {history.comments}
                                    </p>
                                  </div>
                                )}
                              </div>
                            )
                          )}
                        </div>
                      )}
                    </section>

                    <form
                      className="mt-6"
                      onSubmit={(event) =>
                        void processDecision(
                          event,
                          "APPROVE"
                        )
                      }
                    >
                      <label className="block">
                        <span className="font-bold text-slate-900">
                          Reviewer comments
                        </span>

                        <textarea
                          value={comments}
                          onChange={(event) => {
                            setComments(
                              event.target.value
                            );

                            setModalError("");
                          }}
                          maxLength={2000}
                          rows={5}
                          placeholder={
                            "Enter review comments. " +
                            "Comments are required when rejecting."
                          }
                          className="mt-2 w-full resize-y rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-blue-600"
                        />

                        <span className="mt-1 block text-right text-xs text-slate-500">
                          {comments.length}/2000
                        </span>
                      </label>

                      <div className="mt-6 flex flex-col-reverse justify-end gap-3 border-t border-slate-200 pt-5 sm:flex-row">
                        <button
                          type="button"
                          disabled={
                            processingAction !== null
                          }
                          onClick={closePolicy}
                          className="rounded-xl border border-slate-300 px-5 py-3 font-bold text-slate-700 hover:bg-slate-50 disabled:opacity-50"
                        >
                          Close
                        </button>

                        <button
                          type="button"
                          disabled={
                            processingAction !== null
                          }
                          onClick={(event) => {
                            const form =
                              event.currentTarget.form;

                            if (form) {
                              void processDecision(
                                {
                                  preventDefault:
                                    () => undefined,
                                } as FormEvent<HTMLFormElement>,
                                "REJECT"
                              );
                            }
                          }}
                          className="inline-flex items-center justify-center gap-2 rounded-xl bg-red-700 px-5 py-3 font-bold text-white hover:bg-red-800 disabled:bg-red-400"
                        >
                          {processingAction ===
                          "REJECT" ? (
                            <RefreshCw
                              size={18}
                              className="animate-spin"
                            />
                          ) : (
                            <XCircle size={18} />
                          )}

                          {processingAction ===
                          "REJECT"
                            ? "Rejecting..."
                            : "Reject Policy"}
                        </button>

                        <button
                          type="submit"
                          disabled={
                            processingAction !== null
                          }
                          className="inline-flex items-center justify-center gap-2 rounded-xl bg-green-700 px-5 py-3 font-bold text-white hover:bg-green-800 disabled:bg-green-400"
                        >
                          {processingAction ===
                          "APPROVE" ? (
                            <RefreshCw
                              size={18}
                              className="animate-spin"
                            />
                          ) : (
                            <CheckCircle2 size={18} />
                          )}

                          {processingAction ===
                          "APPROVE"
                            ? "Approving..."
                            : "Approve Policy"}
                        </button>
                      </div>
                    </form>
                  </>
                ) : null}
              </div>
            </div>
          </div>
        )}
      </main>
    </ProtectedRoute>
  );
}