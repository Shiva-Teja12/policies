"use client";

import Link from "next/link";
import {
  Bell,
  BookOpen,
  CheckSquare,
  FileCheck2,
  FilePlus2,
  FolderTree,
  Gauge,
  LogOut,
  Menu,
  Rocket,
  ShieldCheck,
  Users,
  X,
} from "lucide-react";
import { usePathname } from "next/navigation";
import {
  ReactNode,
  useEffect,
  useState,
} from "react";
import {
  getSession,
  logout,
  roleLabel,
  UserRole,
  UserSession,
} from "@/lib/auth";

interface NavigationItem {
  label: string;
  href: string;
  icon: React.ElementType;
}

interface PortalLayoutProps {
  children: ReactNode;
  role: UserRole;
}

const navigation: Record<
  UserRole,
  NavigationItem[]
> = {
  HR_ADMIN: [
    {
      label: "Dashboard",
      href: "/hr-admin/dashboard",
      icon: Gauge,
    },
    {
      label: "Policies",
      href: "/hr-admin/policies",
      icon: BookOpen,
    },
    {
      label: "Create Policy",
      href: "/hr-admin/policies/create",
      icon: FilePlus2,
    },
    {
      label: "Categories",
      href: "/hr-admin/categories",
      icon: FolderTree,
    },
    {
      label: "Approvals",
      href: "/hr-admin/approvals",
      icon: ShieldCheck,
    },
    {
      label: "Ready to Publish",
      href: "/hr-admin/ready-to-publish",
      icon: Rocket,
    },
    {
      label: "Compliance",
      href: "/hr-admin/compliance",
      icon: Users,
    },
    {
      label: "Reminders",
      href: "/hr-admin/reminders",
      icon: Bell,
    },
    {
      label: "Notifications",
      href: "/notifications",
      icon: Bell,
    },
  ],

  LEGAL_REVIEWER: [
    {
      label: "Review Queue",
      href: "/legal-reviewer/dashboard",
      icon: ShieldCheck,
    },
    {
      label: "Notifications",
      href: "/notifications",
      icon: Bell,
    },
  ],

  HR_HEAD: [
    {
      label: "Review Queue",
      href: "/hr-head/dashboard",
      icon: CheckSquare,
    },
    {
      label: "Notifications",
      href: "/notifications",
      icon: Bell,
    },
  ],

  MANAGING_DIRECTOR: [
    {
      label: "Final Approvals",
      href: "/managing-director/dashboard",
      icon: FileCheck2,
    },
    {
      label: "Notifications",
      href: "/notifications",
      icon: Bell,
    },
  ],

  MANAGER: [
    {
      label: "Published Policies",
      href: "/manager/dashboard",
      icon: BookOpen,
    },
    {
      label: "Notifications",
      href: "/notifications",
      icon: Bell,
    },
  ],

  EMPLOYEE: [
    {
      label: "My Policies",
      href: "/employee/dashboard",
      icon: BookOpen,
    },
    {
      label: "Notifications",
      href: "/notifications",
      icon: Bell,
    },
  ],
};

