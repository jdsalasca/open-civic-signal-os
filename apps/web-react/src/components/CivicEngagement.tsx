import { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-hot-toast';
import { Avatar } from 'primereact/avatar';
import { InputTextarea } from 'primereact/inputtextarea';
import { CivicComment } from '../types';
import apiClient from '../api/axios';
import { CivicButton } from './ui/CivicButton';
import { CivicCard } from './ui/CivicCard';

interface Props {
  parentId: string;
  parentType: 'SIGNAL' | 'BLOG';
  initialReactions?: Record<string, number>;
}

const REACTION_TYPES = ["👍", "🔥", "🙌", "📍", "👏", "🆘"];

export function CivicEngagement({ parentId, parentType, initialReactions = {} }: Props) {
  const [comments, setComments] = useState<CivicComment[]>([]);
  const [reactions, setReactions] = useState<Record<string, number>>(initialReactions);
  const [newComment, setNewMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const loadData = useCallback(async () => {
    try {
      const endpoint = parentType === 'SIGNAL' 
        ? `signals/${parentId}/comments` 
        : `community/blog/${parentId}/comments`;
      const res = await apiClient.get(endpoint);
      setComments(res.data || []);
    } catch (err) {
      console.error("Failed to load engagement data", err);
    }
  }, [parentId, parentType]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleReact = async (type: string) => {
    try {
      const endpoint = parentType === 'SIGNAL' 
        ? `signals/${parentId}/react` 
        : `community/blog/${parentId}/react`;
      const res = await apiClient.post(endpoint, { type });
      setReactions(res.data);
    } catch (err) {
      toast.error("Reaction failed");
    }
  };

  const handleAddComment = async () => {
    if (!newComment.trim()) return;
    setLoading(true);
    try {
      const endpoint = parentType === 'SIGNAL' 
        ? `signals/${parentId}/comments` 
        : `community/blog/${parentId}/comments`;
      await apiClient.post(endpoint, { content: newComment });
      setNewMessage("");
      toast.success("Comment added");
      loadData();
    } catch (err) {
      toast.error("Failed to post comment");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-column gap-6 mt-8">
      {/* REACTION BAR */}
      <div className="flex flex-wrap align-items-center gap-2 p-3 bg-white-alpha-5 border-round-2xl border-1 border-white-alpha-10">
        <span className="text-xs font-black uppercase text-muted px-2 mr-2">Community Sentiment</span>
        {REACTION_TYPES.map(emoji => (
          <button 
            key={emoji}
            onClick={() => handleReact(emoji)}
            className="flex align-items-center gap-2 px-3 py-2 border-round-xl bg-black-alpha-40 border-1 border-transparent hover:border-brand-primary-alpha-30 transition-all cursor-pointer group"
          >
            <span className="text-base group-hover:scale-125 transition-transform">{emoji}</span>
            <span className="text-xs font-black text-main">{reactions[emoji] || 0}</span>
          </button>
        ))}
      </div>

      {/* COMMENTS SECTION */}
      <CivicCard title="Public Discussion" padding="none">
        <div className="flex flex-column">
          {/* COMMENT LIST */}
          <div className="flex flex-column gap-px bg-white-alpha-10">
            {comments.length === 0 ? (
              <div className="p-8 text-center text-muted italic">No comments yet. Start the conversation.</div>
            ) : (
              comments.map((comment) => (
                <div key={comment.id} className="bg-surface p-5 hover:bg-white-alpha-5 transition-colors">
                  <div className="flex align-items-center gap-3 mb-3">
                    <Avatar label={comment.authorUsername[0].toUpperCase()} shape="circle" className="bg-brand-primary text-white" />
                    <div className="flex flex-column">
                      <span className="text-sm font-black text-main">{comment.authorUsername}</span>
                      <span className="text-min font-bold text-muted uppercase tracking-widest" style={{fontSize: '8px'}}>{comment.authorRole}</span>
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
                placeholder="Share your perspective..."
                rows={3}
                className="w-full bg-surface"
              />
              <div className="flex justify-content-end">
                <CivicButton 
                  label="Post Comment" 
                  icon="pi pi-send" 
                  onClick={handleAddComment} 
                  loading={loading}
                  disabled={!newComment.trim()}
                />
              </div>
            </div>
          </div>
        </div>
      </CivicCard>
    </div>
  );
}
