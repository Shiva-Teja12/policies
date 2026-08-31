"use client";

import Link from "next/link";
import {
  ArrowDown,
  ArrowUp,
  Eye,
  FilePlus2,
  Pencil,
  RefreshCw,
  Search,
  Send,
  Trash2,
} from "lucide-react";
import {
  useCallback,
  useEffect,
  useState,
} from "react";

import ProtectedRoute from "@/components/ProtectedRoute";
import RetirePolicyButton from "@/components/RetirePolicyButton";
import { api, PageData } from "@/lib/api";

interface Category {
  id: number;
  name: string;
  code: string;
  description?: string;
  active: boolean;
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
  effectiveDate?: string;

  createdByName?: string;
  createdAt?: string;
  updatedAt?: string;
}

const PAGE_SIZE = 10;

function statusStyle(
  status: Policy["status"]
): string {
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

function displayLabel(value?: string): string {
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

function formatDate(value?: string | null): string {
  if (!value) {
    return "—";
  }

  const normalizedValue = value
    .trim()
    .replace(" ", "T")
    .replace(/(\.\d{3})\d+/, "$1");

  const date = new Date(normalizedValue);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return (
    date.toLocaleString("en-IN", {
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

export default function HrAdminPoliciesPage() {
  const [policies, setPolicies] = useState<Policy[]>(
    []
  );

  const [categories, setCategories] = useState<
    Category[]
  >([]);

  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");

  const [categoryId, setCategoryId] = useState("");
  const [status, setStatus] = useState("");
  const [mandatory, setMandatory] = useState("");
  const [applicability, setApplicability] =
    useState("");

  const [sortBy, setSortBy] =
    useState("updatedAt");

  const [direction, setDirection] =
    useState<"asc" | "desc">("desc");

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] =
    useState(0);

  const [totalElements, setTotalElements] =
    useState(0);

  const [loading, setLoading] = useState(true);

  const [actionId, setActionId] = useState<
    number | null
  >(null);

  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadCategories = useCallback(async () => {
    try {
      const data = await api<Category[]>(
        "/api/policy-categories"
      );

      setCategories(
        (data || []).filter(
          (category) => category.active
        )
      );
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load policy categories."
      );
    }
  }, []);

  const loadPolicies = useCallback(async () => {
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

      if (categoryId) {
        params.set("categoryId", categoryId);
      }

      if (status) {
        params.set("status", status);
      }

      if (mandatory) {
        params.set("mandatory", mandatory);
      }

      if (applicability) {
        params.set(
          "applicability",
          applicability
        );
      }

      const data = await api<PageData<Policy>>(
        `/api/policies?${params.toString()}`
      );

      setPolicies(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (requestError) {
      setPolicies([]);
      setTotalPages(0);
      setTotalElements(0);

      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load policies."
      );
    } finally {
      setLoading(false);
    }
  }, [
    applicability,
    categoryId,
    direction,
    mandatory,
    page,
    search,
    sortBy,
    status,
  ]);

  useEffect(() => {
    void loadCategories();
  }, [loadCategories]);

  useEffect(() => {
    void loadPolicies();
  }, [loadPolicies]);

  function applySearch() {
    setSearch(searchInput);
    setPage(0);
  }

  function resetFilters() {
    setSearchInput("");
    setSearch("");
    setCategoryId("");
    setStatus("");
    setMandatory("");
    setApplicability("");
    setSortBy("updatedAt");
    setDirection("desc");
    setPage(0);
  }

  function changeSort(field: string) {
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

  async function submitForReview(
    policyId: number
  ) {
    const confirmed = window.confirm(
      "Submit this policy to the Legal Reviewer?"
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
        "Policy submitted to the Legal Reviewer successfully."
      );

      await loadPolicies();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to submit the policy."
      );
    } finally {
      setActionId(null);
    }
  }

  async function deletePolicy(policy: Policy) {
    if (policy.status !== "DRAFT") {
      setError(
        "Only a policy with DRAFT status can be deleted."
      );
      return;
    }

    const confirmed = window.confirm(
      `Delete "${policy.name}"?\n\nThis action cannot be undone.`
    );

    if (!confirmed) {
      return;
    }

    setActionId(policy.id);
    setError("");
    setMessage("");

    try {
      await api(`/api/policies/${policy.id}`, {
        method: "DELETE",
      });

      setMessage(
        `${policy.name} was deleted successfully.`
      );

      /*
       * If the last policy on the current page was
       * deleted, move to the previous page.
       */
      if (policies.length === 1 && page > 0) {
        setPage((current) => current - 1);
      } else {
        await loadPolicies();
      }
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to delete the policy."
      );
    } finally {
      setActionId(null);
    }
  }

  return (
    <ProtectedRoute allowedRoles={["HR_ADMIN"]}>
      <main className="min-h-screen overflow-x-hidden bg-slate-50">
        <header className="bg-[#082b5c] text-white">
          <div className="mx-auto flex w-full max-w-7xl flex-col justify-between gap-4 px-4 py-6 sm:px-6 lg:flex-row lg:items-center">
            <div>
              <p className="text-sm text-blue-200">
                HRMS Policies Module
              </p>

              <h1 className="mt-1 text-3xl font-bold">
                Policy Management
              </h1>

              <p className="mt-2 text-sm text-blue-100">
                Create, review, publish and manage
                company policies.
              </p>
            </div>

            <Link
              href="/hr-admin/policies/create"
              className="flex w-full items-center justify-center gap-2 rounded-xl bg-blue-500 px-4 py-3 text-sm font-bold hover:bg-blue-400 sm:w-auto"
            >
              <FilePlus2 size={18} />
              Create Policy
            </Link>
          </div>
        </header>

        <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6">
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

          <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
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
                  placeholder="Search policy name or content"
                  className="min-w-0 flex-1 rounded-l-xl border border-slate-300 px-4 py-3 outline-none focus:border-blue-600"
                />

                <button
                  type="button"
                  onClick={applySearch}
                  className="flex shrink-0 items-center justify-center rounded-r-xl bg-blue-700 px-5 text-white"
                  aria-label="Search policies"
                >
                  <Search size={18} />
                </button>
              </div>

              <select
                value={categoryId}
                onChange={(event) => {
                  setCategoryId(event.target.value);
                  setPage(0);
                }}
                className="w-full min-w-0 rounded-xl border border-slate-300 px-4 py-3"
              >
                <option value="">
                  All categories
                </option>

                {categories.map((category) => (
                  <option
                    key={category.id}
                    value={category.id}
                  >
                    {category.name}
                  </option>
                ))}
              </select>

              <select
                value={status}
                onChange={(event) => {
                  setStatus(event.target.value);
                  setPage(0);
                }}
                className="w-full min-w-0 rounded-xl border border-slate-300 px-4 py-3"
              >
                <option value="">All statuses</option>
                <option value="DRAFT">Draft</option>

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

                <option value="PUBLISHED">
                  Published
                </option>

                <option value="REJECTED">
                  Rejected
                </option>

                <option value="RETIRED">
                  Retired
                </option>
              </select>

              <select
                value={applicability}
                onChange={(event) => {
                  setApplicability(event.target.value);
                  setPage(0);
                }}
                className="w-full min-w-0 rounded-xl border border-slate-300 px-4 py-3"
              >
                <option value="">
                  All applicability
                </option>

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
                value={mandatory}
                onChange={(event) => {
                  setMandatory(event.target.value);
                  setPage(0);
                }}
                className="w-full min-w-0 rounded-xl border border-slate-300 px-4 py-3"
              >
                <option value="">
                  Mandatory: All
                </option>

                <option value="true">
                  Mandatory only
                </option>

                <option value="false">
                  Optional only
                </option>
              </select>

              <select
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
                className="flex w-full items-center justify-center gap-2 rounded-xl border border-slate-300 px-4 py-3 font-semibold text-slate-700 hover:bg-slate-50"
              >
                <RefreshCw size={17} />
                Reset Filters
              </button>
            </div>
          </section>

          <section className="mt-6 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="flex flex-col justify-between gap-3 border-b border-slate-200 px-5 py-4 sm:flex-row sm:items-center">
              <div>
                <h2 className="text-lg font-bold">
                  Policies
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                  {totalElements} result
                  {totalElements === 1 ? "" : "s"}
                </p>
              </div>

              <button
                type="button"
                disabled={loading}
                onClick={() => void loadPolicies()}
                className="flex items-center justify-center gap-2 rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold disabled:opacity-50"
              >
                <RefreshCw
                  size={16}
                  className={
                    loading ? "animate-spin" : ""
                  }
                />

                Refresh
              </button>
            </div>

            <div className="w-full overflow-x-auto">
              <table className="w-full min-w-[1150px] text-left">
                <thead className="bg-slate-50 text-sm text-slate-700">
                  <tr>
                    <th className="px-5 py-4">
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

                    <th className="px-5 py-4">
                      Category
                    </th>

                    <th className="px-5 py-4">
                      Applicability
                    </th>

                    <th className="px-5 py-4">
                      Mandatory
                    </th>

                    <th className="px-5 py-4">
                      Status
                    </th>

                    <th className="px-5 py-4">
                      Updated
                    </th>

                    <th className="px-5 py-4 text-right">
                      Actions
                    </th>
                  </tr>
                </thead>

                <tbody className="divide-y divide-slate-100">
                  {loading ? (
                    <tr>
                      <td
                        colSpan={7}
                        className="px-5 py-14 text-center text-slate-500"
                      >
                        Loading policies...
                      </td>
                    </tr>
                  ) : policies.length === 0 ? (
                    <tr>
                      <td
                        colSpan={7}
                        className="px-5 py-14 text-center"
                      >
                        <p className="font-semibold text-slate-700">
                          No policies found.
                        </p>

                        <Link
                          href="/hr-admin/policies/create"
                          className="mt-3 inline-block font-bold text-blue-700"
                        >
                          Create a policy
                        </Link>
                      </td>
                    </tr>
                  ) : (
                    policies.map((policy) => (
                      <tr
                        key={policy.id}
                        className="hover:bg-slate-50"
                      >
                        <td className="px-5 py-4">
                          <p className="font-semibold text-slate-900">
                            {policy.name}
                          </p>

                          <p className="mt-1 text-sm text-slate-500">
                            {policy.code}
                          </p>
                        </td>

                        <td className="px-5 py-4">
                          <p>{policy.categoryName}</p>

                          <p className="mt-1 text-xs text-slate-400">
                            {policy.categoryCode}
                          </p>
                        </td>

                        <td className="px-5 py-4 text-sm">
                          {displayLabel(
                            policy.applicability
                          )}
                        </td>

                        <td className="px-5 py-4">
                          <span
                            className={
                              policy.mandatory
                                ? "font-bold text-red-700"
                                : "text-slate-600"
                            }
                          >
                            {policy.mandatory
                              ? "Yes"
                              : "No"}
                          </span>
                        </td>

                        <td className="px-5 py-4">
                          <span
                            className={`rounded-full px-3 py-1 text-xs font-bold ${statusStyle(
                              policy.status
                            )}`}
                          >
                            {displayLabel(
                              policy.status
                            )}
                          </span>
                        </td>

                        <td className="px-5 py-4 text-sm text-slate-600">
                          {formatDate(
                            policy.updatedAt ||
                              policy.createdAt
                          )}
                        </td>

                        <td className="px-5 py-4">
                          <div className="flex flex-wrap justify-end gap-2">
                            {/* VIEW */}
                            <Link
                              href={`/hr-admin/policies/${policy.id}`}
                              className="rounded-lg border border-slate-300 p-2 text-slate-700 hover:bg-slate-100"
                              title="View policy"
                              aria-label={`View ${policy.name}`}
                            >
                              <Eye size={18} />
                            </Link>

                            {/* EDIT */}
                            {(policy.status === "DRAFT" ||
                              policy.status ===
                                "REJECTED") && (
                              <Link
                                href={`/hr-admin/policies/${policy.id}/edit`}
                                className="rounded-lg border border-blue-300 p-2 text-blue-700 hover:bg-blue-50"
                                title="Edit policy"
                                aria-label={`Edit ${policy.name}`}
                              >
                                <Pencil size={18} />
                              </Link>
                            )}

                            {/* DELETE */}
                            {policy.status === "DRAFT" && (
                              <button
                                type="button"
                                disabled={
                                  actionId === policy.id
                                }
                                onClick={() =>
                                  void deletePolicy(policy)
                                }
                                className="rounded-lg border border-red-300 p-2 text-red-700 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
                                title="Delete draft policy"
                                aria-label={`Delete ${policy.name}`}
                              >
                                <Trash2 size={18} />
                              </button>
                            )}

                            {/* SUBMIT */}
                            {(policy.status === "DRAFT" ||
                              policy.status ===
                                "REJECTED") && (
                              <button
                                type="button"
                                disabled={
                                  actionId === policy.id
                                }
                                onClick={() =>
                                  void submitForReview(
                                    policy.id
                                  )
                                }
                                className="flex items-center gap-2 rounded-lg bg-blue-700 px-3 py-2 text-sm font-bold text-white hover:bg-blue-800 disabled:bg-blue-400"
                              >
                                <Send size={16} />

                                {actionId === policy.id
                                  ? "Working..."
                                  : "Submit"}
                              </button>
                            )}

                            {/* PUBLISH */}
                            {policy.status ===
                              "APPROVED" && (
                              <Link
                                href={`/hr-admin/ready-to-publish?policyId=${policy.id}`}
                                className="rounded-lg bg-green-700 px-3 py-2 text-sm font-bold text-white hover:bg-green-800"
                              >
                                Publish
                              </Link>
                            )}

                            {/* RETIRE */}
                            <RetirePolicyButton
                              policyId={policy.id}
                              policyName={policy.name}
                              policyCode={policy.code}
                              status={policy.status}
                            />
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            <div className="flex flex-col items-center justify-between gap-4 border-t border-slate-200 px-5 py-4 sm:flex-row">
              <span className="text-sm text-slate-500">
                Page{" "}
                {totalPages === 0 ? 0 : page + 1} of{" "}
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
                    setPage(
                      (current) => current + 1
                    )
                  }
                  className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-40"
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