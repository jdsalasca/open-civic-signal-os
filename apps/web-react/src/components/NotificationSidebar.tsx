import { Notification } from "../types";

type Props = {
  notifications: Notification[];
};

export function NotificationSidebar({ notifications }: Props) {
  return (
    <section className="notifications-sidebar">
      <h3>Recent Relays (Admin Only)</h3>
      <div className="notif-list">
        {notifications.length === 0 && <p className="small-note">No recent relays.</p>}
        {notifications.map((n) => (
          <div key={n.id} className="notif-item">
            <div className="notif-header">
              <span className="channel-badge">{n.channel}</span>
              <span className="notif-date">{new Date(n.sentAt).toLocaleTimeString()}</span>
            </div>
            <p className="notif-msg">{n.message.substring(0, 60)}...</p>
            <span className="notif-group">To: {n.recipientGroup}</span>
          </div>
        ))}
      </div>
    </section>
  );
}
