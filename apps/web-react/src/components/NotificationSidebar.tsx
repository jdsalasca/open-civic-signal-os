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
        <span className="text-xs font-bold text-main uppercase tracking-widest mb-1 flex align-items-center gap-2">
          <span className="w-1rem h-1rem border-circle bg-status-rejected shadow-1" style={{ animation: 'pulse 2s infinite' }}></span>
          Top Unresolved Issues
        </span>
        <div className="flex flex-column gap-1">
          {issues.map((issue, idx) => {
            const match = issue.match(/^\[(.*?)\]\s*(.*)$/);
            if (match) {
              const [, category, title] = match;
              return (
                <div key={idx} className="flex align-items-center gap-2 text-sm p-2 bg-surface border-round-lg border-1 border-subtle">
                  <span className="text-status-rejected font-mono text-min uppercase tracking-tighter bg-status-rejected-alpha-10 px-1 border-round">[{category}]</span>
                  <span className="text-main font-medium line-height-3 text-sm">{title}</span>
                </div>
              );
            }
            return <div key={idx} className="text-sm text-secondary border-bottom-1 border-subtle pb-2">{issue}</div>;
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
            <div key={n.id} className="p-4 bg-white-alpha-5 border-round-2xl border-1 border-white-alpha-10 hover:bg-white-alpha-10 transition-colors">
              <div className="flex justify-content-between align-items-center mb-3">
                <CivicBadge label={n.channel} severity="progress" />
                <span className="text-xs font-mono text-muted">{new Date(n.sentAt).toLocaleTimeString()}</span>
              </div>
              {parseAlertMessage(n.message)}
              <div className="mt-3 text-min font-black uppercase tracking-tighter text-muted flex align-items-center gap-2">
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
