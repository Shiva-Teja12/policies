"use client";

import Link from "next/link";
import {
  useParams,
  useRouter,
} from "next/navigation";
import {
  AlertTriangle,
  ArrowLeft,
  Save,
  Send,
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
}

interface Approval {
  approvalId: number;

  stage:
    | "LEGAL_REVIEW"
    | "HR_HEAD_REVIEW"
    | "MD_REVIEW";

  decision:
    | "PENDING"
    | "APPROVED"
    | "REJECTED";

  comments?: string | null;
  approverName?: string | null;
  approverEmail?: string | null;
  decidedAt?: string | null;
  sequenceNumber: number;
}

interface PolicyRequest {
  name: string;
  code: string;
  categoryId: number;
  content: string;

  applicability:
    | "ALL"
    | "GRADE_BASED"
    | "DEPT_BASED";

  applicableDepartments: string[];
  applicableGrades: string[];

  mandatory: boolean;
  acknowledgementPeriodDays: number;
  onboardingPeriodDays: number;
}

type SubmitAction = "SAVE" | "SAVE_AND_RESUBMIT";

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

function formatDateTime(value?: string | null) {
  if (!value) {
    return "—";
  }

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

function splitValues(value: string) {
  return value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

export default function EditPolicyPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();

  const policyId = Number(params.id);

  const [originalPolicy, setOriginalPolicy] =
    useState<Policy | null>(null);

  const [categories, setCategories] = useState<
    Category[]
  >([]);

  const [approvals, setApprovals] = useState<
    Approval[]
  >([]);

  const [name, setName] = useState("");
  const [code, setCode] = useState("");
  const [categoryId, setCategoryId] =
    useState("");

  const [content, setContent] = useState("");

  const [applicability, setApplicability] =
    useState<
      "ALL" | "GRADE_BASED" | "DEPT_BASED"
    >("ALL");

  const [
    applicableDepartments,
    setApplicableDepartments,
  ] = useState("");

  const [applicableGrades, setApplicableGrades] =
    useState("");

  const [mandatory, setMandatory] =
    useState(false);

  const [
    acknowledgementPeriodDays,
    setAcknowledgementPeriodDays,
  ] = useState(7);

  const [
    onboardingPeriodDays,
    setOnboardingPeriodDays,
  ] = useState(7);

  const [loading, setLoading] = useState(true);

  const [submitAction, setSubmitAction] =
    useState<SubmitAction | null>(null);

  const [error, setError] = useState("");

  const latestRejection = useMemo(() => {
    return [...approvals]
      .filter(
        (approval) =>
          approval.decision === "REJECTED"
      )
      .sort((first, second) => {
        const firstTime = new Date(
          first.decidedAt ?? 0
        ).getTime();

        const secondTime = new Date(
          second.decidedAt ?? 0
        ).getTime();

        return secondTime - firstTime;
      })[0];
  }, [approvals]);

  const loadPage = useCallback(async () => {
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
      const [
        policyData,
        categoryData,
        approvalData,
      ] = await Promise.all([
        api<Policy>(
          `/api/policies/${policyId}`
        ),

        api<Category[]>(
          "/api/policy-categories"
        ),

        api<Approval[]>(
          `/api/policies/${policyId}/approval-history`
        ),
      ]);

      if (
        policyData.status !== "DRAFT" &&
        policyData.status !== "REJECTED"
      ) {
        throw new Error(
          "Only Draft or Rejected policies can be edited."
        );
      }

      setOriginalPolicy(policyData);

      setCategories(
        (categoryData || []).filter(
          (category) => category.active
        )
      );

      setApprovals(approvalData || []);

      setName(policyData.name || "");
      setCode(policyData.code || "");

      setCategoryId(
        String(policyData.categoryId)
      );

      setContent(policyData.content || "");

      setApplicability(
        policyData.applicability || "ALL"
      );

      setApplicableDepartments(
        (
          policyData.applicableDepartments || []
        ).join(", ")
      );

      setApplicableGrades(
        (policyData.applicableGrades || []).join(
          ", "
        )
      );

      setMandatory(
        Boolean(policyData.mandatory)
      );

      setAcknowledgementPeriodDays(
        policyData.acknowledgementPeriodDays ||
          7
      );

      setOnboardingPeriodDays(
        policyData.onboardingPeriodDays || 7
      );
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load policy."
      );
    } finally {
      setLoading(false);
    }
  }, [policyId]);

  useEffect(() => {
    void loadPage();
  }, [loadPage]);

  function createRequest(): PolicyRequest {
    if (!name.trim()) {
      throw new Error(
        "Policy name is required."
      );
    }

    if (!code.trim()) {
      throw new Error(
        "Policy code is required."
      );
    }

    if (!/^ENF-[A-Z]+-\d{3}$/.test(
      code.trim().toUpperCase()
    )) {
      throw new Error(
        "Policy code must follow ENF-CATEGORY-001 format."
      );
    }

    if (!categoryId) {
      throw new Error(
        "Policy category is required."
      );
    }

    if (!content.trim()) {
      throw new Error(
        "Policy content is required."
      );
    }

    if (
      acknowledgementPeriodDays < 1 ||
      acknowledgementPeriodDays > 365
    ) {
      throw new Error(
        "Existing employee deadline must be between 1 and 365 days."
      );
    }

    if (
      onboardingPeriodDays < 1 ||
      onboardingPeriodDays > 365
    ) {
      throw new Error(
        "New joiner deadline must be between 1 and 365 days."
      );
    }

    const departments =
      applicability === "DEPT_BASED"
        ? splitValues(applicableDepartments)
        : [];

    const grades =
      applicability === "GRADE_BASED"
        ? splitValues(applicableGrades)
        : [];

    if (
      applicability === "DEPT_BASED" &&
      departments.length === 0
    ) {
      throw new Error(
        "Enter at least one applicable department."
      );
    }

    if (
      applicability === "GRADE_BASED" &&
      grades.length === 0
    ) {
      throw new Error(
        "Enter at least one applicable grade."
      );
    }

    return {
      name: name.trim(),
      code: code.trim().toUpperCase(),
      categoryId: Number(categoryId),
      content: content.trim(),
      applicability,
      applicableDepartments: departments,
      applicableGrades: grades,
      mandatory,
      acknowledgementPeriodDays,
      onboardingPeriodDays,
    };
  }

  async function savePolicy(
    action: SubmitAction
  ) {
    setError("");
    setSubmitAction(action);

    try {
      const request = createRequest();

      /*
       * First save the corrected policy.
       * The backend changes REJECTED back to DRAFT.
       */
      const updatedPolicy = await api<Policy>(
        `/api/policies/${policyId}`,
        {
          method: "PUT",
          body: JSON.stringify(request),
        }
      );

      if (action === "SAVE_AND_RESUBMIT") {
        /*
         * Submit only after the update succeeds.
         */
        await api(
          `/api/policies/${policyId}/submit-for-review`,
          {
            method: "PATCH",
          }
        );

        window.alert(
          "Policy changes were saved and the policy was resubmitted to the Legal Reviewer."
        );

        router.push(
          `/hr-admin/policies/${policyId}`
        );

        return;
      }

      window.alert(
        "Policy changes saved successfully."
      );

      /*
       * The backend changes a rejected policy to DRAFT
       * after it is corrected.
       */
      setOriginalPolicy(updatedPolicy);

      router.push(
        `/hr-admin/policies/${policyId}`
      );
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to update the policy."
      );
    } finally {
      setSubmitAction(null);
    }
  }

  function handleSubmit(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();
    void savePolicy("SAVE");
  }

  if (loading) {
    return (
      <ProtectedRoute allowedRoles={["HR_ADMIN"]}>
        <main className="flex min-h-screen items-center justify-center bg-slate-50">
          <p className="text-slate-500">
            Loading policy...
          </p>
        </main>
      </ProtectedRoute>
    );
  }

  return (
    <ProtectedRoute allowedRoles={["HR_ADMIN"]}>
      <main className="min-h-screen bg-slate-50">
        <header className="bg-[#082b5c] text-white">
          <div className="mx-auto flex w-full max-w-7xl items-start gap-4 px-4 py-7 sm:px-6">
            <Link
              href={`/hr-admin/policies/${policyId}`}
              className="mt-1 rounded-xl border border-white/30 p-3 hover:bg-white/10"
              title="Back to policy details"
            >
              <ArrowLeft size={22} />
            </Link>

            <div>
              <p className="text-sm text-blue-200">
                HR Admin
              </p>

              <h1 className="mt-1 text-3xl font-bold">
                Edit Policy
              </h1>

              <p className="mt-2 text-blue-100">
                {originalPolicy?.name} ·{" "}
                {originalPolicy?.code}
              </p>
            </div>
          </div>
        </header>

        <div className="mx-auto w-full max-w-6xl px-4 py-8 sm:px-6">
          {error && (
            <div className="mb-6 rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">
              {error}
            </div>
          )}

          {latestRejection && (
            <section className="mb-6 rounded-2xl border border-red-300 bg-red-50 p-6">
              <div className="flex items-start gap-4">
                <AlertTriangle
                  size={26}
                  className="mt-1 shrink-0 text-red-700"
                />

                <div className="min-w-0">
                  <p className="text-sm font-bold uppercase tracking-wide text-red-600">
                    Rejection comment
                  </p>

                  <p className="mt-3 whitespace-pre-wrap break-words text-lg font-semibold text-red-950">
                    {latestRejection.comments ||
                      "No comment was provided."}
                  </p>

                  <div className="mt-3 flex flex-wrap gap-5 text-sm text-red-800">
                    <span>
                      Stage:{" "}
                      {displayLabel(
                        latestRejection.stage
                      )}
                    </span>

                    <span>
                      Reviewer:{" "}
                      {latestRejection.approverName ||
                        latestRejection.approverEmail ||
                        "Reviewer"}
                    </span>

                    <span>
                      Date:{" "}
                      {formatDateTime(
                        latestRejection.decidedAt
                      )}
                    </span>
                  </div>
                </div>
              </div>
            </section>
          )}

          <form
            onSubmit={handleSubmit}
            className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
          >
            <div className="border-b border-slate-200 px-6 py-5">
              <h2 className="text-xl font-bold">
                Policy Information
              </h2>

              <p className="mt-1 text-sm text-slate-500">
                Correct the policy according to the
                reviewer’s comment.
              </p>
            </div>

            <div className="grid grid-cols-1 gap-6 p-6 md:grid-cols-2">
              <div>
                <label
                  htmlFor="name"
                  className="mb-2 block font-semibold"
                >
                  Policy name *
                </label>

                <input
                  id="name"
                  required
                  maxLength={200}
                  value={name}
                  onChange={(event) =>
                    setName(event.target.value)
                  }
                  className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
                />
              </div>

              <div>
                <label
                  htmlFor="code"
                  className="mb-2 block font-semibold"
                >
                  Policy code *
                </label>

                <input
                  id="code"
                  required
                  value={code}
                  disabled={Boolean(
                    originalPolicy?.publishedOnce
                  )}
                  onChange={(event) =>
                    setCode(
                      event.target.value.toUpperCase()
                    )
                  }
                  placeholder="ENF-HR-001"
                  className="w-full rounded-xl border border-slate-300 px-4 py-3 uppercase outline-none focus:border-blue-600 focus:ring-4 focus:ring-blue-100 disabled:bg-slate-100"
                />

                <p className="mt-1 text-xs text-slate-500">
                  Required format: ENF-CATEGORY-001
                </p>
              </div>

              <div>
                <label
                  htmlFor="category"
                  className="mb-2 block font-semibold"
                >
                  Policy category *
                </label>

                <select
                  id="category"
                  required
                  value={categoryId}
                  onChange={(event) =>
                    setCategoryId(event.target.value)
                  }
                  className="w-full rounded-xl border border-slate-300 px-4 py-3"
                >
                  <option value="">
                    Select category
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
              </div>

              <div>
                <label
                  htmlFor="applicability"
                  className="mb-2 block font-semibold"
                >
                  Applicability *
                </label>

                <select
                  id="applicability"
                  required
                  value={applicability}
                  onChange={(event) =>
                    setApplicability(
                      event.target.value as
                        | "ALL"
                        | "GRADE_BASED"
                        | "DEPT_BASED"
                    )
                  }
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
                    className="mb-2 block font-semibold"
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
                    placeholder="HR, Finance, IT"
                    className="w-full rounded-xl border border-slate-300 px-4 py-3"
                  />

                  <p className="mt-1 text-xs text-slate-500">
                    Separate departments using commas.
                  </p>
                </div>
              )}

              {applicability === "GRADE_BASED" && (
                <div className="md:col-span-2">
                  <label
                    htmlFor="grades"
                    className="mb-2 block font-semibold"
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
                    className="w-full rounded-xl border border-slate-300 px-4 py-3"
                  />

                  <p className="mt-1 text-xs text-slate-500">
                    Separate grades using commas.
                  </p>
                </div>
              )}

              <div>
                <label
                  htmlFor="acknowledgementDays"
                  className="mb-2 block font-semibold"
                >
                  Existing employee deadline
                </label>

                <div className="flex items-center gap-3">
                  <input
                    id="acknowledgementDays"
                    type="number"
                    required
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

                  <span className="text-slate-500">
                    days
                  </span>
                </div>
              </div>

              <div>
                <label
                  htmlFor="onboardingDays"
                  className="mb-2 block font-semibold"
                >
                  New joiner deadline
                </label>

                <div className="flex items-center gap-3">
                  <input
                    id="onboardingDays"
                    type="number"
                    required
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

                  <span className="text-slate-500">
                    days
                  </span>
                </div>
              </div>

              <label className="flex items-start gap-3 rounded-xl border border-slate-200 bg-slate-50 p-4 md:col-span-2">
                <input
                  type="checkbox"
                  checked={mandatory}
                  onChange={(event) =>
                    setMandatory(event.target.checked)
                  }
                  className="mt-1 h-5 w-5"
                />

                <span>
                  <span className="block font-bold">
                    Mandatory acknowledgement
                  </span>

                  <span className="mt-1 block text-sm text-slate-600">
                    Applicable employees must acknowledge
                    every published version.
                  </span>
                </span>
              </label>

              <div className="md:col-span-2">
                <label
                  htmlFor="content"
                  className="mb-2 block font-semibold"
                >
                  Policy content *
                </label>

                <textarea
                  id="content"
                  required
                  rows={16}
                  value={content}
                  onChange={(event) =>
                    setContent(event.target.value)
                  }
                  className="w-full resize-y rounded-xl border border-slate-300 px-4 py-3 leading-7 outline-none focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
                />
              </div>
            </div>

            <footer className="flex flex-col-reverse gap-3 border-t border-slate-200 px-6 py-5 sm:flex-row sm:justify-end">
              <Link
                href={`/hr-admin/policies/${policyId}`}
                className="rounded-xl border border-slate-300 px-5 py-3 text-center font-semibold text-slate-700 hover:bg-slate-50"
              >
                Cancel
              </Link>

              <button
                type="submit"
                disabled={submitAction !== null}
                className="inline-flex items-center justify-center gap-2 rounded-xl border border-blue-300 px-5 py-3 font-semibold text-blue-700 hover:bg-blue-50 disabled:opacity-50"
              >
                <Save size={18} />

                {submitAction === "SAVE"
                  ? "Saving..."
                  : "Save Changes"}
              </button>

              <button
                type="button"
                disabled={submitAction !== null}
                onClick={() =>
                  void savePolicy(
                    "SAVE_AND_RESUBMIT"
                  )
                }
                className="inline-flex items-center justify-center gap-2 rounded-xl bg-blue-700 px-5 py-3 font-semibold text-white hover:bg-blue-800 disabled:opacity-50"
              >
                <Send size={18} />

                {submitAction ===
                "SAVE_AND_RESUBMIT"
                  ? "Saving & Resubmitting..."
                  : "Save & Resubmit"}
              </button>
            </footer>
          </form>
        </div>
      </main>
    </ProtectedRoute>
  );
}