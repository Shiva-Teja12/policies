"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";

const API_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export default function ResetPasswordPage() {

  const searchParams =
    useSearchParams();

  const token =
    searchParams.get("token") || "";

  const [newPassword, setNewPassword] =
    useState("");

  const [confirmPassword, setConfirmPassword] =
    useState("");

  const [error, setError] =
    useState("");

  const [success, setSuccess] =
    useState("");

  const [loading, setLoading] =
    useState(false);

  useEffect(() => {

    if (!token) {
      setError(
        "Invalid password reset link."
      );
    }

  }, [token]);

  async function handleResetPassword() {

    setError("");
    setSuccess("");

    if (!token) {
      setError(
        "Invalid password reset link."
      );
      return;
    }

    if (
      !newPassword ||
      !confirmPassword
    ) {
      setError(
        "Please enter and confirm your new password."
      );
      return;
    }

    if (newPassword.length < 6) {
      setError(
        "Password must be at least 6 characters."
      );
      return;
    }

    if (
      newPassword !== confirmPassword
    ) {
      setError(
        "Passwords do not match."
      );
      return;
    }

    setLoading(true);

    try {

      const response = await fetch(
        `${API_URL}/api/auth/reset-password`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            token,
            newPassword,
            confirmPassword,
          }),
        }
      );

      const text =
        await response.text();

      if (!response.ok) {

        let message =
          "Unable to reset password.";

        try {
          message =
            JSON.parse(text).message ||
            message;
        } catch {}

        throw new Error(message);
      }

      setSuccess(
        text ||
        "Password updated successfully."
      );

      setNewPassword("");
      setConfirmPassword("");

    } catch (error) {

      setError(
        error instanceof Error
          ? error.message
          : "Unable to reset password."
      );

    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-100 px-4">

      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-lg">

        <h1 className="text-center text-3xl font-bold">
          Create New Password
        </h1>

        <p className="mt-2 text-center text-sm text-gray-500">
          Enter your new password and confirm it.
        </p>

        {error && (
          <div className="mt-5 rounded-lg bg-red-50 p-3 text-sm text-red-600">
            {error}
          </div>
        )}

        {success && (
          <div className="mt-5 rounded-lg bg-green-50 p-3 text-sm text-green-600">
            {success}
          </div>
        )}

        <div className="mt-6 space-y-4">

          <input
            type="password"
            value={newPassword}
            onChange={(e) =>
              setNewPassword(e.target.value)
            }
            placeholder="Create new password"
            className="w-full rounded-lg border px-4 py-3"
          />

          <input
            type="password"
            value={confirmPassword}
            onChange={(e) =>
              setConfirmPassword(e.target.value)
            }
            placeholder="Confirm new password"
            className="w-full rounded-lg border px-4 py-3"
          />

          <button
            onClick={handleResetPassword}
            disabled={
              loading || !token
            }
            className="w-full rounded-lg bg-blue-600 py-3 font-semibold text-white disabled:bg-blue-400"
          >
            {loading
              ? "Updating..."
              : "Update Password"}
          </button>

        </div>

        {success && (
          <div className="mt-5 text-center">

            <Link
              href="/login"
              className="font-semibold text-blue-600"
            >
              Go to Login
            </Link>

          </div>
        )}

      </div>

    </main>
  );
}