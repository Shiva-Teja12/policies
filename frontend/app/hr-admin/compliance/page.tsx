"use client";

import Link from "next/link";
import {
  ArrowLeft,
  Download,
  RefreshCw,
  Search,
  Users,
  X,
} from "lucide-react";
import {
  useCallback,
  useEffect,
  useState,
} from "react";
import ProtectedRoute from "@/components/ProtectedRoute";
import {
  api,
  downloadWithToken,
  PageData,
} from "@/lib/api";

interface DepartmentCompliance {
  department: string;
  totalApplicableEmployees: number;
  acknowledgedEmployees: number;
  pendingEmployees: number;
  overdueEmployees: number;
  completionPercentage: number;
}

interface OverdueEmployee {
  employeeId: number;
  employeeName: string;
  employeeEmail: string;
  department?: string;
  deadline: string;
  daysOverdue: number;
}

interface PolicyCompliance {
  policyId: number;
  policyCode: string;
  policyName: string;
  categoryId: number;
  categoryName: string;
  mandatory: boolean;
  policyVersionId: number;
  currentVersion: number;
  effectiveDate: string;
  totalApplicableEmployees: number;
  acknowledgedEmployees: number;
  pendingEmployees: number;
  overdueEmployees: number;
  completionPercentage: number;
  departmentBreakdown: DepartmentCompliance[];
  overdueEmployeeList: OverdueEmployee[];
}

const PAGE_SIZE = 10;

function formatDate(value?: string): string {
  if (!value) {
    return "—";
  }

  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleDateString();
}

function progressColor(percentage: number): string {
  if (percentage >= 90) {
    return "bg-green-500";
  }

  if (percentage >= 70) {
    return "bg-amber-500";
  }

  return "bg-red-500";
}

