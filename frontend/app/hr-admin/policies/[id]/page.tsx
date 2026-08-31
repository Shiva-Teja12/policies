"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  Clock3,
  FileText,
  Pencil,
  Rocket,
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
import RetirePolicyButton from "@/components/RetirePolicyButton";
import { api } from "@/lib/api";

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
    | "GRADE_BASED"
    | "DEPT_BASED";

  applicableDepartments: string[];
  applicableGrades: string[];

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

  acknowledgementPeriodDays: number;
  onboardingPeriodDays: number;
  publishedOnce: boolean;

  effectiveDate?: string | null;
  createdByName?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
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

function displayLabel(value?: string | null) {
  if (!value) {
    return "—";
  }

  return value
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (letter) =>
      letter.toUpperCase()
    );
}

function formatDateOnly(value?: string | null) {
  if (!value) {
    return "—";
  }

  const parts = value.split("-");

  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
  }

  return value;
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "—";
  }

  /*
   * The current backend uses LocalDateTime.now(), and the
   * application runs in Asia/Kolkata. Therefore, timestamps
   * without a timezone must be treated as IST—not UTC.
   */
  let normalizedValue = value
    .trim()
    .replace(" ", "T")
    .replace(/(\.\d{3})\d+/, "$1");

  const hasTimezone =
    normalizedValue.endsWith("Z") ||
    /[+-]\d{2}:\d{2}$/.test(normalizedValue);

  if (!hasTimezone) {
    normalizedValue =
      `${normalizedValue}+05:30`;
  }

  const date = new Date(normalizedValue);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return (
    date.toLocaleString("en-IN", {
      timeZone: "Asia/Kolkata",
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: true,
    }) + " IST"
  );
}
function statusStyle(status: Policy["status"]) {
  switch (status) {
    case "DRAFT":
      return "bg-slate-100 text-slate-700";

    case "LEGAL_REVIEW":
    case "HR_HEAD_REVIEW":
    case "MD_REVIEW":
      return "bg-amber-100 text-amber-800";

    case "APPROVED":
      return "bg-green-100 text-green-800";

    case "PUBLISHED":
      return "bg-blue-100 text-blue-800";

    case "REJECTED":
      return "bg-red-100 text-red-800";

    case "RETIRED":
      return "bg-purple-100 text-purple-800";

    default:
      return "bg-slate-100 text-slate-700";
  }
}

function decisionStyle(
  decision: Approval["decision"]
) {
  switch (decision) {
    case "APPROVED":
      return "bg-green-100 text-green-800";

    case "REJECTED":
      return "bg-red-100 text-red-800";

    default:
      return "bg-amber-100 text-amber-800";
  }
}

function ApprovalIcon({
  decision,
}: {
  decision: Approval["decision"];
}) {
  if (decision === "APPROVED") {
    return (
      <CheckCircle2
        size={22}
        className="text-green-600"
      />
    );
  }

  if (decision === "REJECTED") {
    return (
      <XCircle
        size={22}
        className="text-red-600"
      />
    );
  }

  return (
    <Clock3
      size={22}
      className="text-amber-600"
    />
  );
}

