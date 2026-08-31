"use client";

import Link from "next/link";
import {
  ArrowLeft,
  CalendarDays,
  RefreshCw,
  Rocket,
  Search,
  X,
} from "lucide-react";
import { useSearchParams } from "next/navigation";
import {
  FormEvent,
  Suspense,
  useCallback,
  useEffect,
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
  content: string;
  applicability:
    | "ALL"
    | "GRADE_BASED"
    | "DEPT_BASED";
  applicableDepartments: string[];
  applicableGrades: string[];
  mandatory: boolean;
  status: string;
  acknowledgementPeriodDays: number;
  onboardingPeriodDays: number;
  publishedOnce: boolean;
  effectiveDate?: string;
  updatedAt?: string;
}

interface PublishedVersion {
  id: number;
  policyId: number;
  policyCode: string;
  policyName: string;
  versionNumber: number;
  effectiveDate: string;
  publishedAt: string;
  publishedByName?: string;
  changeSummary?: string;
  currentVersion: boolean;
  archived: boolean;
  assignedEmployees?: number;
}

const PAGE_SIZE = 10;

function currentDate(): string {
  const date = new Date();
  const timezoneOffset = date.getTimezoneOffset();

  const localDate = new Date(
    date.getTime() - timezoneOffset * 60_000
  );

  return localDate.toISOString().split("T")[0];
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

function ReadyToPublishContent() {
  const searchParams = useSearchParams();

  const requestedPolicyId = Number(
    searchParams.get("policyId")
  );

  const [policies, setPolicies] = useState<Policy[]>([]);
  const [selectedPolicy, setSelectedPolicy] =
    useState<Policy | null>(null);

  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");
  const [mandatory, setMandatory] = useState("");
  const [applicability, setApplicability] =
    useState("");

  const [sortBy, setSortBy] = useState("updatedAt");
  const [direction, setDirection] =
    useState<"asc" | "desc">("desc");

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] =
    useState(0);

  const [effectiveDate, setEffectiveDate] =
    useState(currentDate());

  const [changeSummary, setChangeSummary] =
    useState("");

  const [
    acknowledgementPeriodDays,
    setAcknowledgementPeriodDays,
  ] = useState(7);

  const [loading, setLoading] = useState(true);
  const [publishing, setPublishing] =
    useState(false);

  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadApprovedPolicies = useCallback(
    async () => {
      setLoading(true);
      setError("");

      try {
        const params = new URLSearchParams({
          status: "APPROVED",
          page: String(page),
          size: String(PAGE_SIZE),
          sortBy,
          direction,
        });

        if (search.trim()) {
          params.set("search", search.trim());
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

        if (
          Number.isInteger(requestedPolicyId) &&
          requestedPolicyId > 0 &&
          !selectedPolicy
        ) {
          const matchingPolicy = data.content.find(
            (policy) =>
              policy.id === requestedPolicyId
          );

          if (matchingPolicy) {
            setSelectedPolicy(matchingPolicy);

            setAcknowledgementPeriodDays(
              matchingPolicy.acknowledgementPeriodDays ||
                7
            );
          } else {
            try {
              const requestedPolicy =
                await api<Policy>(
                  `/api/policies/${requestedPolicyId}`
                );

              if (
                requestedPolicy.status === "APPROVED"
              ) {
                setSelectedPolicy(
                  requestedPolicy
                );

                setAcknowledgementPeriodDays(
                  requestedPolicy.acknowledgementPeriodDays ||
                    7
                );
              }
            } catch {
              // The requested policy was unavailable.
            }
          }
        }
      } catch (requestError) {
        setPolicies([]);
        setTotalPages(0);
        setTotalElements(0);

        setError(
          requestError instanceof Error
            ? requestError.message
            : "Unable to load approved policies."
        );
      } finally {
        setLoading(false);
      }
    },
    [
      applicability,
      direction,
      mandatory,
      page,
      requestedPolicyId,
      search,
      selectedPolicy,
      sortBy,
    ]
  );

  useEffect(() => {
    void loadApprovedPolicies();
  }, [loadApprovedPolicies]);

  function applySearch() {
    setSearch(searchInput);
    setPage(0);
  }

  function resetFilters() {
    setSearch("");
    setSearchInput("");
    setMandatory("");
    setApplicability("");
    setSortBy("updatedAt");
    setDirection("desc");
    setPage(0);
  }

  function openPublishDialog(policy: Policy) {
    setSelectedPolicy(policy);
    setEffectiveDate(currentDate());
    setChangeSummary("");

    setAcknowledgementPeriodDays(
      policy.acknowledgementPeriodDays || 7
    );

    setError("");
  }

  function closePublishDialog() {
    if (!publishing) {
      setSelectedPolicy(null);
      setError("");
    }
  }

  async function publishPolicy(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    if (!selectedPolicy) {
      return;
    }

    if (!effectiveDate) {
      setError("Effective date is required.");
      return;
    }

    if (
      acknowledgementPeriodDays < 1 ||
      acknowledgementPeriodDays > 365
    ) {
      setError(
        "Acknowledgement period must be between 1 and 365 days."
      );
      return;
    }

    const confirmed = window.confirm(
      `Publish "${selectedPolicy.name}"? A new version will always be created.`
    );

    if (!confirmed) {
      return;
    }

    setPublishing(true);
    setError("");
    setMessage("");

    try {
      const version =
        await api<PublishedVersion>(
          `/api/policies/${selectedPolicy.id}/publish`,
          {
            method: "POST",
            body: JSON.stringify({
              effectiveDate,
              changeSummary:
                changeSummary.trim() || null,
              acknowledgementPeriodDays,
            }),
          }
        );

      setMessage(
        `${selectedPolicy.name} published successfully as Version ${version.versionNumber}. ${version.assignedEmployees ?? 0} employee assignment(s) were created.`
      );

      setSelectedPolicy(null);

      await loadApprovedPolicies();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to publish policy."
      );
    } finally {
      setPublishing(false);
    }
  }

  return (
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
              Ready to Publish
            </h1>

            <p className="mt-1 text-sm text-blue-100">
              Publish policies that completed every required
              approval.
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

        <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-[2fr_1fr_1fr_1fr_auto]">
            <div className="flex">
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
                placeholder="Search approved policies"
                className="min-w-0 flex-1 rounded-l-xl border border-slate-300 px-4 py-3"
              />

              <button
                type="button"
                onClick={applySearch}
                className="rounded-r-xl bg-blue-700 px-4 text-white"
              >
                <Search size={18} />
              </button>
            </div>

            <select
              value={applicability}
              onChange={(event) => {
                setApplicability(event.target.value);
                setPage(0);
              }}
              className="rounded-xl border border-slate-300 px-3 py-3"
            >
              <option value="">All applicability</option>
              <option value="ALL">All employees</option>
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
              className="rounded-xl border border-slate-300 px-3 py-3"
            >
              <option value="">Mandatory: All</option>
              <option value="true">Mandatory</option>
              <option value="false">Optional</option>
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
              className="rounded-xl border border-slate-300 px-3 py-3"
            >
              <option value="updatedAt,desc">
                Recently approved
              </option>
              <option value="name,asc">
                Name A–Z
              </option>
              <option value="name,desc">
                Name Z–A
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
          <div className="flex justify-between border-b px-5 py-4">
            <div>
              <h2 className="font-bold">
                Approved Policies
              </h2>

              <p className="mt-1 text-sm text-slate-500">
                {totalElements} policy
                {totalElements === 1 ? "" : "ies"} ready
              </p>
            </div>

            <Rocket className="text-green-700" />
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left">
              <thead className="bg-slate-50 text-sm">
                <tr>
                  <th className="px-5 py-3">Policy</th>
                  <th className="px-5 py-3">Category</th>
                  <th className="px-5 py-3">
                    Applicability
                  </th>
                  <th className="px-5 py-3">
                    Mandatory
                  </th>
                  <th className="px-5 py-3">
                    Deadline
                  </th>
                  <th className="px-5 py-3 text-right">
                    Action
                  </th>
                </tr>
              </thead>

              <tbody className="divide-y">
                {loading ? (
                  <tr>
                    <td
                      colSpan={6}
                      className="p-12 text-center text-slate-500"
                    >
                      Loading approved policies...
                    </td>
                  </tr>
                ) : policies.length === 0 ? (
                  <tr>
                    <td
                      colSpan={6}
                      className="p-12 text-center"
                    >
                      <Rocket
                        size={40}
                        className="mx-auto text-slate-300"
                      />

                      <p className="mt-3 font-semibold">
                        No policies are ready to publish.
                      </p>

                      <p className="mt-1 text-sm text-slate-500">
                        Approved policies will appear here.
                      </p>
                    </td>
                  </tr>
                ) : (
                  policies.map((policy) => (
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

                      <td className="px-5 py-4">
                        {policy.applicability.replaceAll(
                          "_",
                          " "
                        )}
                      </td>

                      <td className="px-5 py-4">
                        {policy.mandatory ? "Yes" : "No"}
                      </td>

                      <td className="px-5 py-4">
                        {
                          policy.acknowledgementPeriodDays
                        }{" "}
                        days
                      </td>

                      <td className="px-5 py-4 text-right">
                        <button
                          type="button"
                          onClick={() =>
                            openPublishDialog(policy)
                          }
                          className="inline-flex items-center gap-2 rounded-lg bg-green-700 px-4 py-2 font-bold text-white"
                        >
                          <Rocket size={17} />
                          Publish
                        </button>
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

      {selectedPolicy && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-xl rounded-2xl bg-white shadow-2xl">
            <div className="flex items-start justify-between border-b p-6">
              <div>
                <p className="text-sm font-semibold text-green-700">
                  Publish Approved Policy
                </p>

                <h2 className="mt-1 text-xl font-bold">
                  {selectedPolicy.name}
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                  {selectedPolicy.code}
                </p>
              </div>

              <button
                type="button"
                disabled={publishing}
                onClick={closePublishDialog}
                className="rounded-lg p-2 hover:bg-slate-100"
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={publishPolicy} className="p-6">
              {error && (
                <div className="mb-5 rounded-xl bg-red-50 p-4 text-red-700">
                  {error}
                </div>
              )}

              <label className="block text-sm font-semibold">
                Effective date *
              </label>

              <div className="relative mt-2">
                <CalendarDays
                  size={18}
                  className="absolute left-4 top-3.5 text-slate-400"
                />

                <input
                  required
                  type="date"
                  min={currentDate()}
                  value={effectiveDate}
                  onChange={(event) =>
                    setEffectiveDate(
                      event.target.value
                    )
                  }
                  className="w-full rounded-xl border py-3 pl-11 pr-4"
                />
              </div>

              <label className="mt-5 block text-sm font-semibold">
                Acknowledgement deadline
              </label>

              <div className="mt-2 flex items-center gap-3">
                <input
                  type="number"
                  min={1}
                  max={365}
                  value={acknowledgementPeriodDays}
                  onChange={(event) =>
                    setAcknowledgementPeriodDays(
                      Number(event.target.value)
                    )
                  }
                  className="w-full rounded-xl border px-4 py-3"
                />

                <span className="text-sm text-slate-500">
                  days
                </span>
              </div>

              <label className="mt-5 block text-sm font-semibold">
                Change summary
              </label>

              <textarea
                rows={5}
                maxLength={1000}
                value={changeSummary}
                onChange={(event) =>
                  setChangeSummary(event.target.value)
                }
                placeholder="Describe what changed in this version."
                className="mt-2 w-full rounded-xl border px-4 py-3"
              />

              <p className="mt-1 text-right text-xs text-slate-500">
                {changeSummary.length}/1000
              </p>

              <div className="mt-5 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
                Publishing always creates a new sequential
                version. Previous versions and
                acknowledgements remain in history.
              </div>

              <div className="mt-6 flex justify-end gap-3">
                <button
                  type="button"
                  disabled={publishing}
                  onClick={closePublishDialog}
                  className="rounded-xl border px-5 py-3 font-semibold"
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  disabled={publishing}
                  className="flex items-center gap-2 rounded-xl bg-green-700 px-5 py-3 font-bold text-white disabled:bg-green-400"
                >
                  <Rocket size={18} />

                  {publishing
                    ? "Publishing..."
                    : "Publish Policy"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </main>
  );
}

export default function ReadyToPublishPage() {
  return (
    <ProtectedRoute allowedRoles={["HR_ADMIN"]}>
      <Suspense
        fallback={
          <div className="flex min-h-screen items-center justify-center bg-slate-50">
            Loading approved policies...
          </div>
        }
      >
        <ReadyToPublishContent />
      </Suspense>
    </ProtectedRoute>
  );
}