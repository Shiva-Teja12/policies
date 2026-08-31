"use client";

import { useState } from "react";
import Link from "next/link";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleResetPassword() {
    setError("");
    setSuccess("");

    // =========================
    // FRONTEND VALIDATION
    // =========================

    if (!email.trim()) {
      setError("Please enter your email address.");
      return;
    }

    if (!newPassword) {
      setError("Please enter a new password.");
      return;
    }

    if (newPassword.length < 6) {
      setError("Password must be at least 6 characters.");
      return;
    }

    if (!confirmPassword) {
      setError("Please confirm your new password.");
      return;
    }

    if (newPassword !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setLoading(true);

    try {
      // =========================
      // CALL SPRING BOOT BACKEND
      // =========================

      const response = await fetch(
        "http://localhost:8080/api/auth/reset-password",
        {
          method: "POST",

          headers: {
            "Content-Type": "application/json",
          },

          body: JSON.stringify({
            email: email.trim().toLowerCase(),
            newPassword: newPassword,
            confirmPassword: confirmPassword,
          }),
        }
      );

      const responseText = await response.text();

      console.log("Reset password status:", response.status);
      console.log("Reset password response:", responseText);

      let data: {
        message?: string;
      } = {};

      if (responseText) {
        try {
          data = JSON.parse(responseText);
        } catch {
          // Backend may return plain text
          data = {
            message: responseText,
          };
        }
      }

      // =========================
      // BACKEND ERROR
      // =========================

      if (!response.ok) {
        throw new Error(
          data.message || "Unable to reset password."
        );
      }

      // =========================
      // SUCCESS
      // =========================

      setSuccess(
        data.message ||
          "Password reset successfully. You can now login with your new password."
      );

      // Clear fields
      setEmail("");
      setNewPassword("");
      setConfirmPassword("");

    } catch (error) {
      console.error("Reset password error:", error);

      if (error instanceof TypeError) {
        setError(
          "Unable to connect to the backend. Make sure Spring Boot is running on port 8080."
        );
      } else if (error instanceof Error) {
        setError(error.message);
      } else {
        setError("Unable to reset password. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-100 px-4">

      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-lg">

        {/* Header */}

        <div className="mb-8 text-center">

          <h1 className="text-3xl font-bold text-gray-900">
            Reset Password
          </h1>

          <p className="mt-2 text-sm text-gray-500">
            Enter your email and create a new password.
          </p>

        </div>

        {/* Error */}

        {error && (
          <div className="mb-5 rounded-lg bg-red-50 p-3 text-sm text-red-600">
            {error}
          </div>
        )}

        {/* Success */}

        {success && (
          <div className="mb-5 rounded-lg bg-green-50 p-3 text-sm text-green-600">
            {success}
          </div>
        )}

        <div className="space-y-5">

          {/* Email */}

          <div>

            <label
              htmlFor="email"
              className="mb-2 block text-sm font-medium text-gray-700"
            >
              Email Address
            </label>

            <input
              id="email"
              type="email"
              value={email}
              placeholder="Enter your registered email"
              autoComplete="email"
              onChange={(event) =>
                setEmail(event.target.value)
              }
              className="w-full rounded-lg border border-gray-300 px-4 py-3 outline-none focus:border-blue-500"
            />

          </div>

          {/* New Password */}

          <div>

            <label
              htmlFor="newPassword"
              className="mb-2 block text-sm font-medium text-gray-700"
            >
              Create New Password
            </label>

            <input
              id="newPassword"
              type="password"
              value={newPassword}
              placeholder="Enter new password"
              autoComplete="new-password"
              onChange={(event) =>
                setNewPassword(event.target.value)
              }
              className="w-full rounded-lg border border-gray-300 px-4 py-3 outline-none focus:border-blue-500"
            />

          </div>

          {/* Confirm Password */}

          <div>

            <label
              htmlFor="confirmPassword"
              className="mb-2 block text-sm font-medium text-gray-700"
            >
              Confirm New Password
            </label>

            <input
              id="confirmPassword"
              type="password"
              value={confirmPassword}
              placeholder="Confirm new password"
              autoComplete="new-password"
              onChange={(event) =>
                setConfirmPassword(event.target.value)
              }
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  handleResetPassword();
                }
              }}
              className="w-full rounded-lg border border-gray-300 px-4 py-3 outline-none focus:border-blue-500"
            />

          </div>

          {/* Reset Button */}

          <button
            type="button"
            onClick={handleResetPassword}
            disabled={loading}
            className="w-full rounded-lg bg-blue-600 py-3 font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-blue-400"
          >
            {loading
              ? "Resetting Password..."
              : "Reset Password"}
          </button>

        </div>

        {/* Back to Login */}

        <div className="mt-6 text-center">

          <Link
            href="/login"
            className="text-sm font-semibold text-blue-600 hover:underline"
          >
            ← Back to Login
          </Link>

        </div>

      </div>

    </main>
  );
}