import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

type Props = {
  itemId: string;
  reactions?: Record<string, number> | null;
  viewerReaction?: string | null;
  likeKey: string;
  onReact: (key: string) => void;
  onVote: (key: 'UPVOTE' | 'DOWNVOTE') => void;
};

const EXTRA_REACTIONS = [
  { key: '❤️', emoji: '❤️', slug: 'love', labelKey: 'community_feed.love' },
  { key: '😂', emoji: '😂', slug: 'haha', labelKey: 'community_feed.haha' },
  { key: '😮', emoji: '😮', slug: 'wow', labelKey: 'community_feed.wow' },
  { key: '😢', emoji: '😢', slug: 'sad', labelKey: 'community_feed.sad' },
] as const;

/** Up/down votes plus a five-emoji reaction picker, shared by every feed card. */
export function FeedReactionBar({
  itemId,
  reactions,
  viewerReaction,
  likeKey,
  onReact,
  onVote,
}: Props) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const close = (event: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false);
    };
    document.addEventListener('mousedown', close);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', close);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  const options = [
    { key: likeKey, emoji: '👍', slug: 'like', label: t('community_feed.like') },
    ...EXTRA_REACTIONS.map((reaction) => ({
      key: reaction.key,
      emoji: reaction.emoji,
      slug: reaction.slug,
      label: t(reaction.labelKey),
    })),
  ];
  const current = options.find((option) => option.key === viewerReaction);
  const total = options.reduce((sum, option) => sum + (reactions?.[option.key] ?? 0), 0);

  return (
    <>
      <button
        type="button"
        onClick={() => onVote('UPVOTE')}
        aria-pressed={viewerReaction === 'UPVOTE'}
        aria-label={t('community_feed.upvote')}
        title={t('community_feed.upvote')}
        className={viewerReaction === 'UPVOTE' ? 'is-active is-vote' : 'is-vote'}
        data-testid={`community-feed-upvote-${itemId}`}
      >
        <i className="pi pi-arrow-up" aria-hidden="true" />
        <span>{reactions?.UPVOTE ?? 0}</span>
      </button>
      <button
        type="button"
        onClick={() => onVote('DOWNVOTE')}
        aria-pressed={viewerReaction === 'DOWNVOTE'}
        aria-label={t('community_feed.downvote')}
        title={t('community_feed.downvote')}
        className={viewerReaction === 'DOWNVOTE' ? 'is-active is-vote' : 'is-vote'}
        data-testid={`community-feed-downvote-${itemId}`}
      >
        <i className="pi pi-arrow-down" aria-hidden="true" />
        <span>{reactions?.DOWNVOTE ?? 0}</span>
      </button>
      <div className="feed-reaction" ref={rootRef}>
        <button
          type="button"
          onClick={() => setOpen((value) => !value)}
          aria-haspopup="menu"
          aria-expanded={open}
          data-testid={`community-feed-react-trigger-${itemId}`}
        >
          <span aria-hidden="true">{current?.emoji ?? '😊'}</span>
          {current ? current.label : t('community_feed.react')}
          {total > 0 ? ` · ${total}` : ''}
        </button>
        {open && (
          <div className="feed-reaction-menu" role="menu" aria-label={t('community_feed.react')}>
            {options.map((option) => {
              const count = reactions?.[option.key] ?? 0;
              return (
                <button
                  key={option.key}
                  type="button"
                  role="menuitemradio"
                  aria-checked={viewerReaction === option.key}
                  aria-label={`${option.label}${count > 0 ? ` · ${count}` : ''}`}
                  title={option.label}
                  className={viewerReaction === option.key ? 'is-active' : undefined}
                  onClick={() => {
                    onReact(option.key);
                    setOpen(false);
                  }}
                  data-testid={`community-feed-react-${option.slug}-${itemId}`}
                >
                  <span aria-hidden="true">{option.emoji}</span>
                  <small>{count}</small>
                </button>
              );
            })}
          </div>
        )}
      </div>
    </>
  );
}
