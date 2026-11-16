import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { useTranslation } from 'react-i18next';
import { Avatar } from 'primereact/avatar';
import { InputText } from 'primereact/inputtext';
import { InputTextarea } from 'primereact/inputtextarea';
import { CommunityFeedItem } from '../types';
import { Layout } from '../components/Layout';
import { useCommunityStore } from '../store/useCommunityStore';
import { useAuthStore } from '../store/useAuthStore';
import apiClient from '../api/axios';
import { CivicCard } from '../components/ui/CivicCard';
import { CivicEmptyState } from '../components/ui/CivicEmptyState';
import { CivicPageHeader } from '../components/ui/CivicPageHeader';
import { CivicButton } from '../components/ui/CivicButton';
import { toRoleLabel } from '../constants/roleLabels';

type ApiError = Error & { friendlyMessage?: string };
type FeedFilter = 'all' | CommunityFeedItem['type'];

const TITLE_LIMIT = 120;
const BODY_LIMIT = 2000;

/**
 * Community feed, in the shape people already know from social products: one composer, one
 * reverse-chronological timeline, and the action that matters on each card. It reads the existing
 * `/api/community/feed` endpoint, which already mixed reports, announcements and conversations but
 * had no screen using it.
 */
export function CommunityFeed() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { activeCommunityId, memberships } = useCommunityStore();
  const userName = useAuthStore((state) => state.userName);

  const [feed, setFeed] = useState<CommunityFeedItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<FeedFilter>('all');
  const [composerOpen, setComposerOpen] = useState(false);
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [publishing, setPublishing] = useState(false);
  const [supported, setSupported] = useState<Set<string>>(() => new Set());
  const [replyingTo, setReplyingTo] = useState<string | null>(null);
  const [replyText, setReplyText] = useState('');
  const [replySending, setReplySending] = useState(false);
  const [alertsOn, setAlertsOn] = useState(false);

  const activeCommunityName = memberships.find((m) => m.communityId === activeCommunityId)?.communityName;
  const activeRole = memberships.find((m) => m.communityId === activeCommunityId)?.role;

  const loadFeed = useCallback(async () => {
    if (!activeCommunityId) {
      setFeed([]);
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      const res = await apiClient.get<CommunityFeedItem[]>(
        `community/feed?communityId=${activeCommunityId}&days=30`
      );
      setFeed(res.data ?? []);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('community_feed.load_error'));
    } finally {
      setLoading(false);
    }
  }, [activeCommunityId, t]);

  useEffect(() => {
    void loadFeed();
  }, [loadFeed]);

  useEffect(() => {
    if (!activeCommunityId) {
      setAlertsOn(false);
      return;
    }
    apiClient
      .get<{ subscribed: boolean }>(`communities/${activeCommunityId}/alerts`)
      .then(({ data }) => setAlertsOn(Boolean(data.subscribed)))
      .catch(() => setAlertsOn(false));
  }, [activeCommunityId]);

  const toggleAlerts = async () => {
    if (!activeCommunityId) return;
    try {
      const next = !alertsOn;
      await apiClient.request({
        url: `communities/${activeCommunityId}/alerts`,
        method: next ? 'post' : 'delete'
      });
      setAlertsOn(next);
      toast.success(next ? t('community_feed.alerts_on_success') : t('community_feed.alerts_off_success'));
    } catch {
      toast.error(t('community_feed.alerts_error'));
    }
  };

  const visibleItems = useMemo(
    () => (filter === 'all' ? feed : feed.filter((item) => item.type === filter)),
    [feed, filter]
  );

  const newest = feed[0];

  const openItem = (item: CommunityFeedItem) => {
    if (item.type === 'signal') {
      navigate(`/signal/${item.id}`);
    } else if (item.type === 'blog') {
      navigate('/communities/blog');
    } else {
      navigate('/communities/threads');
    }
  };

  const support = async (item: CommunityFeedItem) => {
    try {
      await apiClient.post(`signals/${item.id}/vote`);
      setSupported((current) => new Set(current).add(item.id));
      toast.success(t('community_feed.support_success'));
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('community_feed.support_error'));
    }
  };

  const followType = (item: CommunityFeedItem) => {
    if (item.type === 'signal') return 'SIGNAL';
    if (item.type === 'blog') return 'BLOG';
    return 'THREAD';
  };

  const toggleFollow = async (item: CommunityFeedItem) => {
    const next = !item.following;
    try {
      await apiClient.request({
        url: `follows/${followType(item)}/${item.id}`,
        method: next ? 'post' : 'delete'
      });
      setFeed((current) =>
        current.map((entry) => (entry.id === item.id ? { ...entry, following: next } : entry))
      );
      toast.success(next ? t('community_feed.follow_success') : t('community_feed.unfollow_success'));
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('community_feed.follow_error'));
    }
  };

  const confirmSignal = async (item: CommunityFeedItem) => {
    try {
      const { data } = await apiClient.request<{ count: number; confirmedByViewer: boolean }>({
        url: `signals/${item.id}/confirmations`,
        method: item.viewerConfirmed ? 'delete' : 'post'
      });
      setFeed((current) =>
        current.map((entry) =>
          entry.id === item.id
            ? { ...entry, confirmationCount: data.count, viewerConfirmed: data.confirmedByViewer }
            : entry
        )
      );
      toast.success(data.confirmedByViewer ? t('community_feed.confirmed') : t('community_feed.unconfirmed'));
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('community_feed.confirm_error'));
    }
  };

  const sendReply = async (item: CommunityFeedItem) => {
    if (!activeCommunityId || !replyText.trim()) {
      return;
    }
    setReplySending(true);
    try {
      if (item.type === 'thread-update') {
        await apiClient.post(`community/threads/${item.id}/messages`, {
          sourceCommunityId: activeCommunityId,
          content: replyText.trim()
        });
      } else {
        await apiClient.post(`community/blog/${item.id}/comments`, {
          content: replyText.trim()
        });
      }
      setReplyingTo(null);
      setReplyText('');
      toast.success(t('community_feed.reply_success'));
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('community_feed.reply_error'));
    } finally {
      setReplySending(false);
    }
  };

  const publish = async () => {
    if (!activeCommunityId || !title.trim()) {
      return;
    }
    setPublishing(true);
    try {
      const thread = await apiClient.post<{ id: string }>('community/threads', {
        sourceCommunityId: activeCommunityId,
        targetCommunityId: activeCommunityId,
        title: title.trim()
      });
      if (body.trim()) {
        await apiClient.post(`community/threads/${thread.data.id}/messages`, {
          sourceCommunityId: activeCommunityId,
          content: body.trim()
        });
      }
      setTitle('');
      setBody('');
      setComposerOpen(false);
      await loadFeed();
      toast.success(t('community_feed.published'));
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('community_feed.publish_error'));
    } finally {
      setPublishing(false);
    }
  };

  const filterOptions: Array<{ key: FeedFilter; label: string; icon: string }> = [
    { key: 'all', label: t('community_feed.filter_all'), icon: 'pi pi-list' },
    { key: 'signal', label: t('community_feed.filter_signal'), icon: 'pi pi-bolt' },
    { key: 'blog', label: t('community_feed.filter_blog'), icon: 'pi pi-megaphone' },
    { key: 'thread-update', label: t('community_feed.filter_thread'), icon: 'pi pi-comments' }
  ];

  const itemLabel = (type: CommunityFeedItem['type']) => {
    if (type === 'signal') return t('community_feed.type_signal');
    if (type === 'blog') return t('community_feed.type_blog');
    return t('community_feed.type_thread');
  };

  const itemIcon = (type: CommunityFeedItem['type']) => {
    if (type === 'signal') return 'pi pi-bolt';
    if (type === 'blog') return 'pi pi-megaphone';
    return 'pi pi-comments';
  };

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <CivicPageHeader
          title={t('community_feed.title', {
            community: activeCommunityName || t('dashboard.community_default')
          })}
          description={t('community_feed.desc')}
          actions={
            <CivicButton
              type="button"
              icon="pi pi-plus"
              label={t('community_feed.new_post')}
              onClick={() => setComposerOpen(true)}
              data-testid="community-feed-new-post"
            />
          }
        />

        {!activeCommunityId ? (
          <CivicCard>
            <CivicEmptyState
              icon="pi-map-marker"
              title={t('community_home.no_context_title')}
              description={t('community_home.no_context_desc')}
              actionLabel={t('nav.communities')}
              onAction={() => navigate('/communities')}
            />
          </CivicCard>
        ) : (
          <div className="social-feed-grid">
            <div className="social-feed-main">
              <CivicCard className="social-composer-card" padding="md" data-testid="community-feed-composer">
                {!composerOpen ? (
                  <button
                    type="button"
                    className="social-composer-trigger"
                    onClick={() => setComposerOpen(true)}
                  >
                    <Avatar
                      label={userName?.[0]?.toUpperCase() ?? '?'}
                      shape="circle"
                      className="bg-brand-primary text-on-brand font-bold"
                    />
                    <span className="social-composer-placeholder">
                      {t('community_feed.composer_placeholder', { community: activeCommunityName })}
                    </span>
                    <span className="social-composer-plus" aria-hidden="true">
                      <i className="pi pi-plus" />
                    </span>
                  </button>
                ) : (
                  <div className="social-composer-form">
                    <div className="flex align-items-center gap-3 mb-3">
                      <Avatar
                        label={userName?.[0]?.toUpperCase() ?? '?'}
                        shape="circle"
                        className="bg-brand-primary text-on-brand font-bold"
                      />
                      <div className="flex flex-column">
                        <strong className="text-main">{userName}</strong>
                        <span className="text-xs text-muted">
                          {t('community_feed.composer_audience', { community: activeCommunityName })}
                        </span>
                      </div>
                    </div>

                    <label className="social-composer-label" htmlFor="feed-post-title">
                      {t('community_feed.composer_title')}
                    </label>
                    <InputText
                      id="feed-post-title"
                      value={title}
                      maxLength={TITLE_LIMIT}
                      onChange={(event) => setTitle(event.target.value)}
                      placeholder={t('community_feed.composer_title_placeholder')}
                      className="w-full"
                    />
                    <span className="social-composer-count">
                      {title.length}/{TITLE_LIMIT}
                    </span>

                    <label className="social-composer-label mt-3" htmlFor="feed-post-body">
                      {t('community_feed.composer_body')}
                    </label>
                    <InputTextarea
                      id="feed-post-body"
                      value={body}
                      rows={4}
                      maxLength={BODY_LIMIT}
                      onChange={(event) => setBody(event.target.value)}
                      placeholder={t('community_feed.composer_body_placeholder')}
                      className="w-full"
                    />
                    <span className="social-composer-count">
                      {body.length}/{BODY_LIMIT}
                    </span>

                    <div className="social-composer-actions">
                      <CivicButton
                        type="button"
                        variant="ghost"
                        label={t('community_feed.composer_cancel')}
                        onClick={() => setComposerOpen(false)}
                      />
                      <CivicButton
                        type="button"
                        label={t('community_feed.composer_publish')}
                        icon="pi pi-send"
                        loading={publishing}
                        disabled={!title.trim() || publishing}
                        onClick={publish}
                        data-testid="community-feed-publish"
                      />
                    </div>
                  </div>
                )}
              </CivicCard>

              <div className="social-feed-toolbar">
                <div className="social-feed-filters">
                  {filterOptions.map((option) => (
                    <button
                      key={option.key}
                      type="button"
                      className={`u-filter-chip ${filter === option.key ? 'is-active' : ''}`}
                      onClick={() => setFilter(option.key)}
                      data-testid={`community-feed-filter-${option.key}`}
                    >
                      <i className={option.icon} aria-hidden="true" />
                      {option.label}
                    </button>
                  ))}
                </div>
                {newest && (
                  <span className="social-feed-freshness">
                    <i className="pi pi-clock" aria-hidden="true" />
                    {newest.freshness}
                  </span>
                )}
              </div>

              {loading ? (
                <div className="social-feed-skeleton" aria-hidden="true">
                  {[0, 1, 2].map((row) => (
                    <div key={row} className="social-feed-skeleton-row" />
                  ))}
                </div>
              ) : visibleItems.length === 0 ? (
                <CivicCard data-testid="community-feed-empty">
                  <CivicEmptyState
                    icon="pi-comments"
                    title={t('community_feed.empty_title')}
                    description={t('community_feed.empty_desc')}
                    actionLabel={t('community_feed.empty_action')}
                    onAction={() => setComposerOpen(true)}
                  />
                </CivicCard>
              ) : (
                <div className="social-feed-list" data-testid="community-feed-timeline">
                  {visibleItems.map((item) => (
                    <article
                      key={`${item.type}-${item.id}`}
                      className="social-feed-item"
                      data-testid={`community-feed-item-${item.type}`}
                    >
                      <div className="social-feed-icon" aria-hidden="true">
                        <i className={itemIcon(item.type)} />
                      </div>
                      <div className="social-feed-copy">
                        <div className="social-feed-meta">
                          <span className="social-feed-type">{itemLabel(item.type)}</span>
                          <span className="social-feed-time">{item.freshness}</span>
                        </div>
                        <h3 className="social-feed-title">{item.title}</h3>
                        {item.summary && <p className="social-feed-summary">{item.summary}</p>}
                        <div className="social-feed-actions">
                          <button type="button" onClick={() => openItem(item)}>
                            <i className="pi pi-arrow-right" aria-hidden="true" />
                            {t('community_feed.open')}
                          </button>
                          {item.type === 'signal' && (
                            <button
                              type="button"
                              onClick={() => support(item)}
                              disabled={supported.has(item.id)}
                            >
                              <i className="pi pi-heart" aria-hidden="true" />
                              {supported.has(item.id)
                                ? t('community_feed.supported')
                                : t('community_feed.support')}
                            </button>
                          )}
                          {item.type === 'signal' && (
                            <button
                              type="button"
                              onClick={() => confirmSignal(item)}
                              aria-pressed={Boolean(item.viewerConfirmed)}
                            >
                              <i className="pi pi-eye" aria-hidden="true" />
                              {item.viewerConfirmed
                                ? t('community_feed.confirmed')
                                : t('community_feed.confirm')}
                              {typeof item.confirmationCount === 'number' && item.confirmationCount > 0
                                ? ` · ${item.confirmationCount}`
                                : ''}
                            </button>
                          )}
                          {(item.type === 'thread-update' || item.type === 'blog') && (
                            <button
                              type="button"
                              onClick={() => {
                                setReplyingTo(replyingTo === item.id ? null : item.id);
                                setReplyText('');
                              }}
                              aria-expanded={replyingTo === item.id}
                            >
                              <i className="pi pi-comment" aria-hidden="true" />
                              {t('community_feed.reply')}
                            </button>
                          )}
                          <button
                            type="button"
                            onClick={() => toggleFollow(item)}
                            aria-pressed={Boolean(item.following)}
                            data-testid={`community-feed-follow-${item.id}`}
                          >
                            <i
                              className={item.following ? 'pi pi-check' : 'pi pi-bell'}
                              aria-hidden="true"
                            />
                            {item.following
                              ? t('community_feed.following')
                              : t('community_feed.follow')}
                          </button>
                        </div>

                        {replyingTo === item.id && (
                          <div className="social-feed-reply">
                            <InputTextarea
                              value={replyText}
                              rows={2}
                              maxLength={1000}
                              autoResize
                              onChange={(event) => setReplyText(event.target.value)}
                              placeholder={t('community_feed.reply_placeholder')}
                              className="w-full"
                              data-testid={`community-feed-reply-input-${item.id}`}
                            />
                            <div className="social-feed-reply-actions">
                              <CivicButton
                                type="button"
                                variant="ghost"
                                label={t('community_feed.reply_cancel')}
                                onClick={() => setReplyingTo(null)}
                              />
                              <CivicButton
                                type="button"
                                label={t('community_feed.reply_send')}
                                icon="pi pi-send"
                                loading={replySending}
                                disabled={!replyText.trim() || replySending}
                                onClick={() => sendReply(item)}
                                data-testid={`community-feed-reply-send-${item.id}`}
                              />
                            </div>
                          </div>
                        )}
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </div>

            <aside className="social-feed-aside">
              <CivicCard title={t('community_feed.your_community')} variant="brand">
                <div className="flex flex-column gap-2">
                  <strong className="text-xl text-main">{activeCommunityName}</strong>
                  {activeRole && (
                    <span className="u-pill align-self-start">
                      <i className="pi pi-users text-brand-primary" aria-hidden="true" />
                      {toRoleLabel(activeRole, t)}
                    </span>
                  )}
                  <span className="text-sm text-secondary mt-2">
                    {t('community_feed.your_community_desc')}
                  </span>
                </div>
                <div className="social-feed-aside-links mt-4">
                  <button type="button" onClick={() => navigate('/communities/map')}>
                    <i className="pi pi-map" aria-hidden="true" />
                    {t('community_feed.explore_map')}
                  </button>
                  <button type="button" onClick={() => navigate('/communities/proposals')}>
                    <i className="pi pi-file-edit" aria-hidden="true" />
                    {t('community_feed.explore_proposals')}
                  </button>
                  <button type="button" onClick={() => navigate('/communities/projects')}>
                    <i className="pi pi-briefcase" aria-hidden="true" />
                    {t('community_feed.explore_projects')}
                  </button>
                  <button type="button" onClick={() => navigate('/communities/governance')}>
                    <i className="pi pi-book" aria-hidden="true" />
                    {t('community_feed.explore_governance')}
                  </button>
                </div>
              </CivicCard>

              <CivicCard title={t('community_feed.alerts_title')} className="mt-4">
                <p className="text-sm text-secondary mt-2 mb-3">{t('community_feed.alerts_desc')}</p>
                <CivicButton
                  type="button"
                  icon={alertsOn ? 'pi pi-check' : 'pi pi-bell'}
                  variant={alertsOn ? 'secondary' : 'primary'}
                  label={alertsOn ? t('community_feed.alerts_on') : t('community_feed.alerts_off')}
                  onClick={toggleAlerts}
                  className="w-full"
                  data-testid="community-alerts-toggle"
                />
              </CivicCard>
            </aside>
          </div>
        )}
      </div>
    </Layout>
  );
}
