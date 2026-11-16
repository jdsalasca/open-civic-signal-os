import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { useTranslation } from 'react-i18next';
import { Avatar } from 'primereact/avatar';
import { InputText } from 'primereact/inputtext';
import { InputTextarea } from 'primereact/inputtextarea';
import { CommunityFeedItem, CommunityThread } from '../types';
import { Layout } from '../components/Layout';
import { useCommunityStore } from '../store/useCommunityStore';
import { useAuthStore } from '../store/useAuthStore';
import { useSettingsStore } from '../store/useSettingsStore';
import apiClient from '../api/axios';
import { CivicCard } from '../components/ui/CivicCard';
import { CivicEmptyState } from '../components/ui/CivicEmptyState';
import { CivicButton } from '../components/ui/CivicButton';
import { LinkifiedText } from '../components/ui/LinkifiedText';
import { OnboardingChecklist } from '../components/OnboardingChecklist';
import { toRoleLabel } from '../constants/roleLabels';
import { formatRelativeTime } from '../utils/formatRelativeTime';

type ApiError = Error & { friendlyMessage?: string };
type FeedFilter = 'all' | CommunityFeedItem['type'];
type FeedComment = { id: string; author: string; content: string; createdAt?: string };
type CommentResponse = { id: string; authorUsername: string; content: string; createdAt: string };
type ThreadMessageResponse = { id: string; content: string; createdAt: string; hidden?: boolean; authorName?: string | null };

const TITLE_LIMIT = 120;
const BODY_LIMIT = 2000;

const commentKey = (item: CommunityFeedItem) => `${item.type}:${item.id}`;

const reactionTotal = (item: CommunityFeedItem) =>
  Object.values(item.reactions ?? {}).reduce((sum, value) => sum + value, 0);

const itemHref = (item: CommunityFeedItem) => {
  if (item.type === 'signal') return `/signal/${item.id}`;
  if (item.type === 'blog') return `/communities/blog#blog-post-${item.id}`;
  return `/communities/threads#thread-${item.id}`;
};

/**
 * Community feed, in the shape people already know from social products: one composer, one
 * reverse-chronological timeline, and the action that matters on each card. It reads the existing
 * `/api/community/feed` endpoint, which already mixed reports, announcements and conversations but
 * had no screen using it.
 */
