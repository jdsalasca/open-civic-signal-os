import { classNames } from 'primereact/utils';

interface CivicBadgeProps {
  label: string;
  type?: 'status' | 'category' | 'info';
  severity?: 'new' | 'progress' | 'resolved' | 'rejected' | 'neutral';
}

export function CivicBadge({ label, type = 'status', severity = 'neutral' }: CivicBadgeProps) {
  const severityColors = {
    'new': 'bg-status-new text-on-brand',
    'progress': 'bg-status-progress text-on-brand',
    'resolved': 'bg-status-resolved text-on-brand',
    'rejected': 'bg-status-rejected text-on-brand',
    'neutral': 'bg-white-alpha-10 text-secondary border-1 border-white-alpha-10',
  };

  return (
    <span className={classNames(
      'px-3 py-2 border-round-xl text-xs font-bold inline-flex align-items-center justify-content-center civic-badge',
      severityColors[severity],
      { 'bg-white-alpha-10 text-secondary border-1 border-white-alpha-10': type === 'category' }
    )}>
      {label}
    </span>
  );
}
