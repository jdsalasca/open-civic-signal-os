import { Notification } from "../types";
import { Card } from "primereact/card";
import { Badge } from "primereact/badge";

type Props = {
  notifications: Notification[];
};

export function NotificationSidebar({ notifications }: Props) {
  return (
    <Card className="shadow-4 border-1 border-gray-800 animate-fade-in" header={
      <div className="p-4 pb-0">
        <h2 className="text-lg font-bold text-gray-100 flex align-items-center gap-2">
          <i className="pi pi-megaphone text-red-500 text-sm"></i>
          RECENT BROADCASTS
        </h2>
        <p className="text-xs text-gray-500 m-0 mt-1 uppercase tracking-widest">Administrative alerts history</p>
      </div>
    }>
      <div className="flex flex-column gap-3 mt-3">
        {notifications.map((n) => (
          <div key={n.id} className="p-3 border-round-xl surface-ground border-1 border-white-alpha-10">
            <div className="flex justify-content-between align-items-center mb-2">
              <Badge value={n.channel} severity={n.channel === 'EMAIL' ? 'info' : 'success'} className="text-xs" />
              <span className="text-xs text-gray-600 font-medium">
                {new Date(n.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
              </span>
            </div>
            <p className="text-sm m-0 text-gray-300 font-medium line-height-3 mb-2 italic">"{n.message.substring(0, 50)}..."</p>
            <div className="flex align-items-center gap-1 text-xs text-gray-500">
              <i className="pi pi-user" style={{ fontSize: '0.7rem' }}></i>
              <span>{n.recipientGroup}</span>
            </div>
          </div>
        ))}
        {notifications.length === 0 && <p className="text-center text-gray-600 text-sm py-4">No broadcasts sent yet.</p>}
      </div>
    </Card>
  );
}
