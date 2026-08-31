"use client";

import Link from "next/link";
import {
  FormEvent,
  useState,
} from "react";

const SUPPORT_EMAIL =
  "support@enfec.com";

const issueTypes = [
  "Unable to sign in",
  "Forgot password",
  "Account locked",
  "Incorrect role",
  "Cannot access a policy",
  "Policy acknowledgement problem",
  "Other",
];

export default function SupportPage() {
  const [name, setName] =
    useState("");

  const [email, setEmail] =
    useState("");

  const [issueType, setIssueType] =
    useState(issueTypes[0]);

  const [subject, setSubject] =
    useState("");

  const [description, setDescription] =
    useState("");

  const [error, setError] =
    useState("");

  function handleSubmit(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    setError("");

    if (
      !name.trim() ||
      !email.trim() ||
      !subject.trim() ||
      !description.trim()
    ) {
      setError(
        "Complete all required fields."
      );
      return;
    }

    const emailSubject =
      `[HRMS Support] ${issueType}: ${subject}`;

    const emailBody = [
      `Name: ${name}`,
      `Company email: ${email}`,
      `Issue type: ${issueType}`,
      "",
      "Description:",
      description,
    ].join("\n");

    const mailtoUrl =
      `mailto:${SUPPORT_EMAIL}` +
      `?subject=${encodeURIComponent(
        emailSubject
      )}` +
      `&body=${encodeURIComponent(
        emailBody
      )}`;

    window.location.href = mailtoUrl;
  }

  return (
    <main className="min-h-screen bg-slate-100 px-5 py-10">
      <div className="mx-auto max-w-5xl">
        <div className="mb-6">
          <Link
            href="/login"
            className="inline-flex items-center gap-2 text-sm font-semibold text-blue-700 hover:underline"
          >
            ← Return to login
          </Link>
        </div>

        <div className="grid overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-xl lg:grid-cols-[38%_62%]">
          <section className="bg-[#082b5c] p-8 text-white sm:p-10">
            <div className="text-4xl font-bold">
              HRMS
            </div>

            <p className="mt-2 text-blue-100">
              Policies Module Support
            </p>

            <div className="mt-10 text-6xl">
              🎧
            </div>

            <h1 className="mt-6 text-3xl font-bold">
              How can we help?
            </h1>

            <p className="mt-4 leading-7 text-blue-100">
              Contact the support team if you
              cannot sign in or access the
              correct policy functions.
            </p>

            <div className="mt-10 rounded-2xl border border-blue-300/30 bg-white/5 p-5">
              <p className="text-sm font-semibold text-blue-100">
                Support email
              </p>

              <a
                href={`mailto:${SUPPORT_EMAIL}`}
                className="mt-2 block font-bold text-white hover:underline"
              >
                {SUPPORT_EMAIL}
              </a>
            </div>

            <div className="mt-4 rounded-2xl border border-blue-300/30 bg-white/5 p-5">
              <p className="text-sm font-semibold text-blue-100">
                Working hours
              </p>

              <p className="mt-2 font-bold">
                Monday–Friday
              </p>

              <p className="mt-1 text-sm text-blue-100">
                9:00 AM–6:00 PM IST
              </p>
            </div>
          </section>

          <section className="p-7 sm:p-10">
            <h2 className="text-3xl font-bold text-slate-950">
              Contact Support
            </h2>

            <p className="mt-2 text-slate-600">
              Enter the details of the issue
              you are experiencing.
            </p>

            {error && (
              <div className="mt-6 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                {error}
              </div>
            )}

            <form
              onSubmit={handleSubmit}
              className="mt-7 space-y-5"
            >
              <div className="grid gap-5 sm:grid-cols-2">
                <div>
                  <label
                    htmlFor="name"
                    className="mb-2 block text-sm font-semibold text-slate-700"
                  >
                    Full name *
                  </label>

                  <input
                    id="name"
                    value={name}
                    placeholder="Enter your full name"
                    onChange={(event) =>
                      setName(
                        event.target.value
                      )
                    }
                    className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
                  />
                </div>

                <div>
                  <label
                    htmlFor="email"
                    className="mb-2 block text-sm font-semibold text-slate-700"
                  >
                    Company email *
                  </label>

                  <input
                    id="email"
                    type="email"
                    value={email}
                    placeholder="name@company.com"
                    onChange={(event) =>
                      setEmail(
                        event.target.value
                      )
                    }
                    className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
                  />
                </div>
              </div>

              <div>
                <label
                  htmlFor="issueType"
                  className="mb-2 block text-sm font-semibold text-slate-700"
                >
                  Issue type *
                </label>

                <select
                  id="issueType"
                  value={issueType}
                  onChange={(event) =>
                    setIssueType(
                      event.target.value
                    )
                  }
                  className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
                >
                  {issueTypes.map(
                    (issue) => (
                      <option
                        key={issue}
                        value={issue}
                      >
                        {issue}
                      </option>
                    )
                  )}
                </select>
              </div>

              <div>
                <label
                  htmlFor="subject"
                  className="mb-2 block text-sm font-semibold text-slate-700"
                >
                  Subject *
                </label>

                <input
                  id="subject"
                  value={subject}
                  placeholder="Briefly describe the issue"
                  onChange={(event) =>
                    setSubject(
                      event.target.value
                    )
                  }
                  className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
                />
              </div>

              <div>
                <label
                  htmlFor="description"
                  className="mb-2 block text-sm font-semibold text-slate-700"
                >
                  Description *
                </label>

                <textarea
                  id="description"
                  value={description}
                  rows={6}
                  placeholder="Explain what happened and include any error message you received"
                  onChange={(event) =>
                    setDescription(
                      event.target.value
                    )
                  }
                  className="w-full resize-none rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-blue-600 focus:ring-4 focus:ring-blue-100"
                />
              </div>

              <button
                type="submit"
                className="w-full rounded-xl bg-blue-700 px-5 py-3.5 font-bold text-white hover:bg-blue-800"
              >
                Email Support Request
              </button>

              <p className="text-center text-xs leading-5 text-slate-500">
                Clicking the button opens your
                configured email application with
                the support request information.
              </p>
            </form>
          </section>
        </div>
      </div>
    </main>
  );
}