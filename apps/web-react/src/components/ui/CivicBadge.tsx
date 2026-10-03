import { classNames } from 'primereact/utils';

/**
 * Extra DOM attributes are forwarded to the root element so callers can attach a stable
 * `data-testid` without wrapping the badge in another node. AGENTS.md asks for stable test hooks
 * on interactive and asserted elements, and a wrapper div purely to hold an attribute is markup
 * that exists only for the test.
 */
interface CivicBadgeProps extends Omit<React.HTMLAttributes<HTMLSpanElement>, 'children'> {
  label: string;
  type?: 'status' | 'category' | 'info';
  severity?: 'new' | 'progress' | 'resolved' | 'rejected' | 'neutral';
}

export function CivicBadge({ label, type = 'status', severity = 'neutral', className, ...rest }: CivicBadgeProps) {
  const severityColors = {
    'new': 'bg-status-new text-on-brand',
    'progress': 'bg-status-progress text-on-brand',
    'resolved': 'bg-status-resolved text-on-brand',
    'rejected': 'bg-status-rejected text-on-brand',
    'neutral': 'civic-badge-neutral',
  };

  return (
    <span
      {...rest}
      className={classNames(
      'px-3 py-2 border-round-xl text-xs font-bold inline-flex align-items-center justify-content-center civic-badge',
      severityColors[severity],
      { 'civic-badge-neutral': type === 'category' },
      className
    )}>
      {label}
    </span>
  );
}
