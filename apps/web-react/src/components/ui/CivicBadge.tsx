import { classNames } from 'primereact/utils';

interface CivicBadgeProps {
  label: string;
  type?: 'status' | 'category' | 'info';
  severity?: 'new' | 'progress' | 'resolved' | 'rejected' | 'neutral';
}

export function CivicBadge({ label, type = 'status', severity = 'neutral' }: CivicBadgeProps) {
  const severityColors = {
    'new': 'bg-status-new text-black',
    'progress': 'bg-status-progress text-black',
    'resolved': 'bg-status-resolved text-black',
    'rejected': 'bg-status-rejected text-white',
    'neutral': 'bg-surface-200 text-text-secondary',
  };

  return (
    <span className={classNames(
      'px-2 py-1 border-round-md text-xs font-black uppercase tracking-tighter inline-flex align-items-center justify-content-center',
      severityColors[severity],
      { 'bg-white-alpha-10 text-text-secondary border-1 border-white-alpha-10': type === 'category' }
    )}>
      {label}
    </span>
  );
}