export default function PortalLayout({
  children,
  role,
}: PortalLayoutProps) {
  const pathname = usePathname();

  const [session, setSession] =
    useState<UserSession | null>(null);

  const [mobileOpen, setMobileOpen] =
    useState(false);

  const [collapsed, setCollapsed] =
    useState(false);

  useEffect(() => {
    setSession(getSession());
  }, []);

  useEffect(() => {
    setMobileOpen(false);
  }, [pathname]);

  const roleNavigation = navigation[role];

  /*
   * Select the longest matching route.
   *
   * Example:
   * /hr-admin/policies/create matches both:
   * - /hr-admin/policies
   * - /hr-admin/policies/create
   *
   * The longest route is selected, so only
   * Create Policy is highlighted.
   */
  const activeNavigationHref =
    roleNavigation
      .filter(
        (item) =>
          pathname === item.href ||
          pathname.startsWith(`${item.href}/`)
      )
      .sort(
        (first, second) =>
          second.href.length - first.href.length
      )[0]?.href;

  const sidebarContent = (
    <>
      <div className="flex h-20 items-center justify-between border-b border-white/10 px-5">
        {!collapsed && (
          <div>
            <p className="text-xl font-bold text-white">
              HRMS
            </p>

            <p className="text-xs text-blue-200">
              Policies Module
            </p>
          </div>
        )}

        <button
          type="button"
          onClick={() =>
            setCollapsed((current) => !current)
          }
          className="hidden rounded-lg p-2 text-blue-100 hover:bg-white/10 lg:block"
          aria-label={
            collapsed
              ? "Expand sidebar"
              : "Collapse sidebar"
          }
        >
          <Menu size={20} />
        </button>

        <button
          type="button"
          onClick={() => setMobileOpen(false)}
          className="rounded-lg p-2 text-blue-100 hover:bg-white/10 lg:hidden"
          aria-label="Close navigation"
        >
          <X size={20} />
        </button>
      </div>

      <nav className="flex-1 space-y-1 overflow-y-auto p-3">
        {roleNavigation.map((item) => {
          const Icon = item.icon;

          const active =
            item.href === activeNavigationHref;

          return (
            <Link
              key={`${item.href}-${item.label}`}
              href={item.href}
              title={
                collapsed ? item.label : undefined
              }
              className={`flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-semibold transition ${
                active
                  ? "bg-blue-500 text-white shadow-sm"
                  : "text-blue-100 hover:bg-white/10 hover:text-white"
              }`}
            >
              <Icon
                size={20}
                className="shrink-0"
              />

              {!collapsed && (
                <span>{item.label}</span>
              )}
            </Link>
          );
        })}
      </nav>

      <div className="border-t border-white/10 p-3">
        {!collapsed && (
          <div className="mb-3 rounded-xl bg-white/5 p-3">
            <p className="truncate text-sm font-semibold text-white">
              {session?.name || roleLabel(role)}
            </p>

            <p className="mt-1 truncate text-xs text-blue-200">
              {session?.email || "Authenticated user"}
            </p>

            <p className="mt-1 text-xs font-semibold text-blue-300">
              {roleLabel(role)}
            </p>
          </div>
        )}

        <button
          type="button"
          onClick={logout}
          title={collapsed ? "Sign out" : undefined}
          className="flex w-full items-center gap-3 rounded-xl px-3 py-3 text-sm font-semibold text-red-200 transition hover:bg-red-500/20 hover:text-white"
        >
          <LogOut
            size={20}
            className="shrink-0"
          />

          {!collapsed && <span>Sign out</span>}
        </button>
      </div>
    </>
  );

  return (
    <div className="min-h-screen bg-slate-50">
      <aside
        className={`fixed inset-y-0 left-0 z-40 hidden flex-col bg-[#082b5c] transition-all duration-200 lg:flex ${
          collapsed ? "w-20" : "w-72"
        }`}
      >
        {sidebarContent}
      </aside>

      {mobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            type="button"
            onClick={() => setMobileOpen(false)}
            className="absolute inset-0 bg-black/60"
            aria-label="Close navigation overlay"
          />

          <aside className="relative flex h-full w-72 flex-col bg-[#082b5c] shadow-2xl">
            {sidebarContent}
          </aside>
        </div>
      )}

      <div
        className={`min-w-0 transition-all duration-200 ${
          collapsed
            ? "lg:pl-20"
            : "lg:pl-72"
        }`}
      >
        <div className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-slate-200 bg-white px-4 lg:hidden">
          <button
            type="button"
            onClick={() => setMobileOpen(true)}
            className="rounded-lg border border-slate-300 p-2"
            aria-label="Open navigation"
          >
            <Menu size={20} />
          </button>

          <p className="font-bold text-slate-900">
            {roleLabel(role)}
          </p>

          <Link
            href="/notifications"
            className="rounded-lg border border-slate-300 p-2"
            aria-label="Notifications"
          >
            <Bell size={20} />
          </Link>
        </div>

        <div className="min-w-0">
          {children}
        </div>
      </div>
    </div>
  );
}