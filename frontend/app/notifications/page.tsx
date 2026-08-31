"use client";

import {
  Bell,
  CheckCheck,
  RefreshCw,
  Search,
} from "lucide-react";
import {
  useCallback,
  useEffect,
  useState,
} from "react";
import { api, PageData } from "@/lib/api";
import {
  getSession,
  UserRole,
} from "@/lib/auth";
import ProtectedRoute from "@/components/ProtectedRoute";

interface Notification {
  id: number;
  policyId?: number;
  policyCode?: string;
  policyName?: string;
  title: string;
  message: string;
  status: "PENDING" | "SENT" | "FAILED" | "READ";
  createdAt: string;
  sentAt?: string;
  readAt?: string;
}

const PAGE_SIZE = 10;

const allowedRoles: UserRole[] = [
  "HR_ADMIN",
  "LEGAL_REVIEWER",
  "HR_HEAD",
  "MANAGING_DIRECTOR",
  "MANAGER",
  "EMPLOYEE",
];

function formatDate(value?: string): string {
  if (!value) {
    return "—";
  }

  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString();
}

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<
    Notification[]
  >([]);

  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [sortBy, setSortBy] = useState("createdAt");
  const [direction, setDirection] =
    useState<"asc" | "desc">("desc");

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [unreadCount, setUnreadCount] = useState(0);

  const [loading, setLoading] = useState(true);
  const [readingId, setReadingId] = useState<number | null>(
    null
  );
  const [error, setError] = useState("");

  const loadNotifications = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const params = new URLSearchParams({
        page: String(page),
        size: String(PAGE_SIZE),
        sortBy,
        direction,
      });

      if (status) {
        params.set("status", status);
      }

      const [notificationPage, unread] =
        await Promise.all([
          api<PageData<Notification>>(
            `/api/notifications?${params.toString()}`
          ),
          api<{ unreadCount: number }>(
            "/api/notifications/unread-count"
          ),
        ]);

      setNotifications(notificationPage.content || []);
      setTotalPages(notificationPage.totalPages || 0);
      setTotalElements(
        notificationPage.totalElements || 0
      );
      setUnreadCount(unread.unreadCount || 0);
    } catch (requestError) {
      setNotifications([]);
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load notifications."
      );
    } finally {
      setLoading(false);
    }
  }, [direction, page, sortBy, status]);

  useEffect(() => {
    void loadNotifications();
  }, [loadNotifications]);

  async function markAsRead(notificationId: number) {
    setReadingId(notificationId);
    setError("");

    try {
      await api(
        `/api/notifications/${notificationId}/read`,
        {
          method: "PATCH",
        }
      );

      await loadNotifications();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to mark notification as read."
      );
    } finally {
      setReadingId(null);
    }
  }

  const filteredNotifications = notifications.filter(
    (notification) => {
      const query = search.trim().toLowerCase();

      if (!query) {
        return true;
      }

      return [
        notification.title,
        notification.message,
        notification.policyName,
        notification.policyCode,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase()
        .includes(query);
    }
  );

  const session = getSession();

  return (
    <ProtectedRoute allowedRoles={allowedRoles}>
      <main className="min-h-screen bg-slate-50">
        <header className="bg-[#082b5c] text-white">
          <div className="mx-auto max-w-6xl px-5 py-7">
            <p className="text-sm text-blue-200">
              {session?.role?.replaceAll("_", " ") ||
                "HRMS"}
            </p>

            <h1 className="mt-1 flex items-center gap-3 text-3xl font-bold">
              <Bell />
              Notifications
            </h1>

            <p className="mt-2 text-blue-100">
              You have {unreadCount} unread notification
              {unreadCount === 1 ? "" : "s"}.
            </p>
          </div>
        </header>

        <div className="mx-auto max-w-6xl px-5 py-8">
          {error && (
            <div className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">
              {error}
            </div>
          )}

          <section className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:grid-cols-[2fr_1fr_1fr_auto]">
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
                placeholder="Search notifications"
                className="w-full rounded-xl border border-slate-300 py-3 pl-11 pr-4"
              />
            </div>

            <select
              value={status}
              onChange={(event) => {
                setStatus(event.target.value);
                setPage(0);
              }}
              className="rounded-xl border border-slate-300 px-4 py-3"
            >
              <option value="">All statuses</option>
              <option value="PENDING">Pending</option>
              <option value="SENT">Unread/Sent</option>
              <option value="READ">Read</option>
              <option value="FAILED">Failed</option>
            </select>

            <select
              value={`${sortBy},${direction}`}
              onChange={(event) => {
                const [field, order] =
                  event.target.value.split(",");

                setSortBy(field);
                setDirection(order as "asc" | "desc");
                setPage(0);
              }}
              className="rounded-xl border border-slate-300 px-4 py-3"
            >
              <option value="createdAt,desc">
                Newest first
              </option>
              <option value="createdAt,asc">
                Oldest first
              </option>
              <option value="title,asc">
                Title A–Z
              </option>
              <option value="title,desc">
                Title Z–A
              </option>
            </select>

            <button
              type="button"
              disabled={loading}
              onClick={() => void loadNotifications()}
              className="flex items-center justify-center gap-2 rounded-xl border border-slate-300 px-4 py-3 font-semibold"
            >
              <RefreshCw
                size={17}
                className={loading ? "animate-spin" : ""}
              />
              Refresh
            </button>
          </section>

          <section className="mt-6 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="flex items-center justify-between border-b px-5 py-4">
              <h2 className="font-bold">
                All notifications
              </h2>

              <span className="text-sm text-slate-500">
                {totalElements} total
              </span>
            </div>

            <div className="divide-y divide-slate-100">
              {loading ? (
                <div className="p-12 text-center text-slate-500">
                  Loading notifications...
                </div>
              ) : filteredNotifications.length === 0 ? (
                <div className="p-12 text-center">
                  <Bell
                    size={40}
                    className="mx-auto text-slate-300"
                  />

                  <p className="mt-3 font-semibold text-slate-700">
                    No notifications found
                  </p>
                </div>
              ) : (
                filteredNotifications.map(
                  (notification) => (
                    <article
                      key={notification.id}
                      className={`p-5 ${
                        notification.status !== "READ"
                          ? "bg-blue-50/60"
                          : "bg-white"
                      }`}
                    >
                      <div className="flex items-start justify-between gap-5">
                        <div className="min-w-0">
                          <div className="flex flex-wrap items-center gap-2">
                            <h3 className="font-bold text-slate-900">
                              {notification.title}
                            </h3>

                            {notification.status !== "READ" && (
                              <span className="rounded-full bg-blue-700 px-2 py-0.5 text-xs font-bold text-white">
                                NEW
                              </span>
                            )}
                          </div>

                          <p className="mt-2 leading-6 text-slate-700">
                            {notification.message}
                          </p>

                          {notification.policyName && (
                            <p className="mt-2 text-sm font-semibold text-blue-700">
                              {notification.policyName}
                              {notification.policyCode
                                ? ` (${notification.policyCode})`
                                : ""}
                            </p>
                          )}

                          <p className="mt-3 text-xs text-slate-500">
                            {formatDate(
                              notification.createdAt
                            )}
                          </p>
                        </div>

                        {notification.status !== "READ" && (
                          <button
                            type="button"
                            disabled={
                              readingId === notification.id
                            }
                            onClick={() =>
                              markAsRead(notification.id)
                            }
                            className="flex shrink-0 items-center gap-2 rounded-lg border border-blue-300 px-3 py-2 text-sm font-semibold text-blue-700 disabled:opacity-50"
                          >
                            <CheckCheck size={16} />
                            Mark read
                          </button>
                        )}
                      </div>
                    </article>
                  )
                )
              )}
            </div>

            <div className="flex items-center justify-between border-t px-5 py-4">
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
      </main>
    </ProtectedRoute>
  );
}