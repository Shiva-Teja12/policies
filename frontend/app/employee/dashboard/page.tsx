"use client";

import {
  AlertCircle,
  Bot,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  FileText,
  RefreshCw,
  Search,
  Send,
  X,
} from "lucide-react";
import {
  FormEvent,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import ProtectedRoute from "@/components/ProtectedRoute";
import { api } from "@/lib/api";

type AssignmentStatus =
  | "PENDING"
  | "ACKNOWLEDGED"
  | "OVERDUE"
  | "CANCELLED";

interface MyPolicyStatus {
  assignmentId: number;
  policyId: number;
  policyCode: string;
  policyName: string;
  categoryId: number;
  categoryName: string;
  mandatory: boolean;
  policyVersionId: number;
  versionNumber: number;
  effectiveDate: string;
  deadline: string;
  acknowledged: boolean;
  acknowledgedAt?: string | null;
  daysOverdue: number;
  assignmentStatus: AssignmentStatus;
}

interface PolicyVersion {
  id: number;
  policyId: number;
  policyCode: string;
  policyName: string;
  versionNumber: number;
  content: string;
  effectiveDate: string;
  publishedAt: string;
  publishedById: number;
  publishedByName: string;
  publishedByEmail: string;
  changeSummary?: string | null;
  currentVersion: boolean;
  archived: boolean;
  assignedEmployees: number;
}

interface AcknowledgementResponse {
  acknowledgementId: number;
  employeeId: number;
  employeeName: string;
  employeeEmail: string;
  policyId: number;
  policyCode: string;
  policyName: string;
  policyVersionId: number;
  versionNumber: number;
  acknowledgedAt: string;
  ipAddress?: string | null;
  userAgent?: string | null;
  updatedExistingAcknowledgement: boolean;
}

interface PolicyAnswerSource {
  policyId: number;
  policyCode: string;
  policyName: string;
  versionNumber: number;
  sectionReference: string;
  relevantExcerpt: string;
  relevanceScore: number;
}

interface PolicyAnswer {
  question: string;
  answer: string;
  aiProviderConfigured: boolean;
  retrievalMode: string;
  sources: PolicyAnswerSource[];
}

const PAGE_SIZE = 10;

function formatDate(value?: string | null) {
  if (!value) {
    return "—";
  }

  /*
   * Date-only values must not be converted through UTC because
   * doing so can move the date backward or forward in some zones.
   */
  const parts = value.split("-");

  if (parts.length === 3) {
    const [year, month, day] = parts;

    return `${day}/${month}/${year}`;
  }

  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleDateString("en-IN");
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "—";
  }

  /*
   * Spring LocalDateTime returns no timezone suffix.
   * The backend stores acknowledgement timestamps in UTC,
   * so append Z before converting them to IST.
   */
  const hasTimezone =
    value.endsWith("Z") ||
    /[+-]\d{2}:\d{2}$/.test(value);

  const utcValue = hasTimezone
    ? value
    : `${value}Z`;

  const date = new Date(utcValue);

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

function statusStyle(status: AssignmentStatus) {
  switch (status) {
    case "ACKNOWLEDGED":
      return "bg-green-100 text-green-800";

    case "OVERDUE":
      return "bg-red-100 text-red-800";

    case "PENDING":
      return "bg-amber-100 text-amber-800";

    default:
      return "bg-slate-100 text-slate-700";
  }
}

function statusLabel(status: AssignmentStatus) {
  return status
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (letter) =>
      letter.toUpperCase()
    );
}