export default function ComplianceDashboardPage() {
  const [records, setRecords] = useState<
    PolicyCompliance[]
  >([]);

  const [selectedRecord, setSelectedRecord] =
    useState<PolicyCompliance | null>(null);

  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");
  const [department, setDepartment] = useState("");
  const [mandatory, setMandatory] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");

  const [sortBy, setSortBy] =
    useState("policyName");

  const [direction, setDirection] =
    useState<"asc" | "desc">("asc");

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] =
    useState(0);

  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] =
    useState(false);

  const [error, setError] = useState("");

  const loadCompliance = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const params = new URLSearchParams({
        page: String(page),
        size: String(PAGE_SIZE),
        sortBy,
        direction,
      });

      if (search.trim()) {
        params.set("search", search.trim());
      }

      if (department.trim()) {
        params.set(
          "department",
          department.trim()
        );
      }

      if (mandatory) {
        params.set("mandatory", mandatory);
      }

      if (fromDate) {
        params.set("fromDate", fromDate);
      }

      if (toDate) {
        params.set("toDate", toDate);
      }

      const data =
        await api<PageData<PolicyCompliance>>(
          `/api/policies/compliance-dashboard?${params.toString()}`
        );

      setRecords(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (requestError) {
      setRecords([]);
      setTotalPages(0);
      setTotalElements(0);

      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load compliance dashboard."
      );
    } finally {
      setLoading(false);
    }
  }, [
    department,
    direction,
    fromDate,
    mandatory,
    page,
    search,
    sortBy,
    toDate,
  ]);

  useEffect(() => {
    void loadCompliance();
  }, [loadCompliance]);

  function applySearch() {
    setSearch(searchInput);
    setPage(0);
  }

  function resetFilters() {
    setSearchInput("");
    setSearch("");
    setDepartment("");
    setMandatory("");
    setFromDate("");
    setToDate("");
    setSortBy("policyName");
    setDirection("asc");
    setPage(0);
  }

  async function exportCsv() {
    setExporting(true);
    setError("");

    try {
      const params = new URLSearchParams();

      if (search.trim()) {
        params.set("search", search.trim());
      }

      if (department.trim()) {
        params.set(
          "department",
          department.trim()
        );
      }

      if (mandatory) {
        params.set("mandatory", mandatory);
      }

      if (fromDate) {
        params.set("fromDate", fromDate);
      }

      if (toDate) {
        params.set("toDate", toDate);
      }

      const query = params.toString();

      await downloadWithToken(
        `/api/policies/compliance-dashboard/export${
          query ? `?${query}` : ""
        }`,
        `policy-compliance-${
          new Date().toISOString().split("T")[0]
        }.csv`
      );
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to export compliance report."
      );
    } finally {
      setExporting(false);
    }
  }

  const pageTotalApplicable = records.reduce(
    (total, record) =>
      total + record.totalApplicableEmployees,
    0
  );

  const pageTotalAcknowledged = records.reduce(
    (total, record) =>
      total + record.acknowledgedEmployees,
    0
  );

  const pageTotalOverdue = records.reduce(
    (total, record) =>
      total + record.overdueEmployees,
    0
  );

  const pageCompletion =
    pageTotalApplicable === 0
      ? 0
      : (pageTotalAcknowledged /
          pageTotalApplicable) *
        100;

  return (
    <ProtectedRoute allowedRoles={["HR_ADMIN"]}>
      <main className="min-h-screen overflow-x-hidden bg-slate-50">
        <header className="bg-[#082b5c] text-white">
          <div className="mx-auto flex w-full max-w-7xl flex-col justify-between gap-5 px-4 py-6 sm:px-6 lg:flex-row lg:items-center">
            <div className="flex min-w-0 items-center gap-4">
              <Link
                href="/hr-admin/dashboard"
                className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-blue-300/40 hover:bg-white/10"
                aria-label="Back to dashboard"
              >
                <ArrowLeft size={20} />
              </Link>

              <div className="min-w-0">
                <p className="text-sm text-blue-200">
                  HR Admin
                </p>

                <h1 className="text-2xl font-bold sm:text-3xl">
                  Compliance Dashboard
                </h1>

                <p className="mt-1 text-sm text-blue-100">
                  Compliance is calculated from current
                  policy versions at query time.
                </p>
              </div>
            </div>

            <button
              type="button"
              disabled={exporting}
              onClick={exportCsv}
              className="flex w-full shrink-0 items-center justify-center gap-2 rounded-xl bg-white px-5 py-3 font-bold text-blue-900 hover:bg-blue-50 disabled:opacity-50 sm:w-auto"
            >
              <Download size={18} />

              {exporting
                ? "Exporting..."
                : "Export CSV"}
            </button>
          </div>
        </header>

        <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6">
          {error && (
            <div className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">
              {error}
            </div>
          )}

          <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <div className="min-w-0 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <p className="text-sm font-semibold text-slate-500">
                Matching Policies
              </p>

              <p className="mt-3 text-3xl font-bold">
                {totalElements}
              </p>
            </div>

            <div className="min-w-0 rounded-2xl border border-blue-200 bg-blue-50 p-5">
              <p className="text-sm font-semibold text-blue-700">
                Page Completion
              </p>

              <p className="mt-3 text-3xl font-bold text-blue-950">
                {pageCompletion.toFixed(1)}%
              </p>
            </div>

            <div className="min-w-0 rounded-2xl border border-green-200 bg-green-50 p-5">
              <p className="text-sm font-semibold text-green-700">
                Acknowledged
              </p>

              <p className="mt-3 text-3xl font-bold text-green-950">
                {pageTotalAcknowledged}
              </p>
            </div>

            <div className="min-w-0 rounded-2xl border border-red-200 bg-red-50 p-5">
              <p className="text-sm font-semibold text-red-700">
                Overdue
              </p>

              <p className="mt-3 text-3xl font-bold text-red-950">
                {pageTotalOverdue}
              </p>
            </div>
          </section>

          <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
              <div className="flex min-w-0 md:col-span-2">
                <input
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
                  className="flex shrink-0 items-center justify-center rounded-r-xl bg-blue-700 px-5 text-white hover:bg-blue-800"
                  aria-label="Search policies"
                >
                  <Search size={18} />
                </button>
              </div>

              <div className="min-w-0">
                <label
                  htmlFor="departmentFilter"
                  className="mb-2 block text-xs font-semibold uppercase tracking-wide text-slate-500"
                >
                  Department
                </label>

                <input
                  id="departmentFilter"
                  value={department}
                  onChange={(event) => {
                    setDepartment(event.target.value);
                    setPage(0);
                  }}
                  placeholder="Example: HR"
                  className="w-full min-w-0 rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-blue-600"
                />
              </div>

              <div className="min-w-0">
                <label
                  htmlFor="mandatoryFilter"
                  className="mb-2 block text-xs font-semibold uppercase tracking-wide text-slate-500"
                >
                  Requirement
                </label>

                <select
                  id="mandatoryFilter"
                  value={mandatory}
                  onChange={(event) => {
                    setMandatory(event.target.value);
                    setPage(0);
                  }}
                  className="w-full min-w-0 rounded-xl border border-slate-300 px-4 py-3"
                >
                  <option value="">All policies</option>
                  <option value="true">
                    Mandatory only
                  </option>
                  <option value="false">
                    Optional only
                  </option>
                </select>
              </div>

              <div className="min-w-0">
                <label
                  htmlFor="fromDateFilter"
                  className="mb-2 block text-xs font-semibold uppercase tracking-wide text-slate-500"
                >
                  Effective from
                </label>

                <input
                  id="fromDateFilter"
                  type="date"
                  value={fromDate}
                  onChange={(event) => {
                    setFromDate(event.target.value);
                    setPage(0);
                  }}
                  className="w-full min-w-0 rounded-xl border border-slate-300 px-4 py-3"
                />
              </div>

              <div className="min-w-0">
                <label
                  htmlFor="toDateFilter"
                  className="mb-2 block text-xs font-semibold uppercase tracking-wide text-slate-500"
                >
                  Effective to
                </label>

                <input
                  id="toDateFilter"
                  type="date"
                  min={fromDate || undefined}
                  value={toDate}
                  onChange={(event) => {
                    setToDate(event.target.value);
                    setPage(0);
                  }}
                  className="w-full min-w-0 rounded-xl border border-slate-300 px-4 py-3"
                />
              </div>

              <div className="min-w-0">
                <label
                  htmlFor="complianceSort"
                  className="mb-2 block text-xs font-semibold uppercase tracking-wide text-slate-500"
                >
                  Sort results
                </label>

                <select
                  id="complianceSort"
                  value={`${sortBy},${direction}`}
                  onChange={(event) => {
                    const [field, order] =
                      event.target.value.split(",");

                    setSortBy(field);
                    setDirection(
                      order as "asc" | "desc"
                    );
                    setPage(0);
                  }}
                  className="w-full min-w-0 rounded-xl border border-slate-300 px-4 py-3"
                >
                  <option value="policyName,asc">
                    Policy A–Z
                  </option>

                  <option value="policyName,desc">
                    Policy Z–A
                  </option>

                  <option value="completionPercentage,asc">
                    Lowest completion
                  </option>

                  <option value="completionPercentage,desc">
                    Highest completion
                  </option>

                  <option value="overdueEmployees,desc">
                    Most overdue
                  </option>
                </select>
              </div>

              <div className="flex min-w-0 items-end">
                <button
                  type="button"
                  onClick={resetFilters}
                  className="flex w-full items-center justify-center gap-2 rounded-xl border border-slate-300 px-4 py-3 font-semibold text-slate-700 transition hover:bg-slate-50"
                >
                  <RefreshCw size={17} />
                  Reset Filters
                </button>
              </div>
            </div>
          </section>

          <section className="mt-6 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="w-full overflow-x-auto">
              <table className="w-full min-w-[1050px] text-left">
                <thead className="bg-slate-50 text-sm text-slate-700">
                  <tr>
                    <th className="whitespace-nowrap px-5 py-4">
                      Policy
                    </th>

                    <th className="whitespace-nowrap px-5 py-4">
                      Version
                    </th>

                    <th className="whitespace-nowrap px-5 py-4">
                      Applicable
                    </th>

                    <th className="whitespace-nowrap px-5 py-4">
                      Acknowledged
                    </th>

                    <th className="whitespace-nowrap px-5 py-4">
                      Pending
                    </th>

                    <th className="whitespace-nowrap px-5 py-4">
                      Overdue
                    </th>

                    <th className="whitespace-nowrap px-5 py-4">
                      Completion
                    </th>

                    <th className="whitespace-nowrap px-5 py-4 text-right">
                      Details
                    </th>
                  </tr>
                </thead>

                <tbody className="divide-y divide-slate-100">
                  {loading ? (
                    <tr>
                      <td
                        colSpan={8}
                        className="px-5 py-14 text-center text-slate-500"
                      >
                        Loading compliance data...
                      </td>
                    </tr>
                  ) : records.length === 0 ? (
                    <tr>
                      <td
                        colSpan={8}
                        className="px-5 py-14 text-center text-slate-500"
                      >
                        No compliance records found.
                      </td>
                    </tr>
                  ) : (
                    records.map((record) => (
                      <tr
                        key={record.policyId}
                        className="hover:bg-slate-50"
                      >
                        <td className="px-5 py-4">
                          <p className="font-semibold text-slate-900">
                            {record.policyName}
                          </p>

                          <p className="mt-1 text-sm text-slate-500">
                            {record.policyCode} ·{" "}
                            {record.categoryName}
                          </p>

                          {record.mandatory && (
                            <span className="mt-2 inline-block rounded-full bg-red-100 px-2 py-1 text-xs font-bold text-red-700">
                              MANDATORY
                            </span>
                          )}
                        </td>

                        <td className="whitespace-nowrap px-5 py-4">
                          <p className="font-semibold">
                            Version{" "}
                            {record.currentVersion}
                          </p>

                          <p className="mt-1 text-xs text-slate-500">
                            {formatDate(
                              record.effectiveDate
                            )}
                          </p>
                        </td>

                        <td className="px-5 py-4">
                          {
                            record.totalApplicableEmployees
                          }
                        </td>

                        <td className="px-5 py-4 font-semibold text-green-700">
                          {record.acknowledgedEmployees}
                        </td>

                        <td className="px-5 py-4 font-semibold text-amber-700">
                          {record.pendingEmployees}
                        </td>

                        <td className="px-5 py-4 font-semibold text-red-700">
                          {record.overdueEmployees}
                        </td>

                        <td className="px-5 py-4">
                          <div className="flex min-w-[170px] items-center gap-3">
                            <div className="h-2.5 flex-1 overflow-hidden rounded-full bg-slate-200">
                              <div
                                className={`h-full ${progressColor(
                                  record.completionPercentage
                                )}`}
                                style={{
                                  width: `${Math.min(
                                    100,
                                    Math.max(
                                      0,
                                      record.completionPercentage
                                    )
                                  )}%`,
                                }}
                              />
                            </div>

                            <span className="w-14 text-right font-bold">
                              {record.completionPercentage.toFixed(
                                1
                              )}
                              %
                            </span>
                          </div>
                        </td>

                        <td className="px-5 py-4 text-right">
                          <button
                            type="button"
                            onClick={() =>
                              setSelectedRecord(record)
                            }
                            className="inline-flex items-center gap-2 rounded-lg border border-blue-300 px-3 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50"
                          >
                            <Users size={16} />
                            View
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            <div className="flex flex-col items-center justify-between gap-4 border-t border-slate-200 px-5 py-4 sm:flex-row">
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
                  className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-40"
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
                  className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-40"
                >
                  Next
                </button>
              </div>
            </div>
          </section>
        </div>

        {selectedRecord && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
            <div className="max-h-[88vh] w-full max-w-5xl overflow-y-auto rounded-2xl bg-white shadow-2xl">
              <div className="sticky top-0 z-10 flex items-start justify-between border-b border-slate-200 bg-white p-6">
                <div className="min-w-0">
                  <h2 className="truncate text-xl font-bold text-slate-900">
                    {selectedRecord.policyName}
                  </h2>

                  <p className="mt-1 text-sm text-slate-500">
                    Department compliance and overdue
                    employees
                  </p>
                </div>

                <button
                  type="button"
                  onClick={() =>
                    setSelectedRecord(null)
                  }
                  className="ml-4 shrink-0 rounded-lg p-2 hover:bg-slate-100"
                  aria-label="Close details"
                >
                  <X size={20} />
                </button>
              </div>

              <div className="p-6">
                <h3 className="font-bold text-slate-900">
                  Department Breakdown
                </h3>

                <div className="mt-3 overflow-x-auto rounded-xl border border-slate-200">
                  <table className="w-full min-w-[750px] text-left text-sm">
                    <thead className="bg-slate-50">
                      <tr>
                        <th className="px-4 py-3">
                          Department
                        </th>

                        <th className="px-4 py-3">
                          Applicable
                        </th>

                        <th className="px-4 py-3">
                          Acknowledged
                        </th>

                        <th className="px-4 py-3">
                          Pending
                        </th>

                        <th className="px-4 py-3">
                          Overdue
                        </th>

                        <th className="px-4 py-3">
                          Completion
                        </th>
                      </tr>
                    </thead>

                    <tbody className="divide-y divide-slate-100">
                      {selectedRecord
                        .departmentBreakdown?.length >
                      0 ? (
                        selectedRecord.departmentBreakdown.map(
                          (item) => (
                            <tr key={item.department}>
                              <td className="px-4 py-3 font-semibold">
                                {item.department}
                              </td>

                              <td className="px-4 py-3">
                                {
                                  item.totalApplicableEmployees
                                }
                              </td>

                              <td className="px-4 py-3">
                                {
                                  item.acknowledgedEmployees
                                }
                              </td>

                              <td className="px-4 py-3">
                                {item.pendingEmployees}
                              </td>

                              <td className="px-4 py-3">
                                {item.overdueEmployees}
                              </td>

                              <td className="px-4 py-3 font-bold">
                                {item.completionPercentage.toFixed(
                                  1
                                )}
                                %
                              </td>
                            </tr>
                          )
                        )
                      ) : (
                        <tr>
                          <td
                            colSpan={6}
                            className="px-4 py-8 text-center text-slate-500"
                          >
                            No department breakdown
                            available.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>

                <h3 className="mt-8 font-bold text-slate-900">
                  Overdue Employees
                </h3>

                <div className="mt-3 overflow-x-auto rounded-xl border border-slate-200">
                  <table className="w-full min-w-[750px] text-left text-sm">
                    <thead className="bg-slate-50">
                      <tr>
                        <th className="px-4 py-3">
                          Employee
                        </th>

                        <th className="px-4 py-3">
                          Email
                        </th>

                        <th className="px-4 py-3">
                          Department
                        </th>

                        <th className="px-4 py-3">
                          Deadline
                        </th>

                        <th className="px-4 py-3">
                          Days Overdue
                        </th>
                      </tr>
                    </thead>

                    <tbody className="divide-y divide-slate-100">
                      {selectedRecord.overdueEmployeeList
                        ?.length > 0 ? (
                        selectedRecord.overdueEmployeeList.map(
                          (employee) => (
                            <tr key={employee.employeeId}>
                              <td className="px-4 py-3 font-semibold">
                                {employee.employeeName}
                              </td>

                              <td className="px-4 py-3">
                                {employee.employeeEmail}
                              </td>

                              <td className="px-4 py-3">
                                {employee.department || "—"}
                              </td>

                              <td className="px-4 py-3">
                                {formatDate(
                                  employee.deadline
                                )}
                              </td>

                              <td className="px-4 py-3 font-bold text-red-700">
                                {employee.daysOverdue}
                              </td>
                            </tr>
                          )
                        )
                      ) : (
                        <tr>
                          <td
                            colSpan={5}
                            className="px-4 py-8 text-center text-slate-500"
                          >
                            No overdue employees.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>
    </ProtectedRoute>
  );
}