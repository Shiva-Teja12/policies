"use client";

import { api } from "@/lib/api";
import {
  AlertTriangle,
  Archive,
  CalendarDays,
  X,
} from "lucide-react";
import { FormEvent, useMemo, useState } from "react";

interface RetirePolicyButtonProps {
  policyId: number;
  policyName: string;
  policyCode: string;
  status: string;
}

function getToday() {
  const now = new Date();

  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

export default function RetirePolicyButton({
  policyId,
  policyName,
  policyCode,
  status,
}: RetirePolicyButtonProps) {
  const today = useMemo(() => getToday(), []);

  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const [
    retirementEffectiveDate,
    setRetirementEffectiveDate,
  ] = useState(today);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  /*
   * Retirement is available only for a published policy.
   */
  if (status?.toUpperCase() !== "PUBLISHED") {
    return null;
  }

  function closeModal() {
    if (submitting) {
      return;
    }

    setOpen(false);
    setError("");
    setReason("");
    setRetirementEffectiveDate(today);
  }

  async function handleRetire(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    setError("");

    if (!reason.trim()) {
      setError("Retirement reason is required.");
      return;
    }

    if (!retirementEffectiveDate) {
      setError(
        "Retirement effective date is required."
      );
      return;
    }

    if (retirementEffectiveDate < today) {
      setError(
        "Retirement effective date cannot be in the past."
      );
      return;
    }

    const confirmed = window.confirm(
      `Are you sure you want to retire ${policyName}?`
    );

    if (!confirmed) {
      return;
    }

    setSubmitting(true);

    try {
      /*
       * Backend endpoint:
       * PATCH /api/policies/{id}/retire
       */
      await api(
        `/api/policies/${policyId}/retire`,
        {
          method: "PATCH",
          body: JSON.stringify({
            reason: reason.trim(),
            retirementEffectiveDate,
          }),
        }
      );

      window.alert(
        `${policyName} has been retired successfully.`
      );

      setOpen(false);

      /*
       * Reload so that the policy status and available
       * actions are updated everywhere.
       */
      window.location.reload();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to retire the policy. Please try again."
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        title={`Retire ${policyName}`}
        aria-label={`Retire ${policyName}`}
        className="inline-flex items-center justify-center gap-2 rounded-lg border border-red-300 px-3 py-2 font-semibold text-red-700 transition hover:border-red-500 hover:bg-red-50"
      >
        <Archive size={18} />

        <span>Retire</span>
      </button>

      {open && (
        <div
          className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/60 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="retire-policy-title"
        >
          <div className="max-h-[95vh] w-full max-w-xl overflow-y-auto rounded-2xl bg-white shadow-2xl">
            <header className="flex items-start justify-between border-b border-slate-200 bg-[#0d356b] px-6 py-5 text-white">
              <div>
                <p className="text-sm text-blue-200">
                  HR Admin
                </p>

                <h2
                  id="retire-policy-title"
                  className="mt-1 text-2xl font-bold"
                >
                  Retire Policy
                </h2>

                <p className="mt-1 text-blue-100">
                  {policyName} · {policyCode}
                </p>
              </div>

              <button
                type="button"
                onClick={closeModal}
                disabled={submitting}
                aria-label="Close retirement form"
                className="rounded-lg p-2 transition hover:bg-white/10 disabled:opacity-50"
              >
                <X size={24} />
              </button>
            </header>

            <form
              onSubmit={handleRetire}
              className="space-y-6 p-6"
            >
              <div className="flex gap-3 rounded-xl border border-amber-300 bg-amber-50 p-4 text-amber-900">
                <AlertTriangle
                  size={24}
                  className="mt-0.5 shrink-0"
                />

                <div>
                  <p className="font-bold">
                    Retirement preserves history
                  </p>

                  <p className="mt-1 text-sm leading-6">
                    Retired policies will no longer be
                    assigned to new employees. Existing
                    versions and acknowledgement history
                    will remain available.
                  </p>
                </div>
              </div>

              {error && (
                <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">
                  {error}
                </div>
              )}

              <div>
                <label
                  htmlFor="retirementReason"
                  className="mb-2 block font-semibold text-slate-900"
                >
                  Retirement reason *
                </label>

                <textarea
                  id="retirementReason"
                  required
                  maxLength={1000}
                  rows={5}
                  value={reason}
                  onChange={(event) =>
                    setReason(event.target.value)
                  }
                  placeholder="Example: This policy has been replaced by an updated leave policy."
                  className="w-full resize-y rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-red-500 focus:ring-4 focus:ring-red-100"
                />

                <div className="mt-1 text-right text-sm text-slate-500">
                  {reason.length}/1000
                </div>
              </div>

              <div>
                <label
                  htmlFor="retirementEffectiveDate"
                  className="mb-2 block font-semibold text-slate-900"
                >
                  Retirement effective date *
                </label>

                <div className="relative">
                  <CalendarDays
                    size={20}
                    className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-500"
                  />

                  <input
                    id="retirementEffectiveDate"
                    type="date"
                    required
                    min={today}
                    value={retirementEffectiveDate}
                    onChange={(event) =>
                      setRetirementEffectiveDate(
                        event.target.value
                      )
                    }
                    className="w-full rounded-xl border border-slate-300 py-3 pl-12 pr-4 outline-none transition focus:border-red-500 focus:ring-4 focus:ring-red-100"
                  />
                </div>
              </div>

              <footer className="flex flex-col-reverse gap-3 border-t border-slate-200 pt-5 sm:flex-row sm:justify-end">
                <button
                  type="button"
                  onClick={closeModal}
                  disabled={submitting}
                  className="rounded-xl border border-slate-300 px-5 py-3 font-semibold text-slate-700 transition hover:bg-slate-50 disabled:opacity-50"
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  disabled={submitting}
                  className="inline-flex items-center justify-center gap-2 rounded-xl bg-red-600 px-5 py-3 font-semibold text-white transition hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <Archive size={19} />

                  {submitting
                    ? "Retiring policy..."
                    : "Retire Policy"}
                </button>
              </footer>
            </form>
          </div>
        </div>
      )}
    </>
  );
}