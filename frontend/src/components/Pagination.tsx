import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';

export const PAGE_SIZE = 5;

/** Page from the `page` query parameter, so the page is part of the URL and survives a reload. */
export function usePage<T>(items: T[], pageSize = PAGE_SIZE) {
  const [params] = useSearchParams();
  const pages = Math.max(1, Math.ceil(items.length / pageSize));
  const page = Math.min(pages, Math.max(1, Number(params.get('page')) || 1));
  return { page, pages, rows: items.slice((page - 1) * pageSize, page * pageSize) };
}

export function Pagination({
  page,
  pages,
  total,
  noun,
}: {
  page: number;
  pages: number;
  total: number;
  noun: string;
}) {
  const [params] = useSearchParams();
  if (pages <= 1) return null;

  function hrefFor(n: number) {
    const next = new URLSearchParams(params);
    if (n === 1) next.delete('page');
    else next.set('page', String(n));
    return `?${next.toString()}`;
  }

  return (
    <nav className="pagination" aria-label={`Pages of ${noun}`}>
      {page > 1 ? (
        <Link className="btn btn-sm btn-secondary" to={hrefFor(page - 1)} replace>
          <ChevronLeft aria-hidden="true" /> Previous
        </Link>
      ) : (
        <span className="btn btn-sm btn-secondary" aria-disabled="true">
          <ChevronLeft aria-hidden="true" /> Previous
        </span>
      )}
      <span className="small muted num">
        Page {page} of {pages} · {total} {noun}
      </span>
      {page < pages ? (
        <Link className="btn btn-sm btn-secondary" to={hrefFor(page + 1)} replace>
          Next <ChevronRight aria-hidden="true" />
        </Link>
      ) : (
        <span className="btn btn-sm btn-secondary" aria-disabled="true">
          Next <ChevronRight aria-hidden="true" />
        </span>
      )}
    </nav>
  );
}
