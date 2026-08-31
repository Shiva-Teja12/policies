"use client";

import Link from "next/link";
import {
  ArrowLeft,
  Save,
} from "lucide-react";
import { useRouter } from "next/navigation";
import {
  FormEvent,
  useEffect,
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

interface PolicyResponse {
  id: number;
  name: string;
  code: string;
  status: string;
}

export default function CreatePolicyPage() {
  const router = useRouter();

  const [categories, setCategories] = useState<
    Category[]
  >([]);

  const [name, setName] = useState("");
  const [code, setCode] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [content, setContent] = useState("");

  const [applicability, setApplicability] =
    useState<
      "ALL" | "GRADE_BASED" | "DEPT_BASED"
    >("ALL");

  const [applicableDepartments, setApplicableDepartments] =
    useState("");

  const [applicableGrades, setApplicableGrades] =
    useState("");

  const [mandatory, setMandatory] = useState(true);

  const [
    acknowledgementPeriodDays,
    setAcknowledgementPeriodDays,
  ] = useState(7);

  const [
    onboardingPeriodDays,
    setOnboardingPeriodDays,
  ] = useState(7);

  const [loadingCategories, setLoadingCategories] =
    useState(true);

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadCategories() {
      setLoadingCategories(true);
      setError("");

      try {
        const data = await api<Category[]>(
          "/api/policy-categories"
        );

        const activeCategories = (data || []).filter(
          (category) => category.active
        );

        setCategories(activeCategories);

        if (activeCategories.length > 0) {
          setCategoryId(
            String(activeCategories[0].id)
          );
        }
      } catch (requestError) {
        setError(
          requestError instanceof Error
            ? requestError.message
            : "Unable to load policy categories."
        );
      } finally {
        setLoadingCategories(false);
      }
    }

    void loadCategories();
  }, []);

  function commaSeparatedValues(
    value: string
  ): string[] {
    return value
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean);
  }

  async function createPolicy(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();
    setError("");

    const normalizedName = name.trim();
    const normalizedCode = code.trim().toUpperCase();
    const normalizedContent = content.trim();

    if (!normalizedName) {
      setError("Policy name is required.");
      return;
    }

    if (
      !/^ENF-[A-Z]+-\d{3}$/.test(normalizedCode)
    ) {
      setError(
        "Policy code must follow ENF-CATEGORY-001, for example ENF-HR-001."
      );
      return;
    }

    if (!categoryId) {
      setError("Select a policy category.");
      return;
    }

    if (!normalizedContent) {
      setError("Policy content is required.");
      return;
    }

    const departments =
      applicability === "DEPT_BASED"
        ? commaSeparatedValues(
            applicableDepartments
          )
        : [];

    const grades =
      applicability === "GRADE_BASED"
        ? commaSeparatedValues(applicableGrades)
        : [];

    if (
      applicability === "DEPT_BASED" &&
      departments.length === 0
    ) {
      setError(
        "Enter at least one applicable department."
      );
      return;
    }

    if (
      applicability === "GRADE_BASED" &&
      grades.length === 0
    ) {
      setError(
        "Enter at least one applicable employee grade."
      );
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

    if (
      onboardingPeriodDays < 1 ||
      onboardingPeriodDays > 365
    ) {
      setError(
        "Onboarding period must be between 1 and 365 days."
      );
      return;
    }

    setSaving(true);

    try {
      const createdPolicy =
        await api<PolicyResponse>(
          "/api/policies",
          {
            method: "POST",
            body: JSON.stringify({
              name: normalizedName,
              code: normalizedCode,
              categoryId: Number(categoryId),
              content: normalizedContent,
              applicability,
              applicableDepartments: departments,
              applicableGrades: grades,
              mandatory,
              acknowledgementPeriodDays,
              onboardingPeriodDays,
            }),
          }
        );

      router.push(
        `/hr-admin/policies/${createdPolicy.id}`
      );

      router.refresh();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to create policy."
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <ProtectedRoute allowedRoles={["HR_ADMIN"]}>
      <main className="min-h-screen bg-slate-50">
        <header className="border-b border-slate-200 bg-[#082b5c] text-white">
          <div className="mx-auto flex max-w-5xl items-center gap-4 px-5 py-6">
            <Link
              href="/hr-admin/policies"
              className="rounded-lg border border-blue-300/40 p-2 hover:bg-white/10"
              aria-label="Back to policies"
            >
              <ArrowLeft size={20} />
            </Link>

            <div>
              <p className="text-sm text-blue-200">
                HR Admin
              </p>

              <h1 className="text-2xl font-bold">
                Create Policy
              </h1>

              <p className="mt-1 text-sm text-blue-100">
                A new policy is saved with DRAFT status.
              </p>
            </div>
          </div>
        </header>

        <div className="mx-auto max-w-5xl px-5 py-8">
          <form
            onSubmit={createPolicy}
            className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8"
          >
            <div>
              <h2 className="text-xl font-bold text-slate-900">
                Policy Information
              </h2>

              <p className="mt-1 text-sm text-slate-500">
                Complete the required information before
                creating the draft.
              </p>
            </div>

            {error && (
              <div className="mt-6 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
                {error}
              </div>
            )}

            <div className="mt-7 grid gap-6 md:grid-cols-2">
              <div>
                <label
                  htmlFor="policyName"
                  className="mb-2 block text-sm font-semibold text-slate-700"
                >
                  Policy name *
                </label>

                <input
                  id="policyName"
                  required
                  maxLength={200}
                  value={name}
                  onChange={(event) =>
                    setName(event.target.value)
                  }
                  placeholder="Example: Employee Leave Policy"
                  className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
                />
              </div>

              <div>
                <label
                  htmlFor="policyCode"
                  className="mb-2 block text-sm font-semibold text-slate-700"
                >
                  Policy code *
                </label>

                <input
                  id="policyCode"
                  required
                  value={code}
                  onChange={(event) =>
                    setCode(
                      event.target.value.toUpperCase()
                    )
                  }
                  placeholder="ENF-HR-001"
                  className="w-full rounded-xl border border-slate-300 px-4 py-3 uppercase outline-none focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
                />

                <p className="mt-2 text-xs text-slate-500">
                  Required format: ENF-CATEGORY-001
                </p>
              </div>

              <div>
                <label
                  htmlFor="category"
                  className="mb-2 block text-sm font-semibold text-slate-700"
                >
                  Policy category *
                </label>

                <select
                  id="category"
                  required
                  disabled={loadingCategories}
                  value={categoryId}
                  onChange={(event) =>
                    setCategoryId(event.target.value)
                  }
                  className="w-full rounded-xl border border-slate-300 px-4 py-3 disabled:bg-slate-100"
                >
                  <option value="">
                    {loadingCategories
                      ? "Loading categories..."
                      : "Select category"}
                  </option>

                  {categories.map((category) => (
                    <option
                      key={category.id}
                      value={category.id}
                    >
                      {category.name} ({category.code})
                    </option>
                  ))}
                </select>

                {!loadingCategories &&
                  categories.length === 0 && (
                    <p className="mt-2 text-sm text-red-600">
                      No active categories exist.{" "}
                      <Link
                        href="/hr-admin/categories"
                        className="font-bold underline"
                      >
                        Create a category
                      </Link>
                    </p>
                  )}
              </div>

              <div>
                <label
                  htmlFor="applicability"
                  className="mb-2 block text-sm font-semibold text-slate-700"
                >
                  Applicability *
                </label>

                <select
                  id="applicability"
                  value={applicability}
                  onChange={(event) => {
                    const value = event.target.value as
                      | "ALL"
                      | "GRADE_BASED"
                      | "DEPT_BASED";

                    setApplicability(value);
                  }}
                  className="w-full rounded-xl border border-slate-300 px-4 py-3"
                >
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
              </div>

              {applicability === "DEPT_BASED" && (
                <div className="md:col-span-2">
                  <label
                    htmlFor="departments"
                    className="mb-2 block text-sm font-semibold text-slate-700"
                  >
                    Applicable departments *
                  </label>

                  <input
                    id="departments"
                    required
                    value={applicableDepartments}
                    onChange={(event) =>
                      setApplicableDepartments(
                        event.target.value
                      )
                    }
                    placeholder="HR, IT, Finance"
                    className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-blue-600"
                  />

                  <p className="mt-2 text-xs text-slate-500">
                    Separate multiple departments using
                    commas.
                  </p>
                </div>
              )}

              {applicability === "GRADE_BASED" && (
                <div className="md:col-span-2">
                  <label
                    htmlFor="grades"
                    className="mb-2 block text-sm font-semibold text-slate-700"
                  >
                    Applicable grades *
                  </label>

                  <input
                    id="grades"
                    required
                    value={applicableGrades}
                    onChange={(event) =>
                      setApplicableGrades(
                        event.target.value
                      )
                    }
                    placeholder="G1, G2, G3"
                    className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-blue-600"
                  />

                  <p className="mt-2 text-xs text-slate-500">
                    Separate multiple grades using commas.
                  </p>
                </div>
              )}

              <div>
                <label
                  htmlFor="acknowledgementPeriod"
                  className="mb-2 block text-sm font-semibold text-slate-700"
                >
                  Existing employee deadline
                </label>

                <div className="flex items-center gap-3">
                  <input
                    id="acknowledgementPeriod"
                    type="number"
                    min={1}
                    max={365}
                    value={acknowledgementPeriodDays}
                    onChange={(event) =>
                      setAcknowledgementPeriodDays(
                        Number(event.target.value)
                      )
                    }
                    className="w-full rounded-xl border border-slate-300 px-4 py-3"
                  />

                  <span className="text-sm text-slate-500">
                    days
                  </span>
                </div>
              </div>

              <div>
                <label
                  htmlFor="onboardingPeriod"
                  className="mb-2 block text-sm font-semibold text-slate-700"
                >
                  New joiner deadline
                </label>

                <div className="flex items-center gap-3">
                  <input
                    id="onboardingPeriod"
                    type="number"
                    min={1}
                    max={365}
                    value={onboardingPeriodDays}
                    onChange={(event) =>
                      setOnboardingPeriodDays(
                        Number(event.target.value)
                      )
                    }
                    className="w-full rounded-xl border border-slate-300 px-4 py-3"
                  />

                  <span className="text-sm text-slate-500">
                    days
                  </span>
                </div>
              </div>

              <div className="md:col-span-2">
                <label className="flex items-start gap-3 rounded-xl border border-slate-200 bg-slate-50 p-4">
                  <input
                    type="checkbox"
                    checked={mandatory}
                    onChange={(event) =>
                      setMandatory(
                        event.target.checked
                      )
                    }
                    className="mt-1 h-5 w-5"
                  />

                  <span>
                    <span className="block font-semibold text-slate-800">
                      Mandatory acknowledgement
                    </span>

                    <span className="mt-1 block text-sm text-slate-500">
                      Applicable employees must acknowledge
                      every published version of this policy.
                    </span>
                  </span>
                </label>
              </div>

              <div className="md:col-span-2">
                <label
                  htmlFor="content"
                  className="mb-2 block text-sm font-semibold text-slate-700"
                >
                  Policy content *
                </label>

                <textarea
                  id="content"
                  required
                  rows={18}
                  value={content}
                  onChange={(event) =>
                    setContent(event.target.value)
                  }
                  placeholder="Enter the complete policy content. Markdown is supported."
                  className="w-full resize-y rounded-xl border border-slate-300 px-4 py-3 font-mono text-sm outline-none focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
                />

                <p className="mt-2 text-xs text-slate-500">
                  You can use Markdown headings, lists and
                  emphasis.
                </p>
              </div>
            </div>

            <div className="mt-8 flex flex-col-reverse justify-end gap-3 border-t border-slate-200 pt-6 sm:flex-row">
              <Link
                href="/hr-admin/policies"
                className="rounded-xl border border-slate-300 px-5 py-3 text-center font-semibold text-slate-700 hover:bg-slate-50"
              >
                Cancel
              </Link>

              <button
                type="submit"
                disabled={
                  saving ||
                  loadingCategories ||
                  categories.length === 0
                }
                className="flex items-center justify-center gap-2 rounded-xl bg-blue-700 px-5 py-3 font-bold text-white hover:bg-blue-800 disabled:cursor-not-allowed disabled:bg-blue-400"
              >
                <Save size={19} />

                {saving
                  ? "Creating Draft..."
                  : "Create Draft"}
              </button>
            </div>
          </form>
        </div>
      </main>
    </ProtectedRoute>
  );
}