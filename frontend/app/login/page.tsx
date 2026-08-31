"use client";

import Link from "next/link";
import {
  FormEvent,
  useEffect,
  useState,
} from "react";
import {
  useRouter,
  useSearchParams,
} from "next/navigation";

type Role =
  | "HR_ADMIN"
  | "LEGAL_REVIEWER"
  | "HR_HEAD"
  | "MANAGING_DIRECTOR"
  | "MANAGER"
  | "EMPLOYEE";

interface Portal {
  role: Role;
  title: string;
  description: string;
  icon: string;
}

interface LoginResponse {
  token?: string;
  message?: string;
  userId?: number;
  name?: string;
  email?: string;
  role?: Role;
  dashboardPath?: string;
}

const API_URL =
  process.env.NEXT_PUBLIC_API_URL ||
  "http://localhost:8080";

const portals: Portal[] = [
  {
    role: "HR_ADMIN",
    title: "HR Admin",
    description:
      "Create, publish and manage policies",
    icon: "⚙",
  },
  {
    role: "LEGAL_REVIEWER",
    title: "Legal Reviewer",
    description:
      "Perform legal policy reviews",
    icon: "⚖",
  },
  {
    role: "HR_HEAD",
    title: "HR Head",
    description:
      "Review HR policies and compliance",
    icon: "♙",
  },
  {
    role: "MANAGING_DIRECTOR",
    title: "Managing Director",
    description:
      "Final company-wide approval",
    icon: "▣",
  },
  {
    role: "MANAGER",
    title: "Manager",
    description:
      "Monitor team compliance",
    icon: "♧",
  },
  {
    role: "EMPLOYEE",
    title: "Employee",
    description:
      "Read and acknowledge policies",
    icon: "♙",
  },
];

