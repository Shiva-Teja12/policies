"use client";

import {
  Bell,
  BookOpen,
  RefreshCw,
  Search,
} from "lucide-react";
import Link from "next/link";
import {
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
  categoryName: string;
  content: string;
  applicability: string;
  mandatory: boolean;
  status: string;
  effectiveDate?: string;
  updatedAt?: string;
}

interface Notification {
  id: number;
  policyId?: number;
  policyName?: string;
  title: string;
  message: string;
  status: string;
  createdAt: string;
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

export default function ManagerDashboard() {
  const [policies, setPolicies] = useState<Policy[]>([]);
  const [notifications, setNotifications] = useState<
    Notification[]
  >([]);

  const [search, setSearch] = useState("");
  const [mandatory, setMandatory] = useState("");
  const [applicability, setApplicability] = useState("");
  const [sortBy, setSortBy] = useState("name");
  const [direction, setDirection] =
    useState<"asc" | "desc">("asc");

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [unreadCount, setUnreadCount] = useState(0);

  const [selectedPolicy, setSelectedPolicy] =
    useState<Policy | null>(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const params = new URLSearchParams({
        status: "PUBLISHED",
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
        params.set("applicability", applicability);
      }

      const [
        policyPage,
        notificationPage,
        unreadResponse,
      ] = await Promise.all([
        api<PageData<Policy>>(
          `/api/policies?${params.toString()}`
        ),

        api<PageData<Notification>>(
          "/api/notifications?page=0&size=5&sortBy=createdAt&direction=desc"
        ),

        api<{ unreadCount: number }>(
          "/api/notifications/unread-count"
        ),
      ]);

      setPolicies(policyPage.content || []);
      setTotalPages(policyPage.totalPages || 0);
      setTotalElements(policyPage.totalElements || 0);
      setNotifications(notificationPage.content || []);
      setUnreadCount(unreadResponse.unreadCount || 0);
    } catch (requestError) {
      setPolicies([]);
      setNotifications([]);

      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load manager dashboard."
      );
    } finally {
      setLoading(false);
    }
  }, [
    applicability,
    direction,
    mandatory,
    page,
    search,
    sortBy,
  ]);

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      void loadDashboard();
    }, 250);

    return () => window.clearTimeout(timeout);
  }, [loadDashboard]);

  function resetFilters() {
    setSearch("");
    setMandatory("");
    setApplicability("");
    setSortBy("name");
    setDirection("asc");
    setPage(0);
  }

  return (
    <ProtectedRoute allowedRoles={["MANAGER"]}>
      <main className="min-h-screen bg-slate-50">
        <header className="bg-[#082b5c] text-white">
          <div className="mx-auto flex max-w-7xl items-center justify-between gap-5 px-5 py-7">
            <div>
              <p className="text-sm text-blue-200">
                Manager Portal
              </p>

              <h1 className="mt-1 text-3xl font-bold">
                Policy Dashboard
              </h1>

              <p className="mt-2 text-blue-100">
                Browse currently published company policies
                and view your notifications.
              </p>
            </div>

            <div className="flex gap-3">
              <Link
                href="/notifications"
                className="relative rounded-xl border border-blue-300/40 p-3 hover:bg-white/10"
                aria-label="Notifications"
              >
                <Bell size={20} />

                {unreadCount > 0 && (
                  <span className="absolute -right-2 -top-2 flex h-6 min-w-6 items-center justify-center rounded-full bg-red-500 px-1 text-xs font-bold">
                    {unreadCount > 99 ? "99+" : unreadCount}
                  </span>
                )}
              </Link>

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
            </div>
          </div>
        </header>

        <div className="mx-auto max-w-7xl px-5 py-8">
          {error && (
            <div className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">
              {error}
            </div>
          )}

          <section className="grid gap-4 sm:grid-cols-3">
            <div className="rounded-2xl border border-blue-200 bg-blue-50 p-5">
              <BookOpen className="text-blue-700" />

              <p className="mt-4 text-3xl font-bold text-blue-950">
                {totalElements}
              </p>

              <p className="text-sm font-semibold text-blue-700">
                Published policies
              </p>
            </div>

            <div className="rounded-2xl border border-amber-200 bg-amber-50 p-5">
              <Bell className="text-amber-700" />

              <p className="mt-4 text-3xl font-bold text-amber-950">
                {unreadCount}
              </p>

              <p className="text-sm font-semibold text-amber-700">
                Unread notifications
              </p>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-5">
              <p className="text-sm font-semibold text-slate-500">
                Manager access
              </p>

              <p className="mt-4 font-bold text-slate-900">
                Published Policies
              </p>

              <p className="mt-1 text-sm text-slate-500">
                No HR Admin or approval permissions
              </p>
            </div>
          </section>

          <section className="mt-6 grid gap-6 xl:grid-cols-[2fr_1fr]">
            <div>
              <section className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:grid-cols-2 lg:grid-cols-[2fr_1fr_1fr_1fr_auto]">
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
                    placeholder="Search published policies"
                    className="w-full rounded-xl border border-slate-300 py-3 pl-11 pr-4"
                  />
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
                  <option value="">All requirements</option>
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
                  <option value="name,asc">Name A–Z</option>
                  <option value="name,desc">Name Z–A</option>
                  <option value="updatedAt,desc">
                    Recently updated
                  </option>
                </select>

                <button
                  type="button"
                  onClick={resetFilters}
                  className="rounded-xl border border-slate-300 px-4 py-3 font-semibold"
                >
                  Reset
                </button>
              </section>

              <section className="mt-5 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                <div className="divide-y divide-slate-100">
                  {loading ? (
                    <div className="p-12 text-center text-slate-500">
                      Loading published policies...
                    </div>
                  ) : policies.length === 0 ? (
                    <div className="p-12 text-center text-slate-500">
                      No published policies found.
                    </div>
                  ) : (
                    policies.map((policy) => (
                      <article
                        key={policy.id}
                        className="p-5"
                      >
                        <div className="flex items-start justify-between gap-5">
                          <div>
                            <h2 className="font-bold text-slate-900">
                              {policy.name}
                            </h2>

                            <p className="mt-1 text-sm text-slate-500">
                              {policy.code} ·{" "}
                              {policy.categoryName}
                            </p>

                            <div className="mt-3 flex flex-wrap gap-2">
                              <span className="rounded-full bg-blue-100 px-3 py-1 text-xs font-bold text-blue-800">
                                PUBLISHED
                              </span>

                              {policy.mandatory && (
                                <span className="rounded-full bg-red-100 px-3 py-1 text-xs font-bold text-red-800">
                                  MANDATORY
                                </span>
                              )}

                              <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
                                {policy.applicability.replaceAll(
                                  "_",
                                  " "
                                )}
                              </span>
                            </div>
                          </div>

                          <button
                            type="button"
                            onClick={() =>
                              setSelectedPolicy(policy)
                            }
                            className="shrink-0 rounded-lg bg-blue-700 px-4 py-2 text-sm font-bold text-white"
                          >
                            Read
                          </button>
                        </div>
                      </article>
                    ))
                  )}
                </div>

                <div className="flex items-center justify-between border-t p-4">
                  <span className="text-sm text-slate-500">
                    Page{" "}
                    {totalPages === 0 ? 0 : page + 1} of{" "}
                    {totalPages}
                  </span>

                  <div className="flex gap-2">
                    <button
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

            <aside className="rounded-2xl border border-slate-200 bg-white shadow-sm">
              <div className="flex items-center justify-between border-b p-5">
                <h2 className="font-bold">
                  Recent Notifications
                </h2>

                <Link
                  href="/notifications"
                  className="text-sm font-semibold text-blue-700"
                >
                  View all
                </Link>
              </div>

              <div className="divide-y divide-slate-100">
                {notifications.length === 0 ? (
                  <div className="p-8 text-center text-sm text-slate-500">
                    No notifications found.
                  </div>
                ) : (
                  notifications.map((notification) => (
                    <div
                      key={notification.id}
                      className={`p-5 ${
                        notification.status !== "READ"
                          ? "bg-blue-50/60"
                          : ""
                      }`}
                    >
                      <p className="font-semibold">
                        {notification.title}
                      </p>

                      <p className="mt-2 line-clamp-3 text-sm leading-6 text-slate-600">
                        {notification.message}
                      </p>

                      <p className="mt-2 text-xs text-slate-400">
                        {formatDate(
                          notification.createdAt
                        )}
                      </p>
                    </div>
                  ))
                )}
              </div>
            </aside>
          </section>
        </div>

        {selectedPolicy && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
            <div className="max-h-[85vh] w-full max-w-4xl overflow-y-auto rounded-2xl bg-white shadow-2xl">
              <div className="sticky top-0 flex items-start justify-between border-b bg-white p-6">
                <div>
                  <h2 className="text-2xl font-bold">
                    {selectedPolicy.name}
                  </h2>

                  <p className="mt-1 text-slate-500">
                    {selectedPolicy.code} · Effective{" "}
                    {formatDate(
                      selectedPolicy.effectiveDate
                    )}
                  </p>
                </div>

                <button
                  onClick={() => setSelectedPolicy(null)}
                  className="rounded-lg border px-3 py-2 font-semibold"
                >
                  Close
                </button>
              </div>

              <article className="whitespace-pre-wrap p-6 leading-8 text-slate-800">
                {selectedPolicy.content}
              </article>
            </div>
          </div>
        )}
      </main>
    </ProtectedRoute>
  );
}