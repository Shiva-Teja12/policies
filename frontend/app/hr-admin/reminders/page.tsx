"use client";

import Link from "next/link";
import {
  ArrowLeft,
  BellRing,
  CalendarClock,
  Play,
  RefreshCw,
  Search,
} from "lucide-react";
import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";
import ProtectedRoute from "@/components/ProtectedRoute";
import { api, PageData } from "@/lib/api";

interface ReminderLog {
  id: number;
  employeeId: number;
  employeeName: string;
  employeeEmail: string;
  policyId: number;
  policyCode: string;
  policyName: string;
  reminderStage:
    | "DAY_3_FIRST_REMINDER"
    | "DAY_7_SECOND_REMINDER"
    | "DAY_10_MANAGER_CC"
    | "DAY_14_HR_HEAD_ESCALATION";
  deliveryStatus:
    | "PENDING"
    | "SENT"
    | "FAILED"
    | "READ";
  recipientEmail: string;
  ccEmail?: string;
  sentAt?: string;
  failureReason?: string;
}

const PAGE_SIZE = 10;

function formatDate(value?: string): string {
  if (!value) {
    return "—";
  }

  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString();
}

function stageLabel(stage: ReminderLog["reminderStage"]) {
  switch (stage) {
    case "DAY_3_FIRST_REMINDER":
      return "Day 3 – First Reminder";

    case "DAY_7_SECOND_REMINDER":
      return "Day 7 – Second Reminder";

    case "DAY_10_MANAGER_CC":
      return "Day 10 – Manager CC";

    case "DAY_14_HR_HEAD_ESCALATION":
      return "Day 14 – HR Head Escalation";

    default:
      return stage;
  }
}

function stageStyle(stage: ReminderLog["reminderStage"]) {
  switch (stage) {
    case "DAY_14_HR_HEAD_ESCALATION":
      return "bg-red-100 text-red-800";

    case "DAY_10_MANAGER_CC":
      return "bg-orange-100 text-orange-800";

    case "DAY_7_SECOND_REMINDER":
      return "bg-amber-100 text-amber-800";

    default:
      return "bg-blue-100 text-blue-800";
  }
}

function deliveryStyle(
  status: ReminderLog["deliveryStatus"]
) {
  switch (status) {
    case "SENT":
      return "bg-green-100 text-green-800";

    case "FAILED":
      return "bg-red-100 text-red-800";

    case "READ":
      return "bg-blue-100 text-blue-800";

    default:
      return "bg-slate-100 text-slate-700";
  }
}