export function CommunityFeed() {
  const navigate = useNavigate();
  const { t, i18n } = useTranslation();
  const { activeCommunityId, memberships } = useCommunityStore();
  const userName = useAuthStore((state) => state.userName);

  const [feed, setFeed] = useState<CommunityFeedItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<FeedFilter>('all');
  const [composerOpen, setComposerOpen] = useState(false);
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [publishing, setPublishing] = useState(false);
  const [replyingTo, setReplyingTo] = useState<string | null>(null);
  const [replyText, setReplyText] = useState('');
  const [replySending, setReplySending] = useState(false);
  const [comments, setComments] = useState<Record<string, FeedComment[]>>({});
  const [commentsLoading, setCommentsLoading] = useState<string | null>(null);
  const [commentsError, setCommentsError] = useState<string | null>(null);
  const [loadError, setLoadError] = useState(false);
  const [alertsOn, setAlertsOn] = useState(false);

  const activeCommunityName = memberships.find((m) => m.communityId === activeCommunityId)?.communityName;
  const activeRole = memberships.find((m) => m.communityId === activeCommunityId)?.role;
  const interfaceMode = useSettingsStore((state) => state.interfaceMode);

  const loadFeed = useCallback(async (signal?: AbortSignal) => {
    if (!activeCommunityId) {
      setFeed([]);
      setLoading(false);
      setLoadError(false);
      return;
    }
    try {
      setLoading(true);
      setLoadError(false);
      const res = await apiClient.get<CommunityFeedItem[]>(
        `community/feed?communityId=${activeCommunityId}&days=30`,
        { signal }
      );
      setFeed(res.data ?? []);
    } catch (err) {
      const apiErr = err as ApiError & { name?: string };
      if (apiErr.name === 'CanceledError' || apiErr.name === 'AbortError') return;
      setLoadError(true);
      toast.error(apiErr.friendlyMessage || t('community_feed.load_error'));
    } finally {
      setLoading(false);
    }
  }, [activeCommunityId, t]);

  useEffect(() => {
    const controller = new AbortController();
    void loadFeed(controller.signal);
    return () => controller.abort();
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

  const trending = useMemo(
    () =>
      [...feed]
        .filter((item) => reactionTotal(item) > 0)
        .sort((a, b) => reactionTotal(b) - reactionTotal(a))
        .slice(0, 3),
    [feed]
  );

  const loadComments = async (item: CommunityFeedItem) => {
    const key = commentKey(item);
    setCommentsLoading(key);
    setCommentsError(null);
    try {
      let next: FeedComment[] = [];
      if (item.type === 'signal') {
        const { data } = await apiClient.get<CommentResponse[]>(`signals/${item.id}/comments`);
        next = data.map((comment) => ({
          id: comment.id,
          author: comment.authorUsername,
          content: comment.content,
          createdAt: comment.createdAt,
        }));
      } else if (item.type === 'blog') {
        const { data } = await apiClient.get<CommentResponse[]>(`community/blog/${item.id}/comments`);
        next = data.map((comment) => ({
          id: comment.id,
          author: comment.authorUsername,
          content: comment.content,
          createdAt: comment.createdAt,
        }));
      } else if (activeCommunityId) {
        const { data } = await apiClient.get<CommunityThread>(
          `community/threads/${item.id}?communityId=${activeCommunityId}`
        );
        // The first message is the post body already shown above, so only replies are listed here.
        const rootId = data.messages.find((message) => !message.parentMessageId)?.id;
        next = data.messages
          .filter((message) => !message.hidden && message.id !== rootId)
          .map((message) => ({
            id: message.id,
            author: message.authorName ?? t('community_feed.someone'),
            content: message.content,
            createdAt: message.createdAt,
          }));
      }
      setComments((current) => ({ ...current, [key]: next }));
    } catch {
      setCommentsError(key);
    } finally {
      setCommentsLoading(null);
    }
  };

  const toggleComments = (item: CommunityFeedItem) => {
    const key = commentKey(item);
    if (replyingTo === key) {
      setReplyingTo(null);
      return;
    }
    setReplyingTo(key);
    setReplyText('');
    if (!comments[key] && commentsLoading !== key) {
      void loadComments(item);
    }
  };

  const react = async (item: CommunityFeedItem, reaction: 'LIKE' | 'CELEBRATE') => {
    if (item.type !== 'signal' && item.type !== 'blog') return;
    try {
      const endpoint = item.type === 'signal'
        ? `signals/${item.id}/react`
        : `community/blog/${item.id}/react`;
      const { data } = await apiClient.post<{ reactions: Record<string, number>; viewerReaction: string | null }>(
        endpoint,
        { type: reaction }
      );
      setFeed((current) =>
        current.map((entry) =>
          entry.id === item.id && entry.type === item.type
            ? { ...entry, reactions: data.reactions, viewerReaction: data.viewerReaction }
            : entry
        )
      );
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('community_feed.reaction_error'));
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
        current.map((entry) =>
          entry.id === item.id && entry.type === item.type ? { ...entry, following: next } : entry
        )
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
          entry.id === item.id && entry.type === item.type
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
    const content = replyText.trim();
    if (!activeCommunityId || !content) {
      return;
    }
    const key = commentKey(item);
    setReplySending(true);
    try {
      let created: FeedComment;
      if (item.type === 'signal') {
        const { data } = await apiClient.post<CommentResponse>(`signals/${item.id}/comments`, { content });
        created = { id: data.id, author: data.authorUsername, content: data.content, createdAt: data.createdAt };
      } else if (item.type === 'blog') {
        const { data } = await apiClient.post<CommentResponse>(`community/blog/${item.id}/comments`, { content });
        created = { id: data.id, author: data.authorUsername, content: data.content, createdAt: data.createdAt };
      } else {
        const { data } = await apiClient.post<ThreadMessageResponse>(`community/threads/${item.id}/messages`, {
          sourceCommunityId: activeCommunityId,
          content
        });
        created = {
          id: data.id,
          author: data.authorName ?? userName ?? t('community_feed.someone'),
          content,
          createdAt: data.createdAt
        };
      }
      setComments((current) => ({ ...current, [key]: [...(current[key] ?? []), created] }));
      if (item.type !== 'thread-update') {
        setFeed((current) =>
          current.map((entry) =>
            entry.id === item.id && entry.type === item.type
              ? { ...entry, commentCount: (entry.commentCount ?? 0) + 1 }
              : entry
          )
        );
      }
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
        <header className="social-feed-page-head">
          <div className="min-w-0">
            <h1 className="social-feed-page-title">
              {t('community_feed.title', {
                community: activeCommunityName || t('dashboard.community_default')
              })}
            </h1>
          </div>
          <CivicButton
            type="button"
            icon="pi pi-plus-circle"
            label={t('community_feed.report_action')}
            onClick={() => navigate('/report')}
            data-testid="community-feed-report"
          />
        </header>

        <OnboardingChecklist />

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

                    <p className="social-composer-guidance">
                      {t('community_feed.guidance')}{' '}
                      <Link to="/help" className="civic-inline-link">
                        {t('community_feed.norms')}
                      </Link>
                    </p>

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
                      aria-pressed={filter === option.key}
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
                    {t('community_feed.updated', { time: formatRelativeTime(newest.happenedAt, i18n.language) })}
                  </span>
                )}
              </div>

              {loadError ? (
                <CivicCard data-testid="community-feed-error">
                  <div className="signal-feed-error" role="alert">
                    <i className="pi pi-exclamation-circle" aria-hidden="true" />
                    <div className="signal-feed-error__copy">
                      <strong>{t('community_feed.load_error')}</strong>
                      <span>{t('community_feed.load_error_hint')}</span>
                    </div>
                    <CivicButton
                      type="button"
                      variant="secondary"
                      size="small"
                      label={t('common.retry')}
                      onClick={() => void loadFeed()}
                    />
                  </div>
                </CivicCard>
              ) : loading ? (
                <div className="social-feed-skeleton" aria-hidden="true">
                  {[0, 1, 2].map((row) => (
                    <div key={row} className="social-feed-skeleton-row" />
                  ))}
                </div>
              ) : visibleItems.length === 0 ? (
                <CivicCard data-testid="community-feed-empty">
                  <CivicEmptyState
                    icon="pi-comments"
                    title={
                      feed.length > 0
                        ? t('community_feed.empty_filter_title')
                        : t('community_feed.empty_title')
                    }
                    description={
                      feed.length > 0
                        ? t('community_feed.empty_filter_desc')
                        : t('community_feed.empty_desc')
                    }
                    actionLabel={
                      feed.length > 0
                        ? t('community_feed.empty_filter_action')
                        : t('community_feed.empty_action')
                    }
                    onAction={() => (feed.length > 0 ? setFilter('all') : setComposerOpen(true))}
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
                          {item.authorName && (
                            <span className="social-feed-author">
                              {t('community_feed.by_author', { name: item.authorName })}
                              {item.authorRole === 'PUBLIC_SERVANT' && (
                                <span className="social-feed-official">{t('community_feed.official')}</span>
                              )}
                            </span>
                          )}
                          <span className="social-feed-time">
                            {formatRelativeTime(item.happenedAt, i18n.language)}
                          </span>
                        </div>
                        <h3 className="social-feed-title">
                          <Link to={itemHref(item)} className="social-feed-title__link">
                            {item.title}
                          </Link>
                        </h3>
                        {item.summary && (
                          <p className="social-feed-summary">
                            <LinkifiedText text={item.summary} />
                          </p>
                        )}
                        <div className="social-feed-actions">
                          {(item.type === 'signal' || item.type === 'blog') &&
                            (['LIKE', 'CELEBRATE'] as const).map((reaction) => {
                              const count = item.reactions?.[reaction] ?? 0;
                              const active = item.viewerReaction === reaction;
                              return (
                                <button
                                  key={reaction}
                                  type="button"
                                  onClick={() => react(item, reaction)}
                                  aria-pressed={active}
                                  className={active ? 'is-active' : undefined}
                                  data-testid={`community-feed-react-${reaction.toLowerCase()}-${item.id}`}
                                >
                                  <i
                                    className={reaction === 'LIKE' ? 'pi pi-heart' : 'pi pi-star'}
                                    aria-hidden="true"
                                  />
                                  {t(reaction === 'LIKE' ? 'community_feed.like' : 'community_feed.celebrate')}
                                  {count > 0 ? ` · ${count}` : ''}
                                </button>
                              );
                            })}
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
                          <button
                            type="button"
                            onClick={() => toggleComments(item)}
                            aria-expanded={replyingTo === commentKey(item)}
                            data-testid={`community-feed-comment-${item.id}`}
                          >
                            <i className="pi pi-comment" aria-hidden="true" />
                            {t('community_feed.comment')}
                            {typeof item.commentCount === 'number' && item.commentCount > 0
                              ? ` · ${item.commentCount}`
                              : ''}
                          </button>
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

                        {replyingTo === commentKey(item) && (
                          <div className="social-feed-reply">
                            <strong className="social-feed-comments-title">
                              {t('community_feed.comments')}
                            </strong>
                            {commentsLoading === commentKey(item) ? (
                              <p className="social-feed-comments-status" role="status">
                                {t('common.loading')}
                              </p>
                            ) : commentsError === commentKey(item) ? (
                              <div className="social-feed-comments-status" role="alert">
                                <span>{t('community_feed.comments_error')}</span>
                                <button type="button" onClick={() => void loadComments(item)}>
                                  {t('common.retry')}
                                </button>
                              </div>
                            ) : comments[commentKey(item)]?.length ? (
                              <ul className="social-feed-comments" aria-live="polite">
                                {comments[commentKey(item)].slice(-3).map((comment) => (
                                  <li key={comment.id}>
                                    <div className="social-feed-comment-meta">
                                      <strong>{comment.author}</strong>
                                      {comment.createdAt && (
                                        <time>{formatRelativeTime(comment.createdAt, i18n.language)}</time>
                                      )}
                                    </div>
                                    <p className="social-feed-comment-body">
                                      <LinkifiedText text={comment.content} />
                                    </p>
                                  </li>
                                ))}
                              </ul>
                            ) : (
                              <p className="social-feed-comments-status">
                                {t('community_feed.comments_empty')}
                              </p>
                            )}
                            <InputTextarea
                              value={replyText}
                              rows={2}
                              maxLength={1000}
                              autoResize
                              onChange={(event) => setReplyText(event.target.value)}
                              placeholder={t('community_feed.reply_placeholder')}
                              aria-label={t('community_feed.comment')}
                              className="w-full"
                              data-testid={`community-feed-reply-input-${item.id}`}
                            />
                            <span className="social-composer-count">{replyText.length}/1000</span>
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
                  <button type="button" onClick={() => navigate('/communities/rooms')}>
                    <i className="pi pi-comments" aria-hidden="true" />
                    {t('community_feed.chat')}
                  </button>
                  {interfaceMode === 'advanced' && (
                    <>
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
                    </>
                  )}
                </div>
                <div className="social-feed-alerts-row">
                  <div>
                    <strong>{t('community_feed.alerts_title')}</strong>
                    <span>{t('community_feed.alerts_desc')}</span>
                  </div>
                  <CivicButton
                    type="button"
                    icon={alertsOn ? 'pi pi-check' : 'pi pi-bell'}
                    variant={alertsOn ? 'secondary' : 'primary'}
                    size="small"
                    label={alertsOn ? t('community_feed.alerts_on') : t('community_feed.alerts_off')}
                    onClick={toggleAlerts}
                    data-testid="community-alerts-toggle"
                  />
                </div>
              </CivicCard>

              <CivicCard title={t('community_feed.trending')} className="mt-4">
                {trending.length > 0 ? (
                  <ul className="social-feed-trending">
                    {trending.map((item) => (
                      <li key={`${item.type}-${item.id}`}>
                        <Link to={itemHref(item)}>{item.title}</Link>
                        <span>
                          <i className="pi pi-heart" aria-hidden="true" /> {reactionTotal(item)}
                        </span>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="text-sm text-muted m-0">{t('community_feed.trending_empty')}</p>
                )}
              </CivicCard>
            </aside>
          </div>
        )}
      </div>
    </Layout>
  );
}