export default function LoginPage() {
  const router = useRouter();

  const searchParams =
    useSearchParams();

  const [selectedPortal, setSelectedPortal] =
    useState<Portal>(portals[0]);

  const [email, setEmail] =
    useState("");

  const [password, setPassword] =
    useState("");

  const [
    showPassword,
    setShowPassword,
  ] = useState(false);

  const [error, setError] =
    useState("");

  const [loading, setLoading] =
    useState(false);

  // =========================================================
  // GOOGLE OAUTH ERROR HANDLING
  // =========================================================

  useEffect(() => {
    const oauthError =
      searchParams.get(
        "oauthError"
      );

    if (oauthError) {
      setError(
        oauthError
      );
    }
  }, [searchParams]);

  // =========================================================
  // NORMAL EMAIL + PASSWORD LOGIN
  // =========================================================

  async function handleLogin(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    setError("");

    if (
      !email.trim() ||
      !password
    ) {
      setError(
        "Enter your email and password."
      );

      return;
    }

    setLoading(true);

    try {
      const response =
        await fetch(
          `${API_URL}/api/auth/login`,
          {
            method: "POST",

            headers: {
              "Content-Type":
                "application/json",
            },

            body: JSON.stringify({
              email:
                email.trim(),
              password,
              selectedRole:
                selectedPortal.role,
            }),
          }
        );

      const text =
        await response.text();

      let data: LoginResponse =
        {};

      if (text) {
        try {
          data =
            JSON.parse(text);
        } catch {
          throw new Error(
            "Invalid response from backend."
          );
        }
      }

      if (!response.ok) {
        throw new Error(
          data.message ||
            "Login failed."
        );
      }

      if (
        !data.token ||
        !data.role ||
        !data.dashboardPath
      ) {
        throw new Error(
          "The backend returned an incomplete login response."
        );
      }

      // =====================================================
      // STORE JWT
      // =====================================================

      localStorage.setItem(
        "token",
        data.token
      );

      // =====================================================
      // STORE USER
      // =====================================================

      localStorage.setItem(
        "user",
        JSON.stringify({
          userId:
            data.userId,

          name:
            data.name,

          email:
            data.email,

          role:
            data.role,

          dashboardPath:
            data.dashboardPath,
        })
      );

      // =====================================================
      // REDIRECT TO ROLE DASHBOARD
      // =====================================================

      router.replace(
        data.dashboardPath
      );
    } catch (
      requestError
    ) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to login."
      );
    } finally {
      setLoading(false);
    }
  }

  // =========================================================
  // GOOGLE OAUTH LOGIN
  // =========================================================

  function handleGoogleLogin() {
    setError("");

    /*
     * Redirect the browser
     * to Spring Boot.
     *
     * Spring Security then
     * redirects the browser
     * to Google's OAuth page.
     */

    window.location.href =
      `${API_URL}/oauth2/authorization/google`;
  }

  // =========================================================
  // SELECT PORTAL
  // =========================================================

  function choosePortal(
    portal: Portal
  ) {
    setSelectedPortal(
      portal
    );

    setError("");
  }

  return (
    <main className="min-h-screen bg-slate-50 lg:grid lg:grid-cols-[38%_62%]">

      {/* =====================================================
          LEFT SIDE
          ===================================================== */}

      <section className="hidden min-h-screen bg-[#082b5c] p-12 text-white lg:flex lg:flex-col lg:justify-between">

        <div>
          <h1 className="text-6xl font-bold tracking-tight">
            HRMS
          </h1>

          <p className="mt-3 text-lg text-blue-100">
            Human Resource Management System
          </p>

          <div className="mt-12 h-1 w-24 rounded bg-blue-500" />

          <h2 className="mt-10 text-4xl font-bold">
            Policies Module
          </h2>

          <p className="mt-5 max-w-md text-xl leading-8 text-blue-100">
            Secure role-based access for every
            member of your organisation.
          </p>
        </div>

        <div className="rounded-3xl border border-blue-300/30 bg-white/5 p-8">

          <div className="text-6xl">
            🛡
          </div>

          <p className="mt-5 text-lg text-blue-100">
            Authentication and API permissions
            are checked for every protected
            operation.
          </p>

        </div>

      </section>

      {/* =====================================================
          RIGHT SIDE
          ===================================================== */}

      <section className="min-h-screen overflow-y-auto px-5 py-8 sm:px-10 lg:px-14">

        <div className="mx-auto max-w-5xl">

          {/* =================================================
              HEADER
              ================================================= */}

          <div className="flex items-start justify-between gap-6">

            <div>
              <h1 className="text-3xl font-bold text-slate-950 sm:text-4xl">
                Choose your portal
              </h1>

              <p className="mt-2 text-slate-600">
                Select your role to continue
                securely.
              </p>
            </div>

            <Link
              href="/support"
              className="hidden text-sm font-semibold text-blue-700 hover:underline sm:block"
            >
              Need help? Contact support
            </Link>

          </div>

          {/* =================================================
              PORTAL SELECTION
              ================================================= */}

          <div className="mt-7 grid grid-cols-2 gap-3 md:grid-cols-3">

            {portals.map(
              (portal) => {

                const selected =
                  selectedPortal.role ===
                  portal.role;

                return (
                  <button
                    key={
                      portal.role
                    }
                    type="button"

                    onClick={() =>
                      choosePortal(
                        portal
                      )
                    }

                    className={`relative rounded-2xl border p-4 text-left transition ${
                      selected
                        ? "border-blue-600 bg-blue-50 shadow-sm ring-2 ring-blue-100"
                        : "border-slate-200 bg-white hover:border-blue-300 hover:bg-slate-50"
                    }`}
                  >

                    {selected && (
                      <span className="absolute right-3 top-3 flex h-6 w-6 items-center justify-center rounded-full bg-blue-600 text-xs text-white">
                        ✓
                      </span>
                    )}

                    <span className="text-3xl">
                      {
                        portal.icon
                      }
                    </span>

                    <span className="mt-3 block font-bold text-slate-900">
                      {
                        portal.title
                      }
                    </span>

                    <span className="mt-1 hidden text-xs leading-5 text-slate-500 sm:block">
                      {
                        portal.description
                      }
                    </span>

                  </button>
                );
              }
            )}

          </div>

          {/* =================================================
              LOGIN FORM
              ================================================= */}

          <form
            onSubmit={
              handleLogin
            }
            className="mt-7 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8"
          >

            <h2 className="text-2xl font-bold text-slate-950">
              Sign in as{" "}
              {
                selectedPortal.title
              }
            </h2>

            <p className="mt-2 text-sm text-slate-500">
              Enter your account credentials
              or continue securely with Google.
            </p>

            {/* ===============================================
                ERROR
                =============================================== */}

            {error && (
              <div className="mt-5 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">

                {error}

              </div>
            )}

            {/* ===============================================
                EMAIL
                =============================================== */}

            <div className="mt-6">

              <label
                htmlFor="email"
                className="mb-2 block text-sm font-semibold text-slate-700"
              >
                Email address
              </label>

              <input
                id="email"
                type="email"

                value={
                  email
                }

                autoComplete="email"

                placeholder="Enter your company email"

                onChange={(
                  event
                ) =>
                  setEmail(
                    event.target.value
                  )
                }

                className="w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-950 outline-none transition focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
              />

            </div>

            {/* ===============================================
                PASSWORD
                =============================================== */}

            <div className="mt-5">

              <label
                htmlFor="password"
                className="mb-2 block text-sm font-semibold text-slate-700"
              >
                Password
              </label>

              <div className="relative">

                <input
                  id="password"

                  type={
                    showPassword
                      ? "text"
                      : "password"
                  }

                  value={
                    password
                  }

                  autoComplete="current-password"

                  placeholder="Enter your password"

                  onChange={(
                    event
                  ) =>
                    setPassword(
                      event.target.value
                    )
                  }

                  className="w-full rounded-xl border border-slate-300 px-4 py-3 pr-16 text-slate-950 outline-none transition focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
                />

                <button
                  type="button"

                  onClick={() =>
                    setShowPassword(
                      (
                        current
                      ) =>
                        !current
                    )
                  }

                  className="absolute inset-y-0 right-0 px-4 text-sm font-semibold text-blue-700"
                >
                  {
                    showPassword
                      ? "Hide"
                      : "Show"
                  }
                </button>

              </div>

            </div>

            {/* ===============================================
                REMEMBER + FORGOT PASSWORD
                =============================================== */}

            <div className="mt-4 flex items-center justify-between gap-4">

              <label className="flex items-center gap-2 text-sm text-slate-600">

                <input
                  type="checkbox"
                  className="h-4 w-4"
                />

                Remember me

              </label>

              <Link
                href="/forgot-password"
                className="text-sm font-semibold text-blue-700 hover:underline"
              >
                Forgot password?
              </Link>

            </div>

            {/* ===============================================
                NORMAL LOGIN BUTTON
                =============================================== */}

            <button
              type="submit"

              disabled={
                loading
              }

              className="mt-6 w-full rounded-xl bg-blue-700 px-5 py-3.5 font-bold text-white transition hover:bg-blue-800 disabled:cursor-not-allowed disabled:bg-blue-400"
            >
              {
                loading
                  ? "Signing in..."
                  : `Sign in to ${selectedPortal.title} Portal`
              }
            </button>

            {/* ===============================================
                OR DIVIDER
                =============================================== */}

            <div className="my-6 flex items-center gap-4">

              <div className="h-px flex-1 bg-slate-200" />

              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Or
              </span>

              <div className="h-px flex-1 bg-slate-200" />

            </div>

            {/* ===============================================
                GOOGLE OAUTH BUTTON
                =============================================== */}

            <button
              type="button"

              onClick={
                handleGoogleLogin
              }

              className="flex w-full items-center justify-center gap-3 rounded-xl border border-slate-300 bg-white px-5 py-3.5 font-semibold text-slate-700 transition hover:border-slate-400 hover:bg-slate-50"
            >

              <span className="flex h-6 w-6 items-center justify-center rounded-full border border-slate-200 bg-white text-sm font-bold">
                G
              </span>

              <span>
                Continue with Google
              </span>

            </button>

            {/* ===============================================
                OAUTH INFORMATION
                =============================================== */}

            <p className="mt-3 text-center text-xs leading-5 text-slate-500">
              Google sign-in uses the role assigned
              to your HRMS account.
            </p>

            {/* ===============================================
                EMPLOYEE SIGNUP
                =============================================== */}

            {
              selectedPortal.role ===
                "EMPLOYEE" && (

                <p className="mt-5 text-center text-sm text-slate-600">

                  New employee?{" "}

                  <Link
                    href="/signup"
                    className="font-bold text-blue-700 hover:underline"
                  >
                    Create an account
                  </Link>

                </p>
              )
            }

            {/* ===============================================
                SECURITY MESSAGE
                =============================================== */}

            <div className="mt-6 flex items-center justify-center gap-2 text-center text-sm text-green-700">

              <span>
                🔒
              </span>

              <span>
                Your account role will be verified
                before access is granted.
              </span>

            </div>

          </form>

          {/* =================================================
              MOBILE SUPPORT LINK
              ================================================= */}

          <Link
            href="/support"
            className="mt-6 block text-center text-sm font-semibold text-blue-700 sm:hidden"
          >
            Need help? Contact support
          </Link>

        </div>

      </section>

    </main>
  );
}