"use client";

import {
  FormEvent,
  Suspense,
  useState,
} from "react";

import {
  useRouter,
  useSearchParams,
} from "next/navigation";

const API_URL =
  process.env.NEXT_PUBLIC_API_URL ||
  "http://localhost:8080";

type ApiResponse = {
  success?: boolean;
  message?: string;
  status?: number;
  path?: string;
  timestamp?: string;
};

function ResetPasswordForm() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const token =
    searchParams.get("token") || "";

  const [newPassword, setNewPassword] =
    useState("");

  const [
    confirmPassword,
    setConfirmPassword,
  ] = useState("");

  const [showPassword, setShowPassword] =
    useState(false);

  const [loading, setLoading] =
    useState(false);

  const [message, setMessage] =
    useState("");

  const [error, setError] =
    useState("");

  async function handleSubmit(
    e: FormEvent<HTMLFormElement>
  ) {
    e.preventDefault();

    setMessage("");
    setError("");

    // ============================================
    // FRONTEND VALIDATION
    // ============================================

    if (!token) {
      setError(
        "Invalid password reset link. Please request a new reset link."
      );
      return;
    }

    if (newPassword.length < 8) {
      setError(
        "Password must contain at least 8 characters."
      );
      return;
    }

    if (newPassword !== confirmPassword) {
      setError(
        "New password and confirm password do not match."
      );
      return;
    }

    setLoading(true);

    try {
      // ============================================
      // CALL BACKEND RESET PASSWORD API
      // ============================================

      const response = await fetch(
        `${API_URL}/api/auth/reset-password`,
        {
          method: "POST",

          headers: {
            "Content-Type":
              "application/json",
          },

          body: JSON.stringify({
            token,
            newPassword,
            confirmPassword,
          }),
        }
      );

      // ============================================
      // READ BACKEND RESPONSE
      // ============================================

      let data: ApiResponse | null =
        null;

      try {
        data =
          (await response.json()) as ApiResponse;
      } catch {
        data = null;
      }

      // ============================================
      // BACKEND ERROR
      // ============================================

      if (!response.ok) {
        throw new Error(
          data?.message ||
            "Unable to reset password."
        );
      }

      // ============================================
      // SUCCESS
      // ============================================

      setMessage(
        data?.message
          ? `${data.message}. Redirecting to login...`
          : "Password reset successfully. Redirecting to login..."
      );

      setNewPassword("");
      setConfirmPassword("");

      setTimeout(() => {
        router.push("/login");
      }, 2000);
    } catch (err) {
      // ============================================
      // DISPLAY ONLY USER-FRIENDLY MESSAGE
      // ============================================

      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError(
          "Something went wrong. Please try again."
        );
      }
    } finally {
      setLoading(false);
    }
  }

  // ============================================
  // TOKEN MISSING FROM URL
  // ============================================

  if (!token) {
    return (
      <main className="min-h-screen bg-slate-50 flex items-center justify-center px-4">
        <div className="w-full max-w-md rounded-2xl border border-red-200 bg-white p-8 shadow-lg">
          <h1 className="text-2xl font-bold text-slate-900">
            Invalid Reset Link
          </h1>

          <p className="mt-3 text-sm text-red-600">
            The password reset token is
            missing.
          </p>

          <button
            type="button"
            onClick={() =>
              router.push(
                "/forgot-password"
              )
            }
            className="mt-6 w-full rounded-lg bg-slate-900 py-3 font-semibold text-white transition hover:bg-slate-800"
          >
            Request New Reset Link
          </button>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-slate-50 flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-lg border border-slate-200 p-8">
        {/* ============================================
            HEADER
        ============================================ */}

        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold text-slate-900">
            Reset Password
          </h1>

          <p className="mt-2 text-sm text-slate-600">
            Enter your new password below.
          </p>
        </div>

        {/* ============================================
            SUCCESS MESSAGE
        ============================================ */}

        {message && (
          <div className="mb-5 rounded-lg border border-green-200 bg-green-50 p-4 text-sm text-green-700">
            {message}
          </div>
        )}

        {/* ============================================
            ERROR MESSAGE
        ============================================ */}

        {error && (
          <div className="mb-5 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            {error}
          </div>
        )}

        {/* ============================================
            RESET PASSWORD FORM
        ============================================ */}

        <form
          onSubmit={handleSubmit}
          className="space-y-5"
        >
          {/* NEW PASSWORD */}

          <div>
            <label
              htmlFor="newPassword"
              className="mb-2 block text-sm font-medium text-slate-700"
            >
              New Password
            </label>

            <input
              id="newPassword"
              type={
                showPassword
                  ? "text"
                  : "password"
              }
              required
              minLength={8}
              value={newPassword}
              onChange={(e) =>
                setNewPassword(
                  e.target.value
                )
              }
              placeholder="Enter new password"
              autoComplete="new-password"
              className="w-full rounded-lg border border-slate-300 px-4 py-3 outline-none focus:border-slate-500 focus:ring-2 focus:ring-slate-200"
            />
          </div>

          {/* CONFIRM PASSWORD */}

          <div>
            <label
              htmlFor="confirmPassword"
              className="mb-2 block text-sm font-medium text-slate-700"
            >
              Confirm New Password
            </label>

            <input
              id="confirmPassword"
              type={
                showPassword
                  ? "text"
                  : "password"
              }
              required
              minLength={8}
              value={confirmPassword}
              onChange={(e) =>
                setConfirmPassword(
                  e.target.value
                )
              }
              placeholder="Confirm new password"
              autoComplete="new-password"
              className="w-full rounded-lg border border-slate-300 px-4 py-3 outline-none focus:border-slate-500 focus:ring-2 focus:ring-slate-200"
            />
          </div>

          {/* SHOW PASSWORD */}

          <label className="flex items-center gap-2 text-sm text-slate-600">
            <input
              type="checkbox"
              checked={showPassword}
              onChange={(e) =>
                setShowPassword(
                  e.target.checked
                )
              }
            />

            Show passwords
          </label>

          {/* SUBMIT */}

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-slate-900 py-3 font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading
              ? "Resetting Password..."
              : "Reset Password"}
          </button>
        </form>
      </div>
    </main>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense
      fallback={
        <main className="min-h-screen flex items-center justify-center bg-slate-50">
          <p className="text-slate-600">
            Loading...
          </p>
        </main>
      }
    >
      <ResetPasswordForm />
    </Suspense>
  );
}