export default function RemindersPage() {
  const [logs, setLogs] = useState<ReminderLog[]>([]);

  const [search, setSearch] = useState("");
  const [employeeId, setEmployeeId] = useState("");
  const [policyId, setPolicyId] = useState("");
  const [stageFilter, setStageFilter] = useState("");
  const [statusFilter, setStatusFilter] =
    useState("");

  const [direction, setDirection] =
    useState<"asc" | "desc">("desc");

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] =
    useState(0);

  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);

  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadReminders = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const params = new URLSearchParams({
        page: String(page),
        size: String(PAGE_SIZE),
        direction,
      });

      if (employeeId) {
        params.set("employeeId", employeeId);
      }

      if (policyId) {
        params.set("policyId", policyId);
      }

      const data = await api<PageData<ReminderLog>>(
        `/api/policies/reminders?${params.toString()}`
      );

      setLogs(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (requestError) {
      setLogs([]);
      setTotalPages(0);
      setTotalElements(0);

      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load reminder history."
      );
    } finally {
      setLoading(false);
    }
  }, [direction, employeeId, page, policyId]);

  useEffect(() => {
    void loadReminders();
  }, [loadReminders]);

  const visibleLogs = useMemo(() => {
    const query = search.trim().toLowerCase();

    return logs.filter((log) => {
      const searchableText = [
        log.policyName,
        log.policyCode,
        log.employeeName,
        log.employeeEmail,
        log.recipientEmail,
        log.ccEmail,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();

      const matchesSearch =
        !query || searchableText.includes(query);

      const matchesStage =
        !stageFilter ||
        log.reminderStage === stageFilter;

      const matchesStatus =
        !statusFilter ||
        log.deliveryStatus === statusFilter;

      return (
        matchesSearch &&
        matchesStage &&
        matchesStatus
      );
    });
  }, [logs, search, stageFilter, statusFilter]);

  const sentCount = logs.filter(
    (log) => log.deliveryStatus === "SENT"
  ).length;

  const failedCount = logs.filter(
    (log) => log.deliveryStatus === "FAILED"
  ).length;

  const escalatedCount = logs.filter((log) =>
    [
      "DAY_10_MANAGER_CC",
      "DAY_14_HR_HEAD_ESCALATION",
    ].includes(log.reminderStage)
  ).length;

  function resetFilters() {
    setSearch("");
    setEmployeeId("");
    setPolicyId("");
    setStageFilter("");
    setStatusFilter("");
    setDirection("desc");
    setPage(0);
  }

  async function runRemindersManually() {
    const confirmed = window.confirm(
      "Run the overdue-policy reminder process now?"
    );

    if (!confirmed) {
      return;
    }

    setRunning(true);
    setError("");
    setMessage("");

    try {
      await api("/api/policies/reminders/run", {
        method: "POST",
      });

      setMessage(
        "Reminder process completed successfully."
      );

      setPage(0);
      await loadReminders();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to run reminders."
      );
    } finally {
      setRunning(false);
    }
  }

  return (
    <ProtectedRoute allowedRoles={["HR_ADMIN"]}>
      <main className="min-h-screen bg-slate-50">
        <header className="bg-[#082b5c] text-white">
          <div className="mx-auto flex max-w-7xl flex-col justify-between gap-4 px-5 py-6 sm:flex-row sm:items-center">
            <div className="flex items-center gap-4">
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
                  Reminder Monitoring
                </h1>

                <p className="mt-1 text-sm text-blue-100">
                  Monitor automatic acknowledgement
                  reminders and escalations.
                </p>
              </div>
            </div>

            <div className="flex gap-3">
              <button
                type="button"
                disabled={loading}
                onClick={() =>
                  void loadReminders()
                }
                className="flex items-center gap-2 rounded-xl border border-blue-300/40 px-4 py-2.5 font-semibold hover:bg-white/10 disabled:opacity-50"
              >
                <RefreshCw
                  size={18}
                  className={loading ? "animate-spin" : ""}
                />
                Refresh
              </button>

              <button
                type="button"
                disabled={running}
                onClick={runRemindersManually}
                className="flex items-center gap-2 rounded-xl bg-blue-500 px-4 py-2.5 font-bold hover:bg-blue-400 disabled:opacity-50"
              >
                <Play size={18} />

                {running ? "Running..." : "Run Now"}
              </button>
            </div>
          </div>
        </header>

        <div className="mx-auto max-w-7xl px-5 py-8">
          {message && (
            <div className="mb-5 rounded-xl border border-green-200 bg-green-50 p-4 text-green-800">
              {message}
            </div>
          )}

          {error && (
            <div className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">
              {error}
            </div>
          )}

          <section className="rounded-2xl border border-blue-200 bg-blue-50 p-5">
            <div className="flex items-start gap-4">
              <CalendarClock
                size={28}
                className="mt-1 shrink-0 text-blue-700"
              />

              <div>
                <h2 className="font-bold text-blue-950">
                  Daily scheduler: 09:00 IST
                </h2>

                <p className="mt-2 text-sm leading-6 text-blue-800">
                  Day 3 sends the first reminder. Day 7
                  sends the second reminder. Day 10 copies
                  the employee&apos;s manager. Day 14
                  escalates to the HR Head.
                </p>
              </div>
            </div>
          </section>

          <section className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-2xl border bg-white p-5 shadow-sm">
              <BellRing className="text-blue-700" />

              <p className="mt-4 text-3xl font-bold">
                {totalElements}
              </p>

              <p className="text-sm text-slate-500">
                Total reminder logs
              </p>
            </div>

            <div className="rounded-2xl border border-green-200 bg-green-50 p-5">
              <p className="text-sm font-semibold text-green-700">
                Sent on this page
              </p>

              <p className="mt-3 text-3xl font-bold text-green-950">
                {sentCount}
              </p>
            </div>

            <div className="rounded-2xl border border-red-200 bg-red-50 p-5">
              <p className="text-sm font-semibold text-red-700">
                Failed on this page
              </p>

              <p className="mt-3 text-3xl font-bold text-red-950">
                {failedCount}
              </p>
            </div>

            <div className="rounded-2xl border border-orange-200 bg-orange-50 p-5">
              <p className="text-sm font-semibold text-orange-700">
                Escalated on this page
              </p>

              <p className="mt-3 text-3xl font-bold text-orange-950">
                {escalatedCount}
              </p>
            </div>
          </section>

          <section className="mt-6 rounded-2xl border bg-white p-5 shadow-sm">
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-[2fr_1fr_1fr_1fr_1fr_1fr_auto]">
              <div className="relative">
                <Search
                  size={18}
                  className="absolute left-4 top-3.5 text-slate-400"
                />

                <input
                  value={search}
                  onChange={(event) =>
                    setSearch(event.target.value)
                  }
                  placeholder="Search policy or employee"
                  className="w-full rounded-xl border py-3 pl-11 pr-4"
                />
              </div>

              <input
                type="number"
                min={1}
                value={employeeId}
                onChange={(event) => {
                  setEmployeeId(event.target.value);
                  setPage(0);
                }}
                placeholder="Employee ID"
                className="rounded-xl border px-3 py-3"
              />

              <input
                type="number"
                min={1}
                value={policyId}
                onChange={(event) => {
                  setPolicyId(event.target.value);
                  setPage(0);
                }}
                placeholder="Policy ID"
                className="rounded-xl border px-3 py-3"
              />

              <select
                value={stageFilter}
                onChange={(event) =>
                  setStageFilter(event.target.value)
                }
                className="rounded-xl border px-3 py-3"
              >
                <option value="">All stages</option>

                <option value="DAY_3_FIRST_REMINDER">
                  Day 3
                </option>

                <option value="DAY_7_SECOND_REMINDER">
                  Day 7
                </option>

                <option value="DAY_10_MANAGER_CC">
                  Day 10
                </option>

                <option value="DAY_14_HR_HEAD_ESCALATION">
                  Day 14
                </option>
              </select>

              <select
                value={statusFilter}
                onChange={(event) =>
                  setStatusFilter(event.target.value)
                }
                className="rounded-xl border px-3 py-3"
              >
                <option value="">
                  All delivery statuses
                </option>

                <option value="PENDING">
                  Pending
                </option>

                <option value="SENT">Sent</option>
                <option value="FAILED">Failed</option>
                <option value="READ">Read</option>
              </select>

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
                className="rounded-xl border px-3 py-3"
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
                onClick={resetFilters}
                className="flex items-center justify-center gap-2 rounded-xl border px-4 py-3 font-semibold"
              >
                <RefreshCw size={17} />
                Reset
              </button>
            </div>
          </section>

          <section className="mt-6 overflow-hidden rounded-2xl border bg-white shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[1150px] text-left">
                <thead className="bg-slate-50 text-sm">
                  <tr>
                    <th className="px-5 py-3">
                      Policy
                    </th>

                    <th className="px-5 py-3">
                      Employee
                    </th>

                    <th className="px-5 py-3">
                      Reminder Stage
                    </th>

                    <th className="px-5 py-3">
                      Recipient
                    </th>

                    <th className="px-5 py-3">
                      CC
                    </th>

                    <th className="px-5 py-3">
                      Status
                    </th>

                    <th className="px-5 py-3">
                      Sent At
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
                        Loading reminder history...
                      </td>
                    </tr>
                  ) : visibleLogs.length === 0 ? (
                    <tr>
                      <td
                        colSpan={7}
                        className="p-12 text-center text-slate-500"
                      >
                        No reminder logs found.
                      </td>
                    </tr>
                  ) : (
                    visibleLogs.map((log) => (
                      <tr
                        key={log.id}
                        className="hover:bg-slate-50"
                      >
                        <td className="px-5 py-4">
                          <p className="font-semibold">
                            {log.policyName}
                          </p>

                          <p className="mt-1 text-sm text-slate-500">
                            {log.policyCode} · ID{" "}
                            {log.policyId}
                          </p>
                        </td>

                        <td className="px-5 py-4">
                          <p className="font-semibold">
                            {log.employeeName}
                          </p>

                          <p className="mt-1 text-sm text-slate-500">
                            {log.employeeEmail} · ID{" "}
                            {log.employeeId}
                          </p>
                        </td>

                        <td className="px-5 py-4">
                          <span
                            className={`rounded-full px-3 py-1 text-xs font-bold ${stageStyle(
                              log.reminderStage
                            )}`}
                          >
                            {stageLabel(
                              log.reminderStage
                            )}
                          </span>
                        </td>

                        <td className="px-5 py-4 text-sm">
                          {log.recipientEmail}
                        </td>

                        <td className="px-5 py-4 text-sm">
                          {log.ccEmail || "—"}
                        </td>

                        <td className="px-5 py-4">
                          <span
                            className={`rounded-full px-3 py-1 text-xs font-bold ${deliveryStyle(
                              log.deliveryStatus
                            )}`}
                          >
                            {log.deliveryStatus}
                          </span>

                          {log.failureReason && (
                            <p className="mt-2 max-w-xs text-xs text-red-700">
                              {log.failureReason}
                            </p>
                          )}
                        </td>

                        <td className="px-5 py-4 text-sm text-slate-600">
                          {formatDate(log.sentAt)}
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
                  disabled={page === 0 || loading}
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
                    loading ||
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
      </main>
    </ProtectedRoute>
  );
}