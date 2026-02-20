import { Notification } from "../types";
import { Card } from "primereact/card";
import { Tag } from "primereact/tag";
import { useTranslation } from "react-i18next";

type Props = {
  notifications: Notification[];
};

export function NotificationSidebar({ notifications }: Props) {
  const { t } = useTranslation();

  return (
    <Card title={t('dashboard.system_alerts_title')} className="shadow-4 border-1 border-white-alpha-10 bg-surface">
      <div className="flex flex-column gap-3">
        {notifications.length === 0 ? (
          <p className="text-muted text-sm italic">{t('dashboard.empty_title')}</p>
        ) : (
          notifications.map((n) => (
            <div key={n.id} className="p-3 bg-white-alpha-5 border-round-lg border-1 border-white-alpha-10">
              <div className="flex justify-content-between align-items-center mb-2">
                <Tag value={n.channel} severity="info" pt={{ root: { style: { fontSize: '9px', fontWeight: 'bold' } } }} />
                <span className="text-xs text-muted font-mono">{new Date(n.sentAt).toLocaleDateString()}</span>
              </div>
              <p className="text-sm text-main m-0 line-height-3">{n.message}</p>
            </div>
          ))
        )}
      </div>
    </Card>
  );
}
