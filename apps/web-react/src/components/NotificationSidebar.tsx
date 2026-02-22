import { Notification } from "../types";
import { CivicCard } from "./ui/CivicCard";
import { CivicBadge } from "./ui/CivicBadge";

type Props = {
  notifications: Notification[];
};

export function NotificationSidebar({ notifications }: Props) {

  return (
    <CivicCard title="Operational Alerts" variant="danger">
      <div className="flex flex-column gap-4">
        {notifications.length === 0 ? (
          <div className="text-center py-4 text-muted italic text-sm">
            No system alerts recorded.
          </div>
        ) : (
          notifications.slice(0, 5).map((n) => (
            <div key={n.id} className="p-4 bg-white-alpha-5 border-round-2xl border-1 border-white-alpha-10 hover:bg-white-alpha-10 transition-colors">
              <div className="flex justify-content-between align-items-center mb-3">
                <CivicBadge label={n.channel} severity="progress" />
                <span className="text-xs font-mono text-muted">{new Date(n.sentAt).toLocaleTimeString()}</span>
              </div>
              <p className="text-sm text-secondary m-0 leading-relaxed font-medium">
                {n.message}
              </p>
              <div className="mt-3 text-min font-black uppercase tracking-tighter text-muted">
                Target: {n.recipientGroup}
              </div>
            </div>
          ))
        )}
      </div>
    </CivicCard>
  );
}
