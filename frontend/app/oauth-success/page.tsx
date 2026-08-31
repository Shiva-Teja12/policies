"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

export default function OAuthSuccessPage() {
  const router = useRouter();

  const [error, setError] =
    useState("");

  useEffect(() => {
    try {
      // =====================================================
      // READ GOOGLE OAUTH RESPONSE
      // =====================================================

      const fragment =
        window.location.hash.substring(1);

      const params =
        new URLSearchParams(fragment);

      const token =
        params.get("token");

      const userId =
        params.get("userId");

      const name =
        params.get("name");

      const email =
        params.get("email");

      const role =
        params.get("role");

      const dashboardPath =
        params.get("dashboardPath");

      // =====================================================
      // VALIDATE RESPONSE
      // =====================================================

      if (
        !token ||
        !userId ||
        !email ||
        !role
      ) {
        setError(
          "Google authentication response is incomplete."
        );

        return;
      }

      // =====================================================
      // STORE JWT
      // =====================================================

      localStorage.setItem(
        "token",
        token
      );

      // =====================================================
      // STORE USER
      //
      // IMPORTANT:
      // This now uses exactly the same structure as your
      // normal email/password login.
      // =====================================================

      localStorage.setItem(
        "user",
        JSON.stringify({
          userId:
            Number(userId),

          name:
            name || "",

          email,

          role,

          dashboardPath:
            dashboardPath || "",
        })
      );

      // =====================================================
      // REMOVE OLD SEPARATE STORAGE KEYS
      //
      // This cleans values created by the previous version.
      // =====================================================

      localStorage.removeItem(
        "userId"
      );

      localStorage.removeItem(
        "name"
      );

      localStorage.removeItem(
        "email"
      );

      localStorage.removeItem(
        "role"
      );

      // =====================================================
      // REMOVE JWT FROM VISIBLE URL
      // =====================================================

      window.history.replaceState(
        {},
        document.title,
        "/oauth-success"
      );

      // =====================================================
      // REDIRECT TO CORRECT DASHBOARD
      // =====================================================

      router.replace(
        dashboardPath ||
          "/employee/dashboard"
      );
    } catch {
      setError(
        "Unable to complete Google login."
      );
    }
  }, [router]);

  // =========================================================
  // ERROR SCREEN
  // =========================================================

  if (error) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-slate-50 p-6">

        <div className="w-full max-w-md rounded-2xl border border-red-200 bg-white p-8 shadow-sm">

          <h1 className="text-xl font-bold text-red-700">
            Google login failed
          </h1>

          <p className="mt-3 text-slate-600">
            {error}
          </p>

          <button
            type="button"
            onClick={() =>
              router.replace(
                "/login"
              )
            }
            className="mt-6 rounded-xl bg-blue-700 px-5 py-3 font-semibold text-white hover:bg-blue-800"
          >
            Return to Login
          </button>

        </div>

      </main>
    );
  }

  // =========================================================
  // LOADING SCREEN
  // =========================================================

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50">

      <div className="text-center">

        <div className="mx-auto h-10 w-10 animate-spin rounded-full border-4 border-slate-200 border-t-blue-700" />

        <h1 className="mt-5 text-xl font-bold text-slate-900">
          Signing you in...
        </h1>

        <p className="mt-2 text-slate-500">
          Completing Google authentication.
        </p>

      </div>

    </main>
  );
}