export default function PolicyDetailsPage() {
  const params = useParams<{ id: string }>();
  const policyId = Number(params.id);

  const [policy, setPolicy] =
    useState<Policy | null>(null);

  const [approvals, setApprovals] = useState<
    Approval[]
  >([]);

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] =
    useState(false);

  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const latestRejection = useMemo(() => {
    return [...approvals]
      .filter(
        (approval) =>
          approval.decision === "REJECTED"
      )
      .sort((first, second) => {
        const firstDate = new Date(
          first.decidedAt ?? 0
        ).getTime();

        const secondDate = new Date(
          second.decidedAt ?? 0
        ).getTime();

        return secondDate - firstDate;
      })[0];
  }, [approvals]);

  const loadDetails = useCallback(async () => {
    if (
      !Number.isFinite(policyId) ||
      policyId <= 0
    ) {
      setError("Invalid policy ID.");
      setLoading(false);
      return;
    }

    setLoading(true);
    setError("");

    try {
      const [policyData, approvalData] =
        await Promise.all([
          api<Policy>(
            `/api/policies/${policyId}`
          ),

          api<Approval[]>(
            `/api/policies/${policyId}/approval-history`
          ),
        ]);

      setPolicy(policyData);

      setApprovals(
        (approvalData || []).sort(
          (first, second) =>
            first.sequenceNumber -
            second.sequenceNumber
        )
      );
    } catch (requestError) {
      setPolicy(null);
      setApprovals([]);

      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load policy details."
      );
    } finally {
      setLoading(false);
    }
  }, [policyId]);

  useEffect(() => {
    void loadDetails();
  }, [loadDetails]);

  async function submitForReview() {
    if (!policy) {
      return;
    }

    const confirmed = window.confirm(
      policy.status === "REJECTED"
        ? "Have you updated the policy according to the rejection comment?"
        : "Submit this policy to the Legal Reviewer?"
    );

    if (!confirmed) {
      return;
    }

    setSubmitting(true);
    setError("");
    setMessage("");

    try {
      await api(
        `/api/policies/${policy.id}/submit-for-review`,
        {
          method: "PATCH",
        }
      );

      setMessage(
        "Policy submitted to the Legal Reviewer successfully."
      );

      await loadDetails();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to submit policy."
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <ProtectedRoute allowedRoles={["HR_ADMIN"]}>
      <main className="min-h-screen bg-slate-50">
        <header className="bg-[#082b5c] text-white">
          <div className="mx-auto flex w-full max-w-7xl flex-col justify-between gap-5 px-4 py-7 sm:px-6 lg:flex-row lg:items-center">
            <div className="flex items-start gap-4">
              <Link
                href="/hr-admin/policies"
                title="Back to policies"
                className="mt-1 rounded-xl border border-white/30 p-3 hover:bg-white/10"
              >
                <ArrowLeft size={22} />
              </Link>

              <div>
                <p className="text-sm text-blue-200">
                  HR Admin · Policy Details
                </p>

                <h1 className="mt-1 text-3xl font-bold">
                  {loading
                    ? "Loading..."
                    : policy?.name ??
                      "Policy Details"}
                </h1>

                {policy && (
                  <p className="mt-2 text-blue-100">
                    {policy.code}
                  </p>
                )}
              </div>
            </div>

            {policy && (
              <div className="flex flex-wrap gap-3">
                {(policy.status === "DRAFT" ||
                  policy.status ===
                    "REJECTED") && (
                  <>
                    <Link
                      href={`/hr-admin/policies/${policy.id}/edit`}
                      className="inline-flex items-center gap-2 rounded-xl border border-white/30 px-4 py-3 font-semibold hover:bg-white/10"
                    >
                      <Pencil size={18} />
                      Edit Policy
                    </Link>

                    <button
                      type="button"
                      disabled={submitting}
                      onClick={() =>
                        void submitForReview()
                      }
                      className="inline-flex items-center gap-2 rounded-xl bg-blue-500 px-4 py-3 font-semibold hover:bg-blue-400 disabled:opacity-50"
                    >
                      <Send size={18} />

                      {submitting
                        ? "Submitting..."
                        : policy.status ===
                            "REJECTED"
                          ? "Resubmit"
                          : "Submit for Review"}
                    </button>
                  </>
                )}

                {policy.status === "APPROVED" && (
                  <Link
                    href={`/hr-admin/ready-to-publish?policyId=${policy.id}`}
                    className="inline-flex items-center gap-2 rounded-xl bg-green-600 px-4 py-3 font-semibold hover:bg-green-500"
                  >
                    <Rocket size={18} />
                    Publish Policy
                  </Link>
                )}

                <RetirePolicyButton
                  policyId={policy.id}
                  policyName={policy.name}
                  policyCode={policy.code}
                  status={policy.status}
                />
              </div>
            )}
          </div>
        </header>

        <div className="mx-auto w-full max-w-7xl space-y-6 px-4 py-8 sm:px-6">
          {message && (
            <div className="rounded-xl border border-green-200 bg-green-50 p-4 text-green-800">
              {message}
            </div>
          )}

          {error && (
            <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">
              {error}
            </div>
          )}

          {loading && (
            <section className="rounded-2xl border border-slate-200 bg-white p-12 text-center shadow-sm">
              <p className="text-slate-500">
                Loading policy details...
              </p>
            </section>
          )}

          {!loading && policy && (
            <>
              {/* REJECTION COMMENT */}
              {policy.status === "REJECTED" &&
                latestRejection && (
                  <section className="overflow-hidden rounded-2xl border border-red-300 bg-red-50 shadow-sm">
                    <div className="flex items-start gap-4 p-6">
                      <div className="rounded-full bg-red-100 p-3">
                        <AlertTriangle
                          size={27}
                          className="text-red-700"
                        />
                      </div>

                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-bold uppercase tracking-wide text-red-600">
                          Policy rejected
                        </p>

                        <h2 className="mt-1 text-xl font-bold text-red-900">
                          Rejection comment
                        </h2>

                        <div className="mt-4 rounded-xl border border-red-200 bg-white p-4">
                          <p className="whitespace-pre-wrap break-words text-lg font-semibold leading-7 text-slate-900">
                            {latestRejection.comments?.trim() ||
                              "No rejection comment was provided."}
                          </p>
                        </div>

                        <div className="mt-4 grid grid-cols-1 gap-2 text-sm text-red-800 sm:grid-cols-3">
                          <p>
                            <strong>Stage:</strong>{" "}
                            {displayLabel(
                              latestRejection.stage
                            )}
                          </p>

                          <p>
                            <strong>
                              Rejected by:
                            </strong>{" "}
                            {latestRejection.approverName ||
                              latestRejection.approverEmail ||
                              "Reviewer"}
                          </p>

                          <p>
                            <strong>
                              Rejected at:
                            </strong>{" "}
                            {formatDateTime(
                              latestRejection.decidedAt
                            )}
                          </p>
                        </div>

                        <p className="mt-4 text-sm text-red-800">
                          Edit the policy according to this
                          comment and resubmit it for review.
                        </p>
                      </div>
                    </div>
                  </section>
                )}

              {/* POLICY INFORMATION */}
              <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                <div className="flex flex-col justify-between gap-3 border-b border-slate-200 px-6 py-5 sm:flex-row sm:items-center">
                  <div>
                    <h2 className="text-xl font-bold">
                      Policy Information
                    </h2>

                    <p className="mt-1 text-sm text-slate-500">
                      Policy configuration and details.
                    </p>
                  </div>

                  <span
                    className={`w-fit rounded-full px-4 py-2 text-sm font-bold ${statusStyle(
                      policy.status
                    )}`}
                  >
                    {displayLabel(policy.status)}
                  </span>
                </div>

                <div className="grid grid-cols-1 gap-6 p-6 sm:grid-cols-2 lg:grid-cols-4">
                  <div>
                    <p className="text-sm font-semibold text-slate-500">
                      Policy code
                    </p>

                    <p className="mt-1 font-bold">
                      {policy.code}
                    </p>
                  </div>

                  <div>
                    <p className="text-sm font-semibold text-slate-500">
                      Category
                    </p>

                    <p className="mt-1 font-bold">
                      {policy.categoryName}
                    </p>

                    <p className="mt-1 text-xs text-slate-500">
                      {policy.categoryCode}
                    </p>
                  </div>

                  <div>
                    <p className="text-sm font-semibold text-slate-500">
                      Applicability
                    </p>

                    <p className="mt-1 font-bold">
                      {displayLabel(
                        policy.applicability
                      )}
                    </p>
                  </div>

                  <div>
                    <p className="text-sm font-semibold text-slate-500">
                      Mandatory
                    </p>

                    <p
                      className={`mt-1 font-bold ${
                        policy.mandatory
                          ? "text-red-700"
                          : ""
                      }`}
                    >
                      {policy.mandatory
                        ? "Yes"
                        : "No"}
                    </p>
                  </div>

                  <div>
                    <p className="text-sm font-semibold text-slate-500">
                      Created by
                    </p>

                    <p className="mt-1 font-bold">
                      {policy.createdByName || "—"}
                    </p>
                  </div>

                  <div>
                    <p className="text-sm font-semibold text-slate-500">
                      Existing employee deadline
                    </p>

                    <p className="mt-1 font-bold">
                      {policy.acknowledgementPeriodDays ??
                        7}{" "}
                      days
                    </p>
                  </div>

                  <div>
                    <p className="text-sm font-semibold text-slate-500">
                      New joiner deadline
                    </p>

                    <p className="mt-1 font-bold">
                      {policy.onboardingPeriodDays ??
                        7}{" "}
                      days
                    </p>
                  </div>

                  <div>
                    <p className="text-sm font-semibold text-slate-500">
                      Effective date
                    </p>

                    <p className="mt-1 font-bold">
                      {formatDateOnly(
                        policy.effectiveDate
                      )}
                    </p>
                  </div>

                  <div>
                    <p className="text-sm font-semibold text-slate-500">
                      Created at
                    </p>

                    <p className="mt-1 text-sm font-medium">
                      {formatDateTime(
                        policy.createdAt
                      )}
                    </p>
                  </div>

                  <div>
                    <p className="text-sm font-semibold text-slate-500">
                      Updated at
                    </p>

                    <p className="mt-1 text-sm font-medium">
                      {formatDateTime(
                        policy.updatedAt
                      )}
                    </p>
                  </div>
                </div>
              </section>

              {/* POLICY CONTENT */}
              <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                <div className="flex items-center gap-3 border-b border-slate-200 px-6 py-5">
                  <FileText
                    size={24}
                    className="text-blue-700"
                  />

                  <h2 className="text-xl font-bold">
                    Complete Policy Content
                  </h2>
                </div>

                <article className="whitespace-pre-wrap break-words p-6 leading-8 text-slate-700">
                  {policy.content ||
                    "No policy content is available."}
                </article>
              </section>

              {/* APPROVAL HISTORY */}
              <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                <div className="border-b border-slate-200 px-6 py-5">
                  <h2 className="text-xl font-bold">
                    Approval History
                  </h2>

                  <p className="mt-1 text-sm text-slate-500">
                    Reviewer decisions and comments.
                  </p>
                </div>

                {approvals.length === 0 ? (
                  <div className="p-10 text-center text-slate-500">
                    This policy has not been submitted for
                    review.
                  </div>
                ) : (
                  <div className="divide-y divide-slate-200">
                    {approvals.map((approval) => (
                      <div
                        key={approval.approvalId}
                        className="p-6"
                      >
                        <div className="flex flex-col justify-between gap-4 sm:flex-row">
                          <div className="flex items-start gap-3">
                            <ApprovalIcon
                              decision={
                                approval.decision
                              }
                            />

                            <div>
                              <h3 className="font-bold">
                                {displayLabel(
                                  approval.stage
                                )}
                              </h3>

                              <p className="mt-1 text-sm text-slate-500">
                                Stage{" "}
                                {approval.sequenceNumber}
                              </p>
                            </div>
                          </div>

                          <span
                            className={`h-fit w-fit rounded-full px-3 py-1 text-xs font-bold ${decisionStyle(
                              approval.decision
                            )}`}
                          >
                            {displayLabel(
                              approval.decision
                            )}
                          </span>
                        </div>

                        <div className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-3">
                          <div>
                            <p className="text-xs font-bold uppercase text-slate-500">
                              Reviewer
                            </p>

                            <p className="mt-2 font-medium">
                              {approval.approverName ||
                                approval.approverEmail ||
                                (approval.decision ===
                                "PENDING"
                                  ? "Pending"
                                  : "—")}
                            </p>
                          </div>

                          <div>
                            <p className="text-xs font-bold uppercase text-slate-500">
                              Submitted
                            </p>

                            <p className="mt-2 text-sm font-medium">
                              {formatDateTime(
                                approval.submittedAt
                              )}
                            </p>
                          </div>

                          <div>
                            <p className="text-xs font-bold uppercase text-slate-500">
                              Decision date
                            </p>

                            <p className="mt-2 text-sm font-medium">
                              {formatDateTime(
                                approval.decidedAt
                              )}
                            </p>
                          </div>
                        </div>

                        {approval.comments?.trim() && (
                          <div
                            className={`mt-5 rounded-xl border p-4 ${
                              approval.decision ===
                              "REJECTED"
                                ? "border-red-200 bg-red-50"
                                : "border-green-200 bg-green-50"
                            }`}
                          >
                            <p className="text-xs font-bold uppercase tracking-wide text-slate-600">
                              {approval.decision ===
                              "REJECTED"
                                ? "Rejection comment"
                                : "Reviewer comment"}
                            </p>

                            <p className="mt-2 whitespace-pre-wrap break-words font-semibold leading-7 text-slate-900">
                              {approval.comments}
                            </p>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </section>
            </>
          )}
        </div>
      </main>
    </ProtectedRoute>
  );
}