import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Avatar } from 'primereact/avatar';
import apiClient from '../../api/axios';
import { UserNotification } from '../../types';

/** Personal inbox: unread count in the top bar, latest items in a compact panel. */
export function NotificationBell() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const [items, setItems] = useState<UserNotification[]>([]);
  const [loading, setLoading] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  const loadCount = useCallback(async () => {
    try {
      const { data } = await apiClient.get<{ unread: number }>('notifications/unread-count');
      setUnread(data?.unread ?? 0);
    } catch {
      // A notification count must never break the shell.
    }
  }, []);

  const loadInbox = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await apiClient.get<UserNotification[]>('notifications/inbox');
      setItems(data ?? []);
      setUnread((data ?? []).filter((item) => !item.readAt).length);
    } catch {
      setItems([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadCount();
    const timer = window.setInterval(() => void loadCount(), 60_000);
    return () => window.clearInterval(timer);
  }, [loadCount]);

  useEffect(() => {
    if (!open) {
      return;
    }
    void loadInbox();
    const close = (event: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, [open, loadInbox]);

  const openItem = async (item: UserNotification) => {
    if (!item.readAt) {
      try {
        await apiClient.post(`notifications/${item.id}/read`);
        setUnread((current) => Math.max(0, current - 1));
        setItems((current) =>
          current.map((entry) => (entry.id === item.id ? { ...entry, readAt: new Date().toISOString() } : entry))
        );
      } catch {
        // Navigation still works if the read marker fails.
      }
    }
    setOpen(false);
    if (item.targetType === 'SIGNAL') {
      navigate(`/signal/${item.targetId}`);
    } else if (item.targetType === 'ACTIVITY') {
      navigate('/communities/activities');
    } else if (item.targetType === 'BLOG') {
      navigate('/communities/blog');
    } else {
      navigate('/communities/threads');
    }
  };

  const markAll = async () => {
    try {
      await apiClient.post('notifications/read-all');
      setUnread(0);
      setItems((current) => current.map((item) => ({ ...item, readAt: item.readAt ?? new Date().toISOString() })));
    } catch {
      // Keep the panel usable.
    }
  };

  const messageFor = (item: UserNotification) => {
    const actor = item.actorName || t('notifications.someone');
    const title = item.targetTitle || '';
    if (item.type === 'THREAD_REPLY') {
      return t('notifications.thread_reply', { actor, title });
    }
    if (item.type === 'SIGNAL_SUPPORT') {
      return t('notifications.signal_support', { actor, title });
    }
    if (item.type === 'SIGNAL_STATUS') {
      return t('notifications.signal_status', { title });
    }
    if (item.type === 'SIGNAL_CONFIRMATION') {
      return t('notifications.signal_confirmation', { actor, title });
    }
    if (item.type === 'NEIGHBOURHOOD_SIGNAL') {
      return t('notifications.neighbourhood_signal', { title });
    }
    if (item.type === 'NEIGHBOURHOOD_EVENT') {
      return t('notifications.neighbourhood_event', { title });
    }
    if (item.type === 'ACTIVITY_REMINDER') {
      return t('notifications.activity_reminder', { title });
    }
    return t('notifications.fallback', { title });
  };

  const timeAgo = (value: string) => {
    const timestamp = new Date(value).getTime();
    const minutes = Math.max(0, Math.floor((Date.now() - timestamp) / 60000));
    if (minutes < 1) return t('notifications.just_now');
    if (minutes < 60) return t('notifications.minutes', { count: minutes });
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return t('notifications.hours', { count: hours });
    return new Intl.RelativeTimeFormat(i18n.language, { numeric: 'auto' }).format(
      -Math.floor(hours / 24),
      'day'
    );
  };

  return (
    <div className="notification-bell" ref={rootRef}>
      <button
        type="button"
        className="notification-bell-trigger"
        aria-label={t('notifications.title')}
        aria-haspopup="dialog"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
        data-testid="notification-bell"
      >
        <i className="pi pi-bell" aria-hidden="true" />
        {unread > 0 && (
          <span className="notification-badge" data-testid="notification-badge">
            {unread > 9 ? '9+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div className="notification-panel" role="dialog" aria-label={t('notifications.title')}>
          <div className="notification-panel-head">
            <strong>{t('notifications.title')}</strong>
            {unread > 0 && (
              <button type="button" onClick={markAll} data-testid="notification-mark-all">
                {t('notifications.mark_all')}
              </button>
            )}
          </div>

          {loading ? (
            <div className="notification-empty">{t('notifications.loading')}</div>
          ) : items.length === 0 ? (
            <div className="notification-empty" data-testid="notification-empty">
              {t('notifications.empty')}
            </div>
          ) : (
            <ul className="notification-list">
              {items.slice(0, 8).map((item) => (
                <li key={item.id}>
                  <button
                    type="button"
                    className={item.readAt ? 'notification-item' : 'notification-item is-unread'}
                    onClick={() => openItem(item)}
                    data-testid={`notification-item-${item.id}`}
                  >
                    <Avatar
                      label={(item.actorName?.[0] ?? '?').toUpperCase()}
                      shape="circle"
                      className="bg-brand-primary text-on-brand font-bold"
                    />
                    <span className="notification-copy">
                      <span>{messageFor(item)}</span>
                      <small>{timeAgo(item.createdAt)}</small>
                    </span>
                    {!item.readAt && <span className="notification-dot" aria-hidden="true" />}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