export default function EmployeeDashboardPage() {
  const [policies, setPolicies] = useState<
    MyPolicyStatus[]
  >([]);

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] =
    useState("ALL");
  const [sortBy, setSortBy] =
    useState("deadline");
  const [page, setPage] = useState(0);

  const [selectedAssignment, setSelectedAssignment] =
    useState<MyPolicyStatus | null>(null);
  const [selectedVersion, setSelectedVersion] =
    useState<PolicyVersion | null>(null);
  const [confirmedReading, setConfirmedReading] =
    useState(false);

  const [loading, setLoading] = useState(true);
  const [versionLoading, setVersionLoading] =
    useState(false);
  const [acknowledging, setAcknowledging] =
    useState(false);

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [modalError, setModalError] = useState("");

  const [question, setQuestion] = useState("");
  const [asking, setAsking] = useState(false);
  const [policyAnswer, setPolicyAnswer] =
    useState<PolicyAnswer | null>(null);
  const [questionError, setQuestionError] =
    useState("");

  const loadPolicies = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const result = await api<MyPolicyStatus[]>(
        "/api/policies/my-status"
      );

      setPolicies(result || []);
    } catch (requestError) {
      setPolicies([]);

      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load your policies."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadPolicies();
  }, [loadPolicies]);

  const assignedCount = policies.length;

  const acknowledgedCount = policies.filter(
    (policy) => policy.acknowledged
  ).length;

  const overdueCount = policies.filter(
    (policy) =>
      policy.assignmentStatus === "OVERDUE"
  ).length;

  const filteredPolicies = useMemo(() => {
    const keyword = search.trim().toLowerCase();

    const result = policies.filter((policy) => {
      const matchesSearch =
        !keyword ||
        policy.policyName
          .toLowerCase()
          .includes(keyword) ||
        policy.policyCode
          .toLowerCase()
          .includes(keyword) ||
        policy.categoryName
          .toLowerCase()
          .includes(keyword);

      const matchesStatus =
        statusFilter === "ALL" ||
        policy.assignmentStatus === statusFilter;

      return matchesSearch && matchesStatus;
    });

    return [...result].sort((first, second) => {
      switch (sortBy) {
        case "name":
          return first.policyName.localeCompare(
            second.policyName
          );

        case "status":
          return first.assignmentStatus.localeCompare(
            second.assignmentStatus
          );

        case "deadline-desc":
          return (
            new Date(second.deadline).getTime() -
            new Date(first.deadline).getTime()
          );

        case "deadline":
        default:
          return (
            new Date(first.deadline).getTime() -
            new Date(second.deadline).getTime()
          );
      }
    });
  }, [policies, search, sortBy, statusFilter]);

  const totalPages = Math.ceil(
    filteredPolicies.length / PAGE_SIZE
  );

  const visiblePolicies = useMemo(() => {
    const start = page * PAGE_SIZE;

    return filteredPolicies.slice(
      start,
      start + PAGE_SIZE
    );
  }, [filteredPolicies, page]);

  useEffect(() => {
    setPage(0);
  }, [search, sortBy, statusFilter]);

  useEffect(() => {
    if (totalPages === 0) {
      setPage(0);
      return;
    }

    if (page >= totalPages) {
      setPage(totalPages - 1);
    }
  }, [page, totalPages]);

  async function openPolicy(
    assignment: MyPolicyStatus
  ) {
    setSelectedAssignment(assignment);
    setSelectedVersion(null);
    setConfirmedReading(false);
    setModalError("");
    setVersionLoading(true);

    try {
      const version = await api<PolicyVersion>(
        `/api/policies/${assignment.policyId}/versions/${assignment.versionNumber}`
      );

      setSelectedVersion(version);
    } catch (requestError) {
      setModalError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load the published policy."
      );
    } finally {
      setVersionLoading(false);
    }
  }

  function closePolicy() {
    if (acknowledging) {
      return;
    }

    setSelectedAssignment(null);
    setSelectedVersion(null);
    setConfirmedReading(false);
    setModalError("");
  }

  async function acknowledgePolicy() {
    if (
      !selectedAssignment ||
      !selectedVersion
    ) {
      return;
    }

    if (!confirmedReading) {
      setModalError(
        "Confirm that you have read and understood the policy."
      );
      return;
    }

    const confirmed = window.confirm(
      `Acknowledge ${selectedAssignment.policyName} ` +
        `Version ${selectedAssignment.versionNumber}?`
    );

    if (!confirmed) {
      return;
    }

    setAcknowledging(true);
    setModalError("");

    try {
      const acknowledgement =
        await api<AcknowledgementResponse>(
          `/api/policies/${selectedAssignment.policyId}/acknowledge`,
          {
            method: "POST",
          }
        );

      setMessage(
        `${selectedAssignment.policyName} Version ` +
          `${selectedAssignment.versionNumber} was acknowledged ` +
          `at ${formatDateTime(
            acknowledgement.acknowledgedAt
          )}.`
      );

      setSelectedAssignment(null);
      setSelectedVersion(null);
      setConfirmedReading(false);

      await loadPolicies();
    } catch (requestError) {
      setModalError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to acknowledge the policy."
      );
    } finally {
      setAcknowledging(false);
    }
  }

  async function askPolicyQuestion(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    if (question.trim().length < 3) {
      setQuestionError(
        "Enter a question containing at least 3 characters."
      );
      return;
    }

    setAsking(true);
    setQuestionError("");
    setPolicyAnswer(null);

    try {
      const result = await api<PolicyAnswer>(
        "/api/policies/ask",
        {
          method: "POST",
          body: JSON.stringify({
            question: question.trim(),
          }),
        }
      );

      setPolicyAnswer(result);
    } catch (requestError) {
      setQuestionError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to answer your question."
      );
    } finally {
      setAsking(false);
    }
  }

  return (
    <ProtectedRoute allowedRoles={["EMPLOYEE"]}>
      <main className="min-h-screen bg-slate-50">
        <header className="bg-[#0b3266] text-white">
          <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6">
            <p className="text-blue-200">
              Employee Portal
            </p>

            <h1 className="mt-1 text-3xl font-bold sm:text-4xl">
              My Policies
            </h1>

            <p className="mt-3 text-lg text-blue-100">
              Read and acknowledge policies assigned
              to you.
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

          <section className="grid grid-cols-1 gap-5 md:grid-cols-3">
            <article className="rounded-2xl border border-slate-300 bg-white p-6 shadow-sm">
              <p className="text-slate-600">
                Assigned
              </p>

              <p className="mt-2 text-4xl font-bold">
                {assignedCount}
              </p>
            </article>

            <article className="rounded-2xl border border-green-200 bg-green-50 p-6">
              <p className="text-green-700">
                Acknowledged
              </p>

              <p className="mt-2 text-4xl font-bold text-green-900">
                {acknowledgedCount}
              </p>
            </article>

            <article className="rounded-2xl border border-red-200 bg-red-50 p-6">
              <p className="text-red-700">
                Overdue
              </p>

              <p className="mt-2 text-4xl font-bold text-red-900">
                {overdueCount}
              </p>
            </article>
          </section>

          <section className="mt-7 rounded-2xl border border-slate-300 bg-white p-5 shadow-sm">
            <div className="grid grid-cols-1 gap-4 lg:grid-cols-[2fr_1fr_1fr]">
              <div className="relative">
                <Search
                  size={20}
                  className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400"
                />

                <input
                  type="search"
                  value={search}
                  onChange={(event) =>
                    setSearch(event.target.value)
                  }
                  placeholder="Search policy or category"
                  className="w-full rounded-xl border border-slate-300 py-3 pl-12 pr-4 outline-none focus:border-blue-600"
                />
              </div>

              <select
                value={statusFilter}
                onChange={(event) =>
                  setStatusFilter(event.target.value)
                }
                className="rounded-xl border border-slate-300 px-4 py-3"
              >
                <option value="ALL">
                  All statuses
                </option>

                <option value="PENDING">
                  Pending
                </option>

                <option value="ACKNOWLEDGED">
                  Acknowledged
                </option>

                <option value="OVERDUE">
                  Overdue
                </option>
              </select>

              <select
                value={sortBy}
                onChange={(event) =>
                  setSortBy(event.target.value)
                }
                className="rounded-xl border border-slate-300 px-4 py-3"
              >
                <option value="deadline">
                  Deadline earliest
                </option>

                <option value="deadline-desc">
                  Deadline latest
                </option>

                <option value="name">
                  Policy A–Z
                </option>

                <option value="status">
                  Status A–Z
                </option>
              </select>
            </div>
          </section>

          <section className="mt-7 overflow-hidden rounded-2xl border border-slate-300 bg-white shadow-sm">
            <div className="w-full overflow-x-auto">
              <table className="w-full min-w-[1000px] text-left">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-5 py-4">
                      Policy
                    </th>

                    <th className="px-5 py-4">
                      Version
                    </th>

                    <th className="px-5 py-4">
                      Effective
                    </th>

                    <th className="px-5 py-4">
                      Deadline
                    </th>

                    <th className="px-5 py-4">
                      Status
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
                        colSpan={6}
                        className="px-5 py-14 text-center text-slate-500"
                      >
                        <RefreshCw className="mx-auto mb-3 h-6 w-6 animate-spin" />
                        Loading your policies...
                      </td>
                    </tr>
                  ) : visiblePolicies.length === 0 ? (
                    <tr>
                      <td
                        colSpan={6}
                        className="px-5 py-14 text-center"
                      >
                        <BookOpen className="mx-auto h-10 w-10 text-slate-300" />

                        <p className="mt-3 font-semibold text-slate-700">
                          No assigned policies found
                        </p>
                      </td>
                    </tr>
                  ) : (
                    visiblePolicies.map((policy) => (
                      <tr
                        key={policy.assignmentId}
                        className="hover:bg-slate-50"
                      >
                        <td className="px-5 py-4">
                          <p className="font-bold text-slate-900">
                            {policy.policyName}
                          </p>

                          <p className="mt-1 text-sm text-slate-500">
                            {policy.policyCode} ·{" "}
                            {policy.categoryName}
                          </p>

                          {policy.mandatory && (
                            <span className="mt-2 inline-block rounded-full bg-red-100 px-2 py-1 text-xs font-bold text-red-700">
                              Mandatory
                            </span>
                          )}
                        </td>

                        <td className="px-5 py-4">
                          Version {policy.versionNumber}
                        </td>

                        <td className="px-5 py-4">
                          {formatDate(
                            policy.effectiveDate
                          )}
                        </td>

                        <td className="px-5 py-4">
                          <div className="flex items-center gap-2">
                            <CalendarDays
                              size={17}
                              className="text-slate-500"
                            />

                            {formatDate(policy.deadline)}
                          </div>

                          {policy.daysOverdue > 0 && (
                            <p className="mt-1 text-sm font-semibold text-red-700">
                              {policy.daysOverdue} day
                              {policy.daysOverdue === 1
                                ? ""
                                : "s"}{" "}
                              overdue
                            </p>
                          )}
                        </td>

                        <td className="px-5 py-4">
                          <span
                            className={`rounded-full px-3 py-1 text-xs font-bold ${statusStyle(
                              policy.assignmentStatus
                            )}`}
                          >
                            {statusLabel(
                              policy.assignmentStatus
                            )}
                          </span>

                          {policy.acknowledgedAt && (
                            <p className="mt-2 text-xs text-slate-500">
                              {formatDateTime(
                                policy.acknowledgedAt
                              )}
                            </p>
                          )}
                        </td>

                        <td className="px-5 py-4 text-right">
                          <button
                            type="button"
                            onClick={() =>
                              void openPolicy(policy)
                            }
                            className="inline-flex items-center gap-2 rounded-lg bg-blue-700 px-4 py-2 font-bold text-white hover:bg-blue-800"
                          >
                            <BookOpen size={18} />

                            {policy.acknowledged
                              ? "Read Policy"
                              : "Read and Acknowledge"}
                          </button>
                        </td>
                      </tr>
                    ))
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

          <section className="mt-7 rounded-2xl border border-slate-300 bg-white p-6 shadow-sm">
            <div className="flex items-center gap-3">
              <Bot className="text-blue-700" />

              <div>
                <h2 className="text-xl font-bold">
                  Ask about a policy
                </h2>

                <p className="text-sm text-slate-500">
                  Ask a question about currently
                  published policies.
                </p>
              </div>
            </div>

            <form
              onSubmit={askPolicyQuestion}
              className="mt-5 flex flex-col gap-3 sm:flex-row"
            >
              <input
                type="text"
                value={question}
                maxLength={1000}
                onChange={(event) =>
                  setQuestion(event.target.value)
                }
                placeholder="Example: What does the leave policy say?"
                className="min-w-0 flex-1 rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-blue-600"
              />

              <button
                type="submit"
                disabled={asking}
                className="inline-flex items-center justify-center gap-2 rounded-xl bg-blue-700 px-5 py-3 font-bold text-white disabled:bg-blue-400"
              >
                {asking ? (
                  <RefreshCw
                    size={18}
                    className="animate-spin"
                  />
                ) : (
                  <Send size={18} />
                )}

                {asking ? "Asking..." : "Ask"}
              </button>
            </form>

            {questionError && (
              <div className="mt-4 rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">
                {questionError}
              </div>
            )}

            {policyAnswer && (
              <div className="mt-5 rounded-xl border border-blue-200 bg-blue-50 p-5">
                <h3 className="font-bold text-blue-950">
                  Answer
                </h3>

                <p className="mt-3 whitespace-pre-wrap leading-7 text-slate-800">
                  {policyAnswer.answer}
                </p>

                {policyAnswer.sources?.length > 0 && (
                  <div className="mt-5 border-t border-blue-200 pt-4">
                    <h4 className="font-bold text-blue-950">
                      Sources
                    </h4>

                    <ul className="mt-3 space-y-3">
                      {policyAnswer.sources.map(
                        (source, index) => (
                          <li
                            key={`${source.policyId}-${source.versionNumber}-${index}`}
                            className="rounded-lg bg-white p-3"
                          >
                            <p className="font-semibold">
                              {source.policyName} – Version{" "}
                              {source.versionNumber}
                            </p>

                            <p className="mt-1 text-sm text-slate-600">
                              {source.sectionReference}
                            </p>
                          </li>
                        )
                      )}
                    </ul>
                  </div>
                )}
              </div>
            )}
          </section>
        </div>

        {selectedAssignment && (
          <div
            className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/60 p-4 sm:p-8"
            role="dialog"
            aria-modal="true"
          >
            <div className="my-auto w-full max-w-4xl overflow-hidden rounded-2xl bg-white shadow-2xl">
              <header className="flex items-start justify-between gap-5 border-b border-slate-200 bg-[#0b3266] p-6 text-white">
                <div>
                  <p className="text-sm text-blue-200">
                    Published Policy
                  </p>

                  <h2 className="mt-1 text-2xl font-bold">
                    {selectedAssignment.policyName}
                  </h2>

                  <p className="mt-1 text-blue-100">
                    {selectedAssignment.policyCode} · Version{" "}
                    {selectedAssignment.versionNumber}
                  </p>
                </div>

                <button
                  type="button"
                  disabled={acknowledging}
                  onClick={closePolicy}
                  className="rounded-lg p-2 hover:bg-white/10 disabled:opacity-50"
                  aria-label="Close policy"
                >
                  <X size={25} />
                </button>
              </header>

              <div className="max-h-[76vh] overflow-y-auto p-6">
                {versionLoading ? (
                  <div className="flex items-center justify-center py-20">
                    <RefreshCw className="mr-3 h-6 w-6 animate-spin text-blue-700" />
                    Loading published policy...
                  </div>
                ) : modalError && !selectedVersion ? (
                  <div className="rounded-xl border border-red-200 bg-red-50 p-5 text-red-700">
                    <div className="flex items-center gap-2 font-bold">
                      <AlertCircle size={20} />
                      Unable to load policy
                    </div>

                    <p className="mt-2">
                      {modalError}
                    </p>
                  </div>
                ) : selectedVersion ? (
                  <>
                    {modalError && (
                      <div className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">
                        {modalError}
                      </div>
                    )}

                    <section className="grid grid-cols-1 gap-4 rounded-xl border border-slate-200 bg-slate-50 p-5 sm:grid-cols-2 lg:grid-cols-4">
                      <div>
                        <p className="text-sm font-semibold text-slate-500">
                          Version
                        </p>

                        <p className="mt-1 font-bold">
                          {selectedVersion.versionNumber}
                        </p>
                      </div>

                      <div>
                        <p className="text-sm font-semibold text-slate-500">
                          Effective date
                        </p>

                        <p className="mt-1 font-bold">
                          {formatDate(
                            selectedVersion.effectiveDate
                          )}
                        </p>
                      </div>

                      <div>
                        <p className="text-sm font-semibold text-slate-500">
                          Deadline
                        </p>

                        <p className="mt-1 font-bold">
                          {formatDate(
                            selectedAssignment.deadline
                          )}
                        </p>
                      </div>

                      <div>
                        <p className="text-sm font-semibold text-slate-500">
                          Published by
                        </p>

                        <p className="mt-1 font-bold">
                          {selectedVersion.publishedByName ||
                            "—"}
                        </p>
                      </div>
                    </section>

                    {selectedVersion.changeSummary && (
                      <section className="mt-5 rounded-xl border border-amber-200 bg-amber-50 p-4">
                        <h3 className="font-bold text-amber-900">
                          Change Summary
                        </h3>

                        <p className="mt-2 text-amber-900">
                          {selectedVersion.changeSummary}
                        </p>
                      </section>
                    )}

                    <section className="mt-5 rounded-xl border border-slate-200">
                      <div className="flex items-center gap-2 border-b border-slate-200 px-5 py-4">
                        <FileText className="text-blue-700" />

                        <h3 className="text-lg font-bold">
                          Complete Policy Content
                        </h3>
                      </div>

                      <div className="max-h-[45vh] overflow-y-auto whitespace-pre-wrap break-words p-5 leading-7 text-slate-700">
                        {selectedVersion.content}
                      </div>
                    </section>

                    {selectedAssignment.acknowledged ? (
                      <section className="mt-5 rounded-xl border border-green-200 bg-green-50 p-5">
                        <div className="flex items-center gap-3 text-green-800">
                          <CheckCircle2 size={23} />

                          <div>
                            <p className="font-bold">
                              Policy acknowledged
                            </p>

                            <p className="mt-1 text-sm">
                              Acknowledged at{" "}
                              {formatDateTime(
                                selectedAssignment.acknowledgedAt
                              )}
                            </p>
                          </div>
                        </div>
                      </section>
                    ) : (
                      <section className="mt-5">
                        <label className="flex cursor-pointer items-start gap-3 rounded-xl border border-blue-200 bg-blue-50 p-5">
                          <input
                            type="checkbox"
                            checked={confirmedReading}
                            onChange={(event) => {
                              setConfirmedReading(
                                event.target.checked
                              );
                              setModalError("");
                            }}
                            className="mt-1 h-5 w-5"
                          />

                          <span>
                            <span className="block font-bold text-blue-950">
                              I confirm that I have read and
                              understood this policy.
                            </span>

                            <span className="mt-1 block text-sm text-blue-800">
                              Your acknowledgement timestamp,
                              IP address and browser information
                              will be recorded.
                            </span>
                          </span>
                        </label>
                      </section>
                    )}

                    <div className="mt-6 flex flex-col-reverse justify-end gap-3 border-t border-slate-200 pt-5 sm:flex-row">
                      <button
                        type="button"
                        disabled={acknowledging}
                        onClick={closePolicy}
                        className="rounded-xl border border-slate-300 px-5 py-3 font-bold text-slate-700 hover:bg-slate-50 disabled:opacity-50"
                      >
                        Close
                      </button>

                      {!selectedAssignment.acknowledged && (
                        <button
                          type="button"
                          disabled={
                            !confirmedReading ||
                            acknowledging
                          }
                          onClick={() =>
                            void acknowledgePolicy()
                          }
                          className="inline-flex items-center justify-center gap-2 rounded-xl bg-blue-700 px-5 py-3 font-bold text-white hover:bg-blue-800 disabled:cursor-not-allowed disabled:bg-blue-300"
                        >
                          {acknowledging ? (
                            <RefreshCw
                              size={18}
                              className="animate-spin"
                            />
                          ) : (
                            <CheckCircle2 size={18} />
                          )}

                          {acknowledging
                            ? "Acknowledging..."
                            : "Acknowledge Policy"}
                        </button>
                      )}
                    </div>
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