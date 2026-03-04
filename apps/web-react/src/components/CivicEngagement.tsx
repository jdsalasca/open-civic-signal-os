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
    if (!newComment.trim()) return;
    if (!commentsVisible) {
      setCommentsVisible(true);
    }
    setLoading(true);
    try {
      const endpoint = parentType === 'SIGNAL' 
        ? `signals/${parentId}/comments` 
        : `community/blog/${parentId}/comments`;
      await apiClient.post(endpoint, { content: newComment });
      setNewMessage("");
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
      <div className="flex flex-wrap align-items-center gap-2 p-3 bg-white-alpha-5 border-round-2xl border-1 border-white-alpha-10">
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
                : "bg-black-alpha-40 border-transparent hover:border-brand-primary-alpha-30"
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
          <div className="flex align-items-center justify-content-between p-4 bg-black-alpha-20 border-bottom-1 border-white-alpha-10">
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
          <div className="flex flex-column gap-px bg-white-alpha-10">
            {comments.length === 0 ? (
              <div className="p-8 text-center text-muted italic">{t("engagement.empty_comments")}</div>
            ) : (
              comments.map((comment) => (
                <div key={comment.id} className="bg-surface p-5 hover:bg-white-alpha-5 transition-colors">
                  <div className="flex align-items-center gap-3 mb-3">
                    <Avatar label={comment.authorUsername[0].toUpperCase()} shape="circle" className="bg-brand-primary text-white" />
                    <div className="flex flex-column">
                      <span className="text-sm font-black text-main">{comment.authorUsername}</span>
                      <span className="text-min font-bold text-muted uppercase tracking-widest" style={{fontSize: '8px'}}>{toRoleListLabel(comment.authorRole, t)}</span>
                    </div>
                    <span className="text-min text-muted ml-auto">{new Date(comment.createdAt).toLocaleString()}</span>
                  </div>
                  <p className="m-0 text-secondary text-base line-height-3 font-medium">{comment.content}</p>
                </div>
              ))
            )}
          </div>

          {/* INPUT AREA */}
          <div className="p-5 bg-black-alpha-20 border-top-1 border-white-alpha-10">
            <div className="flex flex-column gap-3">
              <InputTextarea 
                value={newComment}
                onChange={(e) => setNewMessage(e.target.value)}
                placeholder={t("engagement.comment_placeholder")}
                rows={3}
                className="w-full bg-surface"
              />
              <div className="flex justify-content-end">
                <CivicButton 
                  label={t("engagement.post_comment")} 
                  icon="pi pi-send" 
                  onClick={handleAddComment} 
                  loading={loading}
                  disabled={!newComment.trim()}
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
