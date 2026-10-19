import { Notification } from "../types";
import { formatTime } from "../utils/formatStamp";
import { useTranslation } from "react-i18next";
import { CivicCard } from "./ui/CivicCard";
import { CivicBadge } from "./ui/CivicBadge";

type Props = {
  notifications: Notification[];
};

export function NotificationSidebar({ notifications }: Props) {
  const { t } = useTranslation();

  const channelLabel = (channel: string) =>
    t(`dashboard.alerts_channels.${channel}`, { defaultValue: channel });
  const groupLabel = (group: string) =>
    t(`dashboard.alerts_groups.${group}`, { defaultValue: group });
  const categoryLabel = (category: string) =>
    t(`categories.${category}`, { defaultValue: category });

  const parseAlertMessage = (msg: string) => {
    if (!msg.startsWith("Top unresolved community issues:")) {
      return (
        <p className="text-sm text-secondary m-0 leading-relaxed font-medium">
          {msg}
        </p>
      );
    }
    const issuesPart = msg.replace("Top unresolved community issues:", "").trim();
    const issues = issuesPart.split(",").map((i) => i.trim()).filter(Boolean).slice(0, 3);

    return (
      <div className="flex flex-column gap-2 mt-2">
        <span className="u-eyebrow mb-1 flex align-items-center gap-2">
          <span className="signal-status-chip__dot" style={{ background: "var(--status-rejected)" }} aria-hidden="true"></span>
          {t("dashboard.alerts_top_issues")}
        </span>
        <div className="flex flex-column gap-1">
          {issues.map((issue, idx) => {
            const match = issue.match(/^\[(.*?)\]\s*(.*)$/);
            if (match) {
              const [, category, title] = match;
              return (
                <div key={idx} className="u-surface-note u-surface-note-compact flex align-items-center gap-2 text-sm">
                  <CivicBadge label={categoryLabel(category)} severity="rejected" />
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
        <span className="flex align-items-center gap-2">
          <i className="pi pi-bolt text-status-rejected" aria-hidden="true"></i>
          <span>{t("dashboard.alerts_title")}</span>
        </span>
      )}
      variant="danger"
      data-testid="dashboard-alerts"
    >
      <div className="flex flex-column gap-4">
        {notifications.length === 0 ? (
          <div className="text-center py-4 text-muted italic text-sm">
            {t("dashboard.alerts_empty")}
          </div>
        ) : (
          notifications.slice(0, 3).map((n) => (
            <div key={n.id} className="u-surface-note hover:bg-surface-soft transition-colors" data-testid={`dashboard-alert-${n.id}`}>
              <div className="u-card-split-header mb-3">
                <CivicBadge label={channelLabel(n.channel)} severity="progress" />
                <span className="text-xs text-muted u-meta-value">{formatTime(n.sentAt) ?? "-"}</span>
              </div>
              {parseAlertMessage(n.message)}
              <div className="mt-3 u-eyebrow flex align-items-center gap-2">
                <i className="pi pi-users text-muted" aria-hidden="true"></i>
                {t("dashboard.alerts_target", { group: groupLabel(n.recipientGroup) })}
              </div>
            </div>
          ))
        )}
      </div>
    </CivicCard>
  );
}
