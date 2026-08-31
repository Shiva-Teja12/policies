"use client";

import Link from "next/link";
import {
  ArrowLeft,
  FolderPlus,
  RefreshCw,
  Search,
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
import { api } from "@/lib/api";

interface Category {
  id: number;
  name: string;
  code: string;
  description?: string;
  active: boolean;
}

const PAGE_SIZE = 10;

export default function PolicyCategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [search, setSearch] = useState("");
  const [activeFilter, setActiveFilter] = useState("");
  const [sortDirection, setSortDirection] =
    useState<"asc" | "desc">("asc");
  const [page, setPage] = useState(0);

  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState("");
  const [code, setCode] = useState("");
  const [description, setDescription] = useState("");

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deactivatingId, setDeactivatingId] =
    useState<number | null>(null);

  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadCategories = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const data = await api<Category[]>(
        "/api/policy-categories"
      );

      setCategories(data || []);
    } catch (requestError) {
      setCategories([]);
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load policy categories."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadCategories();
  }, [loadCategories]);

  const filteredCategories = useMemo(() => {
    const query = search.trim().toLowerCase();

    return [...categories]
      .filter((category) => {
        const matchesSearch =
          !query ||
          category.name.toLowerCase().includes(query) ||
          category.code.toLowerCase().includes(query) ||
          (category.description || "")
            .toLowerCase()
            .includes(query);

        const matchesActive =
          !activeFilter ||
          String(category.active) === activeFilter;

        return matchesSearch && matchesActive;
      })
      .sort((first, second) => {
        const comparison = first.name.localeCompare(
          second.name
        );

        return sortDirection === "asc"
          ? comparison
          : -comparison;
      });
  }, [
    activeFilter,
    categories,
    search,
    sortDirection,
  ]);

  const totalPages = Math.ceil(
    filteredCategories.length / PAGE_SIZE
  );

  const visibleCategories = filteredCategories.slice(
    page * PAGE_SIZE,
    page * PAGE_SIZE + PAGE_SIZE
  );

  async function createCategory(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();
    setError("");
    setMessage("");

    if (!/^[A-Z]+$/.test(code.trim())) {
      setError(
        "Category code must contain uppercase letters only, for example HR or LEGAL."
      );
      return;
    }

    setSaving(true);

    try {
      await api<Category>("/api/policy-categories", {
        method: "POST",
        body: JSON.stringify({
          name: name.trim(),
          code: code.trim().toUpperCase(),
          description: description.trim(),
        }),
      });

      setName("");
      setCode("");
      setDescription("");
      setShowCreate(false);
      setMessage("Policy category created successfully.");

      await loadCategories();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to create category."
      );
    } finally {
      setSaving(false);
    }
  }

  async function deactivateCategory(category: Category) {
    if (
      !window.confirm(
        `Deactivate category "${category.name}"? Existing policies will remain unchanged.`
      )
    ) {
      return;
    }

    setDeactivatingId(category.id);
    setError("");
    setMessage("");

    try {
      await api(
        `/api/policy-categories/${category.id}/deactivate`,
        {
          method: "PATCH",
        }
      );

      setMessage("Category deactivated successfully.");
      await loadCategories();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to deactivate category."
      );
    } finally {
      setDeactivatingId(null);
    }
  }

  function resetFilters() {
    setSearch("");
    setActiveFilter("");
    setSortDirection("asc");
    setPage(0);
  }

  return (
    <ProtectedRoute allowedRoles={["HR_ADMIN"]}>
      <main className="min-h-screen bg-slate-50">
        <header className="bg-[#082b5c] text-white">
          <div className="mx-auto flex max-w-6xl items-center justify-between gap-5 px-5 py-6">
            <div className="flex items-center gap-4">
              <Link
                href="/hr-admin/dashboard"
                className="rounded-lg border border-blue-300/40 p-2 hover:bg-white/10"
              >
                <ArrowLeft size={20} />
              </Link>

              <div>
                <p className="text-sm text-blue-200">
                  HR Admin
                </p>

                <h1 className="text-2xl font-bold">
                  Policy Categories
                </h1>
              </div>
            </div>

            <button
              type="button"
              onClick={() => {
                setShowCreate(true);
                setError("");
              }}
              className="flex items-center gap-2 rounded-xl bg-blue-500 px-4 py-2.5 font-bold text-white"
            >
              <FolderPlus size={18} />
              Add Category
            </button>
          </div>
        </header>

        <div className="mx-auto max-w-6xl px-5 py-8">
          {message && (
            <div className="mb-5 rounded-xl border border-green-200 bg-green-50 p-4 text-green-800">
              {message}
            </div>
          )}

          {error && !showCreate && (
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
                onChange={(event) => {
                  setSearch(event.target.value);
                  setPage(0);
                }}
                placeholder="Search category"
                className="w-full rounded-xl border border-slate-300 py-3 pl-11 pr-4"
              />
            </div>

            <select
              value={activeFilter}
              onChange={(event) => {
                setActiveFilter(event.target.value);
                setPage(0);
              }}
              className="rounded-xl border border-slate-300 px-4 py-3"
            >
              <option value="">All statuses</option>
              <option value="true">Active</option>
              <option value="false">Inactive</option>
            </select>

            <select
              value={sortDirection}
              onChange={(event) => {
                setSortDirection(
                  event.target.value as "asc" | "desc"
                );
                setPage(0);
              }}
              className="rounded-xl border border-slate-300 px-4 py-3"
            >
              <option value="asc">Name A–Z</option>
              <option value="desc">Name Z–A</option>
            </select>

            <button
              type="button"
              onClick={resetFilters}
              className="flex items-center justify-center gap-2 rounded-xl border border-slate-300 px-4 py-3 font-semibold"
            >
              <RefreshCw size={17} />
              Reset
            </button>
          </section>

          <section className="mt-6 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="flex justify-between border-b px-5 py-4">
              <h2 className="font-bold">
                Categories
              </h2>

              <span className="text-sm text-slate-500">
                {filteredCategories.length} result
                {filteredCategories.length === 1 ? "" : "s"}
              </span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full min-w-[750px] text-left">
                <thead className="bg-slate-50 text-sm">
                  <tr>
                    <th className="px-5 py-3">Name</th>
                    <th className="px-5 py-3">Code</th>
                    <th className="px-5 py-3">
                      Description
                    </th>
                    <th className="px-5 py-3">Status</th>
                    <th className="px-5 py-3 text-right">
                      Action
                    </th>
                  </tr>
                </thead>

                <tbody className="divide-y divide-slate-100">
                  {loading ? (
                    <tr>
                      <td
                        colSpan={5}
                        className="p-10 text-center"
                      >
                        Loading categories...
                      </td>
                    </tr>
                  ) : visibleCategories.length === 0 ? (
                    <tr>
                      <td
                        colSpan={5}
                        className="p-10 text-center text-slate-500"
                      >
                        No categories found.
                      </td>
                    </tr>
                  ) : (
                    visibleCategories.map((category) => (
                      <tr key={category.id}>
                        <td className="px-5 py-4 font-semibold">
                          {category.name}
                        </td>

                        <td className="px-5 py-4">
                          <span className="rounded bg-slate-100 px-2 py-1 font-mono text-sm">
                            {category.code}
                          </span>
                        </td>

                        <td className="px-5 py-4 text-slate-600">
                          {category.description || "—"}
                        </td>

                        <td className="px-5 py-4">
                          <span
                            className={`rounded-full px-3 py-1 text-xs font-bold ${
                              category.active
                                ? "bg-green-100 text-green-800"
                                : "bg-slate-100 text-slate-600"
                            }`}
                          >
                            {category.active
                              ? "ACTIVE"
                              : "INACTIVE"}
                          </span>
                        </td>

                        <td className="px-5 py-4 text-right">
                          {category.active ? (
                            <button
                              type="button"
                              disabled={
                                deactivatingId === category.id
                              }
                              onClick={() =>
                                deactivateCategory(category)
                              }
                              className="inline-flex items-center gap-2 rounded-lg border border-red-300 px-3 py-2 text-sm font-semibold text-red-700 disabled:opacity-50"
                            >
                              <XCircle size={16} />
                              {deactivatingId === category.id
                                ? "Deactivating..."
                                : "Deactivate"}
                            </button>
                          ) : (
                            <span className="text-sm text-slate-400">
                              No action
                            </span>
                          )}
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

        {showCreate && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
            <form
              onSubmit={createCategory}
              className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl"
            >
              <h2 className="text-xl font-bold">
                Add Policy Category
              </h2>

              {error && (
                <div className="mt-4 rounded-xl bg-red-50 p-3 text-red-700">
                  {error}
                </div>
              )}

              <label className="mt-5 block text-sm font-semibold">
                Category name *
              </label>

              <input
                required
                maxLength={100}
                value={name}
                onChange={(event) =>
                  setName(event.target.value)
                }
                placeholder="Example: Human Resources"
                className="mt-2 w-full rounded-xl border px-4 py-3"
              />

              <label className="mt-5 block text-sm font-semibold">
                Category code *
              </label>

              <input
                required
                maxLength={30}
                value={code}
                onChange={(event) =>
                  setCode(event.target.value.toUpperCase())
                }
                placeholder="HR"
                className="mt-2 w-full rounded-xl border px-4 py-3 uppercase"
              />

              <label className="mt-5 block text-sm font-semibold">
                Description
              </label>

              <textarea
                rows={4}
                maxLength={500}
                value={description}
                onChange={(event) =>
                  setDescription(event.target.value)
                }
                className="mt-2 w-full rounded-xl border px-4 py-3"
              />

              <div className="mt-6 flex justify-end gap-3">
                <button
                  type="button"
                  disabled={saving}
                  onClick={() => {
                    setShowCreate(false);
                    setError("");
                  }}
                  className="rounded-xl border px-5 py-3 font-semibold"
                >
                  Cancel
                </button>

                <button
                  disabled={saving}
                  className="rounded-xl bg-blue-700 px-5 py-3 font-bold text-white disabled:bg-blue-400"
                >
                  {saving
                    ? "Creating..."
                    : "Create Category"}
                </button>
              </div>
            </form>
          </div>
        )}
      </main>
    </ProtectedRoute>
  );
}