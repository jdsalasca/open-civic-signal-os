import { useCallback, useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
import { Avatar } from "primereact/avatar";
import { useTranslation } from "react-i18next";
import { CommunityBlogPost } from "../types";
import { Layout } from "../components/Layout";
import { useCommunityStore } from "../store/useCommunityStore";
import { useAuthStore } from "../store/useAuthStore";
import apiClient from "../api/axios";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicField } from "../components/ui/CivicField";
import { CivicEngagement } from "../components/CivicEngagement";
import { CivicSelect } from "../components/ui/CivicSelect";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";
import { CivicCharacterCount } from "../components/ui/CivicCharacterCount";
import { FORM_LIMITS } from "../constants/formLimits";

type ApiError = Error & { friendlyMessage?: string };

export function CommunityBlog() {
  const { t } = useTranslation();
  const { activeCommunityId, memberships } = useCommunityStore();
  const { activeRole } = useAuthStore();
  const [posts, setPosts] = useState<CommunityBlogPost[]>([]);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [statusTag, setStatusTag] = useState("IN_PROGRESS");
  const [publishing, setPublishing] = useState(false);

  const isStaff = activeRole === "PUBLIC_SERVANT" || activeRole === "SUPER_ADMIN";
  const titleLength = title.trim().length;
  const contentLength = content.trim().length;
  const statusTagOptions = [
    { label: t('community_blog.status_planned'), value: "PLANNED" },
    { label: t('community_blog.status_in_progress'), value: "IN_PROGRESS" },
    { label: t('community_blog.status_completed'), value: "COMPLETED" },
    { label: t('community_blog.status_blocked'), value: "BLOCKED" },
  ];
  const canPublish = Boolean(
    activeCommunityId &&
    titleLength >= FORM_LIMITS.blog.titleMin &&
    contentLength >= FORM_LIMITS.blog.contentMin
  );
  const activeCommunityName = memberships.find(m => m.communityId === activeCommunityId)?.communityName;

  const loadPosts = useCallback(async () => {
    if (!activeCommunityId) return;
    try {
      const res = await apiClient.get(`community/blog?communityId=${activeCommunityId}`);
      setPosts(res.data || []);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('community_blog.load_error'));
    }
  }, [activeCommunityId, t]);

  useEffect(() => {
    loadPosts();
  }, [loadPosts]);

  const createPost = async () => {
    if (!canPublish) return;
    setPublishing(true);
    try {
      await apiClient.post("community/blog", {
        communityId: activeCommunityId,
        title,
        content,
        statusTag,
      });
      setTitle("");
      setContent("");
      toast.success(t('community_blog.publish_success'));
      loadPosts();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('community_blog.publish_error'));
    } finally {
      setPublishing(false);
    }
  };

  const getStatusSeverity = (tag: string) => {
    switch (tag) {
      case 'COMPLETED': return 'resolved';
      case 'BLOCKED': return 'rejected';
      case 'IN_PROGRESS': return 'progress';
      default: return 'new';
    }
  };

  return (
    <Layout>
      <div className="animate-fade-up">
        <CivicPageHeader
          title={t('community_blog.title')}
          description={t('community_blog.desc', { community: activeCommunityName || t('community_blog.default_community') })}
        />

        <div className="grid">
          {isStaff && (
            <div className="col-12 lg:col-4">
              <CivicCard title={t('community_blog.dispatch_title')} variant="brand">
                <div className="flex flex-column gap-2">
                  <CivicField label={t('community_blog.headline')}>
                    <div className="flex flex-column gap-2">
                      <InputText
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        placeholder={t('community_blog.headline_placeholder')}
                        className="w-full"
                        data-testid="blog-title-input"
                        maxLength={FORM_LIMITS.blog.titleMax}
                      />
                      <CivicCharacterCount current={title.length} max={FORM_LIMITS.blog.titleMax} min={FORM_LIMITS.blog.titleMin} />
                    </div>
                  </CivicField>
                  
                  <CivicField label={t('community_blog.context')}>
                    <div className="flex flex-column gap-2">
                      <InputTextarea
                        value={content}
                        onChange={(e) => setContent(e.target.value)}
                        rows={6}
                        className="w-full"
                        placeholder={t('community_blog.context_placeholder')}
                        data-testid="blog-content-input"
                        maxLength={FORM_LIMITS.blog.contentMax}
                      />
                      <CivicCharacterCount current={content.length} max={FORM_LIMITS.blog.contentMax} min={FORM_LIMITS.blog.contentMin} />
                    </div>
                  </CivicField>

                  <CivicField label={t('community_blog.status')}>
                    <CivicSelect
                      value={statusTag}
                      options={statusTagOptions}
                      onChange={(e) => setStatusTag(e.value)}
                      className="w-full"
                    />
                  </CivicField>

                  <CivicButton
                    label={t('community_blog.publish')}
                    icon="pi pi-send"
                    onClick={createPost}
                    disabled={!canPublish}
                    loading={publishing}
                    className="w-full py-4 mt-2"
                    glow
                    data-testid="publish-blog-button"
                  />
                </div>
              </CivicCard>
            </div>
          )}

          <div className={isStaff ? "col-12 lg:col-8" : "col-12 lg:col-8 lg:col-offset-2"}>
            {posts.length === 0 ? (
              <CivicCard className="text-center p-8">
                <i className="pi pi-history text-4xl text-muted mb-4 block"></i>
                <h3 className="text-main text-2xl font-black m-0">{t('community_blog.empty_title')}</h3>
                <p className="text-secondary mt-2">{t('community_blog.empty_desc')}</p>
              </CivicCard>
            ) : (
              <div className="flex flex-column gap-8">
                {posts.map((post) => (
                  <div key={post.id} className="flex flex-column gap-4">
                    <div className="glass-panel border-round-3xl p-6 hover:border-white-alpha-30 transition-all duration-300">
                      <div className="flex justify-content-between align-items-start mb-6">
                        <div className="flex align-items-center gap-3">
                          <Avatar label={post.authorUsername?.[0].toUpperCase()} shape="circle" className="bg-brand-primary text-white font-bold shadow-4" />
                          <div className="flex flex-column">
                            <span className="text-sm font-bold text-main">{post.authorUsername}</span>
                            <span className="text-xs text-muted font-bold uppercase tracking-tighter">{post.authorRole}</span>
                          </div>
                        </div>
                        <CivicBadge label={post.statusTag.replace('_', ' ')} severity={getStatusSeverity(post.statusTag)} />
                      </div>

                      <h2 className="text-3xl font-black text-main m-0 mb-4 tracking-tight leading-tight">{post.title}</h2>
                      <p className="text-secondary text-lg line-height-4 font-medium opacity-90 m-0 mb-6" style={{whiteSpace: 'pre-wrap'}}>{post.content}</p>
                      
                      <div className="flex justify-content-between align-items-center pt-6 border-top-1 border-white-alpha-10">
                        <div className="flex align-items-center gap-2 text-xs text-muted font-bold uppercase tracking-widest">
                          <i className="pi pi-calendar"></i>
                          <span>{new Date(post.publishedAt).toLocaleDateString()}</span>
                          <span className="mx-1">•</span>
                          <span>{new Date(post.publishedAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
                        </div>
                      </div>
                    </div>
                    
                    <div className="px-4">
                      <CivicEngagement 
                        parentId={post.id} 
                        parentType="BLOG" 
                        initialReactions={post.reactions} 
                      />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}
