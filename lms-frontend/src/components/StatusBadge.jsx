const LABELS = {
  PENDING: ['Processing', 'wait'],
  ACTIVE: ['Active', 'ok'],
  COMPLETED: ['Completed', 'ok'],
  DISABLED: ['Disabled', 'bad'],
  DRAFT: ['Draft', 'wait'],
  PUBLISHED: ['Published', 'ok'],
  SUSPENDED: ['Suspended', 'bad'],
  CONFIRMED: ['Enrolled', 'ok'],
  REJECTED: ['Payment declined', 'bad'],
  CANCELLED: ['Refunded', 'bad'],
}

export default function StatusBadge({ status }) {
  const [label, tone] = LABELS[status] || [status, 'wait']
  return <span className={`badge badge-${tone}`}>{label}</span>
}
