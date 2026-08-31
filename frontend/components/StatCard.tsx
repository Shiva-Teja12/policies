import { ReactNode } from "react";

interface StatCardProps {
  label: string;
  value: string | number;
  description?: string;
  icon?: ReactNode;
  tone?:
    | "blue"
    | "green"
    | "amber"
    | "red"
    | "purple";
}

const tones = {
  blue: "bg-blue-50 text-blue-700",
  green:
    "bg-green-50 text-green-700",
  amber:
    "bg-amber-50 text-amber-700",
  red: "bg-red-50 text-red-700",
  purple:
    "bg-purple-50 text-purple-700",
};

export default function StatCard({
  label,
  value,
  description,
  icon,
  tone = "blue",
}: StatCardProps) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-slate-500">
            {label}
          </p>

          <p className="mt-2 text-3xl font-bold text-slate-950">
            {value}
          </p>

          {description && (
            <p className="mt-2 text-xs text-slate-500">
              {description}
            </p>
          )}
        </div>

        {icon && (
          <div
            className={`flex h-11 w-11 items-center justify-center rounded-xl ${tones[tone]}`}
          >
            {icon}
          </div>
        )}
      </div>
    </div>
  );
}