import { Notification } from "../types";
import { CivicCard } from "./ui/CivicCard";
import { CivicBadge } from "./ui/CivicBadge";

type Props = {
  notifications: Notification[];
};

export function NotificationSidebar({ notifications }: Props) {
  const parseAlertMessage = (msg: string) => {
    if (!msg.startsWith('Top unresolved community issues:')) {
      return (
        <p className="text-sm text-secondary m-0 leading-relaxed font-medium">
          {msg}
        </p>
      );
    }
    const issuesPart = msg.replace('Top unresolved community issues:', '').trim();
    const issues = issuesPart.split(',').map(i => i.trim()).filter(Boolean).slice(0, 3);

    return (
      <div className="flex flex-column gap-2 mt-2">
        <span className="u-eyebrow mb-1 flex align-items-center gap-2">
          <span className="w-1rem h-1rem border-circle bg-status-rejected shadow-1" style={{ animation: 'pulse 2s infinite' }}></span>
          Top Unresolved Issues
        </span>
        <div className="flex flex-column gap-1">
          {issues.map((issue, idx) => {
            const match = issue.match(/^\[(.*?)\]\s*(.*)$/);
            if (match) {
              const [, category, title] = match;
              return (
                <div key={idx} className="u-surface-note u-surface-note-compact flex align-items-center gap-2 text-sm">
                  <span className="text-status-rejected text-min uppercase tracking-tighter bg-status-rejected-alpha-10 px-2 py-1 border-round-xl border-1 border-status-rejected-alpha-20">[{category}]</span>
                  <span className="u-list-item-copy text-sm">{title}</span>
                </div>
              );
            }
            return <div key={idx} className="u-list-item-copy text-sm border-bottom-1 border-subtle pb-2">{issue}</div>;
          })}
        </div>
      </div>
    );
  };

  return (
    <CivicCard
      title={(
        <div className="flex align-items-center gap-2">
          <i className="pi pi-bolt text-status-rejected"></i>
          <span>Operational Alerts</span>
        </div>
      )}
      variant="danger"
    >
      <div className="flex flex-column gap-4">
        {notifications.length === 0 ? (
          <div className="text-center py-4 text-muted italic text-sm">
            No system alerts recorded.
          </div>
        ) : (
          notifications.slice(0, 3).map((n) => (
            <div key={n.id} className="u-surface-note hover:bg-surface-soft transition-colors">
              <div className="u-card-split-header mb-3">
                <CivicBadge label={n.channel} severity="progress" />
                <span className="text-xs text-muted u-meta-value">{new Date(n.sentAt).toLocaleTimeString()}</span>
              </div>
              {parseAlertMessage(n.message)}
              <div className="mt-3 u-eyebrow flex align-items-center gap-2">
                <i className="pi pi-users text-muted"></i>
                Target: {n.recipientGroup}
              </div>
            </div>
          ))
        )}
      </div>
    </CivicCard>
  );
}
