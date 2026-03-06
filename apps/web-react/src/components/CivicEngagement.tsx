import { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-hot-toast';
import { Avatar } from 'primereact/avatar';
import { InputTextarea } from 'primereact/inputtextarea';
import { useTranslation } from "react-i18next";
import { CivicComment } from '../types';
import apiClient from '../api/axios';
import { CivicButton } from './ui/CivicButton';
import { CivicCard } from './ui/CivicCard';
import { toRoleListLabel } from "../constants/roleLabels";
import { CivicCharacterCount } from "./ui/CivicCharacterCount";
import { FORM_LIMITS } from "../constants/formLimits";
import { isSubmitShortcut } from "../utils/keyboard";

interface Props {
  parentId: string;
  parentType: 'SIGNAL' | 'BLOG';
  initialReactions?: Record<string, number>;
  initialViewerReaction?: string;
  initialCommentCount?: number;
  autoloadComments?: boolean;
}

const REACTION_TYPES = ["👍", "🔥", "🙌", "📍", "👏", "🆘"];

export function CivicEngagement({
  parentId,
  parentType,
  initialReactions = {},
  initialViewerReaction,
  initialCommentCount = 0,
  autoloadComments = true,
}: Props) {
  const { t } = useTranslation();
  const [comments, setComments] = useState<CivicComment[]>([]);
  const [reactions, setReactions] = useState<Record<string, number>>(initialReactions);
  const [viewerReaction, setViewerReaction] = useState<string | undefined>(initialViewerReaction);
  const [newComment, setNewMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [reactionPending, setReactionPending] = useState<string | null>(null);
  const [commentsVisible, setCommentsVisible] = useState(autoloadComments);
  const [commentsLoaded, setCommentsLoaded] = useState(false);
  const [commentCount, setCommentCount] = useState(initialCommentCount);
  const [replyTarget, setReplyTarget] = useState<CivicComment | null>(null);

  const minCommentLength = FORM_LIMITS.threads.messageMin;
  const maxCommentLength = FORM_LIMITS.threads.messageMax;
  const currentCommentLength = newComment.trim().length;

  const loadData = useCallback(async () => {
    try {
      const endpoint = parentType === 'SIGNAL' 
        ? `signals/${parentId}/comments` 
        : `community/blog/${parentId}/comments`;
      const res = await apiClient.get(endpoint);
      const nextComments = res.data || [];
      setComments(nextComments);
      setCommentCount(nextComments.length);
      setCommentsLoaded(true);
    } catch (err) {
      console.error("Failed to load engagement data", err);
    }
  }, [parentId, parentType]);

  useEffect(() => {
    if (autoloadComments) {
      loadData();
    }
  }, [autoloadComments, loadData]);

  useEffect(() => {
    if (!autoloadComments && commentsVisible && !commentsLoaded) {
      loadData();
    }
  }, [autoloadComments, commentsVisible, commentsLoaded, loadData]);

  useEffect(() => {
    setReactions(initialReactions || {});
  }, [initialReactions]);

  useEffect(() => {
    setViewerReaction(initialViewerReaction);
  }, [initialViewerReaction]);

  const handleReact = async (type: string) => {
    if (reactionPending) return;
    setReactionPending(type);
    try {
      const endpoint = parentType === 'SIGNAL' 
        ? `signals/${parentId}/react` 
        : `community/blog/${parentId}/react`;
      const res = await apiClient.post(endpoint, { type });
      setReactions(res.data?.reactions || {});
      setViewerReaction(res.data?.viewerReaction || undefined);
    } catch (err) {
      toast.error(t("engagement.reaction_failed"));
    } finally {
      setReactionPending(null);
    }
  };

  const handleAddComment = async () => {
    const trimmedComment = newComment.trim();
    if (trimmedComment.length < minCommentLength) return;
    if (!commentsVisible) {
      setCommentsVisible(true);
    }
    setLoading(true);
    try {
      const endpoint = parentType === 'SIGNAL' 
        ? `signals/${parentId}/comments` 
        : `community/blog/${parentId}/comments`;
      const replyPrefix = replyTarget ? `@${replyTarget.authorUsername} ` : "";
      await apiClient.post(endpoint, { content: `${replyPrefix}${trimmedComment}`.trim() });
      setNewMessage("");
      setReplyTarget(null);
      toast.success(t("engagement.comment_added"));
      loadData();
    } catch (err) {
      toast.error(t("engagement.comment_failed"));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-column gap-6 mt-8">
      {/* REACTION BAR */}
      <div className="flex flex-wrap align-items-center gap-2 p-3 bg-surface-soft border-round-2xl border-1 border-surface-soft">
        <span className="text-xs font-black uppercase text-muted px-2 mr-2">{t("engagement.reactions")}</span>
        {REACTION_TYPES.map(emoji => (
          <button 
            key={emoji}
            type="button"
            onClick={() => handleReact(emoji)}
            disabled={Boolean(reactionPending)}
            className={`flex align-items-center gap-2 px-3 py-2 border-round-xl border-1 transition-all cursor-pointer group ${
              viewerReaction === emoji
                ? "bg-brand-primary-alpha-25 border-brand-primary-alpha-60 shadow-1"
                : "bg-surface-soft-strong border-surface-soft hover:border-brand-primary-alpha-30"
            }`}
          >
            <span className="text-base group-hover:scale-125 transition-transform">{emoji}</span>
            <span className="text-xs font-black text-main">{reactions[emoji] || 0}</span>
          </button>
        ))}
      </div>

      {/* COMMENTS SECTION */}
      <CivicCard title={t("engagement.public_discussion")} padding="none">
        <div className="flex flex-column">
          <div className="flex align-items-center justify-content-between p-4 bg-surface-soft-strong border-bottom-1 border-surface-soft">
            <span className="text-xs font-black uppercase tracking-widest text-muted">
              {t("engagement.comments")} ({commentCount})
            </span>
            <CivicButton
              label={commentsVisible ? t("engagement.hide_comments") : t("engagement.view_comments")}
              variant="ghost"
              icon={commentsVisible ? "pi pi-chevron-up" : "pi pi-chevron-down"}
              onClick={() => setCommentsVisible((prev) => !prev)}
            />
          </div>

          {commentsVisible && (
            <>
          {/* COMMENT LIST */}
          <div className="flex flex-column gap-px bg-surface-soft">
            {comments.length === 0 ? (
              <div className="p-8 text-center text-muted italic">{t("engagement.empty_comments")}</div>
            ) : (
              comments.map((comment) => (
                <div key={comment.id} className="bg-surface p-5 hover:bg-surface-soft transition-colors">
                  <div className="flex align-items-center gap-3 mb-3">
                    <Avatar label={comment.authorUsername[0].toUpperCase()} shape="circle" className="bg-brand-primary text-white" />
                    <div className="flex flex-column">
                      <span className="text-sm font-black text-main">{comment.authorUsername}</span>
                      <span className="text-xs font-bold text-muted uppercase tracking-widest">{toRoleListLabel(comment.authorRole, t)}</span>
                    </div>
                    <span className="text-xs text-muted ml-auto">{new Date(comment.createdAt).toLocaleString()}</span>
                  </div>
                  <p className="m-0 text-secondary text-base line-height-3 font-medium">{comment.content}</p>
                  <div className="mt-3 flex justify-content-end">
                    <CivicButton
                      type="button"
                      variant="ghost"
                      size="small"
                      icon="pi pi-reply"
                      label={t("engagement.reply")}
                      onClick={() => {
                        setCommentsVisible(true);
                        setReplyTarget(comment);
                        setNewMessage((prev) => prev || `@${comment.authorUsername} `);
                      }}
                    />
                  </div>
                </div>
              ))
            )}
          </div>

          {/* INPUT AREA */}
          <div className="p-5 bg-surface-soft-strong border-top-1 border-surface-soft">
            <div className="flex flex-column gap-3">
              {replyTarget && (
                <div className="flex align-items-center justify-content-between gap-3 bg-brand-primary-alpha-10 border-1 border-brand-primary-alpha-20 border-round-xl px-3 py-2">
                  <span className="text-sm text-main">
                    {t("engagement.replying_to", { user: replyTarget.authorUsername })}
                  </span>
                  <CivicButton
                    type="button"
                    variant="ghost"
                    size="small"
                    label={t("engagement.cancel_reply")}
                    onClick={() => setReplyTarget(null)}
                  />
                </div>
              )}
              <InputTextarea 
                value={newComment}
                onChange={(e) => setNewMessage(e.target.value)}
                onKeyDown={(e) => {
                  if (isSubmitShortcut(e) && currentCommentLength >= minCommentLength && !loading) {
                    e.preventDefault();
                    handleAddComment();
                  }
                }}
                placeholder={t("engagement.comment_placeholder")}
                rows={3}
                className="w-full bg-surface"
                maxLength={maxCommentLength}
              />
              <CivicCharacterCount current={currentCommentLength} max={maxCommentLength} min={minCommentLength} />
              <small className="text-muted text-xs">{t("engagement.submit_shortcut_hint")}</small>
              <div className="flex justify-content-end">
                <CivicButton 
                  label={t("engagement.post_comment")} 
                  icon="pi pi-send" 
                  onClick={handleAddComment} 
                  loading={loading}
                  disabled={currentCommentLength < minCommentLength}
                />
              </div>
            </div>
          </div>
            </>
          )}
        </div>
      </CivicCard>
    </div>
  );
}
