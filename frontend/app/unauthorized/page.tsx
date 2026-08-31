"use client";

import {
  dashboardForRole,
  getSession,
} from "@/lib/auth";
import Link from "next/link";

export default function UnauthorizedPage() {
  const session = getSession();

  const dashboard = session
    ? dashboardForRole(
        session.role
      )
    : "/login";

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-100 px-5">
      <div className="w-full max-w-lg rounded-3xl border border-slate-200 bg-white p-10 text-center shadow-xl">
        <div className="text-6xl">
          🔒
        </div>

        <h1 className="mt-6 text-3xl font-bold text-slate-950">
          Access denied
        </h1>

        <p className="mt-3 leading-7 text-slate-600">
          Your account does not have
          permission to access this portal
          or function.
        </p>

        <Link
          href={dashboard}
          className="mt-7 inline-flex rounded-xl bg-blue-700 px-6 py-3 font-bold text-white hover:bg-blue-800"
        >
          Return to my dashboard
        </Link>
      </div>
    </main>
  );
}