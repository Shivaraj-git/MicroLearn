export default function Pager({ page, totalPages, onPage }) {
  if (totalPages <= 1) return null
  return (
    <nav className="pager" aria-label="Pages">
      <button className="btn btn-line" disabled={page <= 0} onClick={() => onPage(page - 1)}>Previous</button>
      <span className="muted">Page {page + 1} of {totalPages}</span>
      <button className="btn btn-line" disabled={page >= totalPages - 1} onClick={() => onPage(page + 1)}>Next</button>
    </nav>
  )
}
