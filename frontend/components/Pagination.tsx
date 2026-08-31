interface PaginationProps {
  page: number;
  totalPages: number;
  totalElements: number;
  size: number;
  onPageChange: (
    page: number
  ) => void;
  onSizeChange: (
    size: number
  ) => void;
}

export default function Pagination({
  page,
  totalPages,
  totalElements,
  size,
  onPageChange,
  onSizeChange,
}: PaginationProps) {
  const firstRecord =
    totalElements === 0
      ? 0
      : page * size + 1;

  const lastRecord = Math.min(
    (page + 1) * size,
    totalElements
  );

  return (
    <div className="flex flex-col gap-4 border-t border-slate-200 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
      <p className="text-sm text-slate-600">
        Showing {firstRecord}–
        {lastRecord} of{" "}
        {totalElements}
      </p>

      <div className="flex flex-wrap items-center gap-2">
        <select
          value={size}
          onChange={(event) =>
            onSizeChange(
              Number(
                event.target.value
              )
            )
          }
          className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
        >
          <option value={10}>
            10 rows
          </option>
          <option value={20}>
            20 rows
          </option>
          <option value={50}>
            50 rows
          </option>
        </select>

        <button
          type="button"
          disabled={page === 0}
          onClick={() =>
            onPageChange(page - 1)
          }
          className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-40"
        >
          Previous
        </button>

        <span className="rounded-lg bg-blue-50 px-3 py-2 text-sm font-bold text-blue-700">
          Page {page + 1} of{" "}
          {Math.max(totalPages, 1)}
        </span>

        <button
          type="button"
          disabled={
            page >= totalPages - 1
          }
          onClick={() =>
            onPageChange(page + 1)
          }
          className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-40"
        >
          Next
        </button>
      </div>
    </div>
  );
}