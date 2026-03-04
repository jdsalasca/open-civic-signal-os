import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
import { Avatar } from "primereact/avatar";
import { useTranslation } from "react-i18next";
import { CommunityBlogPost } from "../types";
import { Layout } from "../components/Layout";
import { useCommunityStore } from "../store/useCommunityStore";
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
import { CivicEmptyState } from "../components/ui/CivicEmptyState";
import { CivicActionBar } from "../components/ui/CivicActionBar";
import { toRoleListLabel } from "../constants/roleLabels";
import { extractFirstImageUrl, isValidImageUrl, prependImageToContent, stripMarkdownImages } from "../utils/communityContent";

type ApiError = Error & { friendlyMessage?: string };

const renderContent = (content: string) => {
  const cleaned = stripMarkdownImages(content);
  return (
    <p className="text-secondary text-base line-height-4 font-medium m-0" style={{ whiteSpace: "pre-wrap" }}>
      {cleaned}
    </p>
  );
};

export function CommunityBlog() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { activeCommunityId, memberships } = useCommunityStore();
  const [posts, setPosts] = useState<CommunityBlogPost[]>([]);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [imageUrl, setImageUrl] = useState("");
  const [statusTag, setStatusTag] = useState("IN_PROGRESS");
  const [publishing, setPublishing] = useState(false);
  const [commentCountsByPost, setCommentCountsByPost] = useState<Record<string, number>>({});

  const activeMembership = memberships.find((m) => m.communityId === activeCommunityId);
  const canPublishByRole =
    activeMembership?.role === "PUBLIC_SERVANT_LIAISON" || activeMembership?.role === "COORDINATOR";
  const titleLength = title.trim().length;
  const contentLength = content.trim().length;
  const hasValidImageUrl = isValidImageUrl(imageUrl);

  const statusTagOptions = useMemo(
    () => [
      { label: t("community_blog.status_planned"), value: "PLANNED" },
      { label: t("community_blog.status_in_progress"), value: "IN_PROGRESS" },
      { label: t("community_blog.status_completed"), value: "COMPLETED" },
      { label: t("community_blog.status_blocked"), value: "BLOCKED" },
    ],
    [t]
  );

  const canPublish = Boolean(
    activeCommunityId &&
      titleLength >= FORM_LIMITS.blog.titleMin &&
      contentLength >= FORM_LIMITS.blog.contentMin &&
      hasValidImageUrl
  );

  const activeCommunityName = memberships.find((m) => m.communityId === activeCommunityId)?.communityName;

  const loadPosts = useCallback(async () => {
    if (!activeCommunityId) return;
    try {
      const res = await apiClient.get(`community/blog?communityId=${activeCommunityId}`);
      const timeline = res.data || [];
      setPosts(timeline);

      if (timeline.length === 0) {
        setCommentCountsByPost({});
        return;
      }

      const postIds = timeline.map((post: CommunityBlogPost) => post.id).join(",");
      const countRes = await apiClient.get(`community/blog/comments/count?postIds=${postIds}`);
      setCommentCountsByPost(countRes.data || {});
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_blog.load_error"));
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
        content: prependImageToContent(content, imageUrl, t("community_blog.image_alt")),
        statusTag,
      });
      setTitle("");
      setContent("");
      setImageUrl("");
      toast.success(t("community_blog.publish_success"));
      loadPosts();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_blog.publish_error"));
    } finally {
      setPublishing(false);
    }
  };

  const getStatusSeverity = (tag: string) => {
    switch (tag) {
      case "COMPLETED":
        return "resolved";
      case "BLOCKED":
        return "rejected";
      case "IN_PROGRESS":
        return "progress";
      default:
        return "new";
    }
  };

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <CivicPageHeader
          title={t("community_blog.title")}
          description={t("community_blog.desc", { community: activeCommunityName || t("community_blog.default_community") })}
        />
        <CivicActionBar className="mb-5">
          {canPublishByRole && (
            <CivicButton
              type="button"
              icon="pi pi-pencil"
              label="Write post"
              onClick={() => document.getElementById("blog-compose-card")?.scrollIntoView({ behavior: "smooth", block: "start" })}
            />
          )}
          <CivicButton type="button" icon="pi pi-comments" label="Threads" variant="secondary" onClick={() => navigate("/communities/threads")} />
          <CivicButton type="button" icon="pi pi-bolt" label="Live feed" variant="ghost" onClick={() => navigate("/communities/feed")} />
          {activeCommunityId && !canPublishByRole && (
            <span className="text-sm text-muted font-semibold" data-testid="blog-create-permission-note">
              {t("community_blog.permission_note")}
            </span>
          )}
        </CivicActionBar>

        <div className="grid">
          {canPublishByRole && (
            <div className="col-12 lg:col-4">
              <CivicCard id="blog-compose-card" title={t("community_blog.dispatch_title")} variant="brand">
                <div className="flex flex-column gap-3">
                  <CivicField label={t("community_blog.headline")}>
                    <div className="flex flex-column gap-2">
                      <InputText
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        placeholder={t("community_blog.headline_placeholder")}
                        className="w-full"
                        data-testid="blog-title-input"
                        maxLength={FORM_LIMITS.blog.titleMax}
                      />
                      <CivicCharacterCount current={title.length} max={FORM_LIMITS.blog.titleMax} min={FORM_LIMITS.blog.titleMin} />
                    </div>
                  </CivicField>

                  <CivicField label={t("community_blog.context")}>
                    <div className="flex flex-column gap-2">
                      <InputTextarea
                        value={content}
                        onChange={(e) => setContent(e.target.value)}
                        rows={8}
                        className="w-full"
                        placeholder={t("community_blog.context_placeholder")}
                        data-testid="blog-content-input"
                        maxLength={FORM_LIMITS.blog.contentMax}
                      />
                      <small className="text-muted text-xs">{t("community_blog.image_hint")}</small>
                      <CivicCharacterCount current={content.length} max={FORM_LIMITS.blog.contentMax} min={FORM_LIMITS.blog.contentMin} />
                    </div>
                  </CivicField>
                  <CivicField label={t("community_blog.image_url")}>
                    <div className="flex flex-column gap-2">
                      <InputText
                        value={imageUrl}
                        onChange={(e) => setImageUrl(e.target.value)}
                        placeholder={t("community_blog.image_url_placeholder")}
                        className="w-full"
                        data-testid="blog-image-url-input"
                        maxLength={1200}
                      />
                      {!hasValidImageUrl ? (
                        <small className="p-error">{t("community_blog.image_url_invalid")}</small>
                      ) : (
                        <small className="text-muted text-xs">{t("community_blog.image_url_help")}</small>
                      )}
                    </div>
                  </CivicField>
                  {imageUrl.trim() && hasValidImageUrl && (
                    <div className="border-round-xl overflow-hidden border-1 border-subtle">
                      <img
                        src={imageUrl.trim()}
                        alt={t("community_blog.image_alt")}
                        className="w-full"
                        style={{ maxHeight: "14rem", objectFit: "cover" }}
                        loading="lazy"
                      />
                    </div>
                  )}

                  <CivicField label={t("community_blog.status")}>
                    <CivicSelect
                      value={statusTag}
                      options={statusTagOptions}
                      onChange={(e) => setStatusTag(e.value)}
                      className="w-full"
                    />
                  </CivicField>

                  <CivicButton
                    type="button"
                    label={t("community_blog.publish")}
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

          <div className={canPublishByRole ? "col-12 lg:col-8" : "col-12 lg:col-8 lg:col-offset-2"}>
            {!activeCommunityId ? (
              <CivicCard>
                <CivicEmptyState
                  icon="pi-map-marker"
                  title={t("community_blog.empty_title")}
                  description={t("report.community_required")}
                  actionLabel={t("nav.communities")}
                  onAction={() => navigate("/communities")}
                />
              </CivicCard>
            ) : posts.length === 0 ? (
              <CivicCard>
                <CivicEmptyState
                  icon="pi-history"
                  title={t("community_blog.empty_title")}
                  description={t("community_blog.empty_desc")}
                />
              </CivicCard>
            ) : (
              <div className="flex flex-column gap-4">
                {posts.map((post) => {
                  const coverImage = extractFirstImageUrl(post.content);
                  return (
                    <article key={post.id} className="glass-panel border-round-3xl overflow-hidden">
                      {coverImage && (
                        <img
                          src={coverImage}
                          alt={post.title}
                          loading="lazy"
                          className="w-full"
                          style={{ maxHeight: "320px", objectFit: "cover" }}
                        />
                      )}

                      <div className="p-5 md:p-6 flex flex-column gap-4">
                        <header className="flex justify-content-between align-items-start gap-3">
                          <div className="flex align-items-center gap-3">
                            <Avatar label={post.authorUsername?.[0]?.toUpperCase()} shape="circle" className="bg-brand-primary text-white font-bold" />
                            <div className="flex flex-column">
                              <span className="text-sm font-bold text-main">{post.authorUsername}</span>
                              <span className="text-xs text-muted">{toRoleListLabel(post.authorRole, t)}</span>
                            </div>
                          </div>
                          <CivicBadge label={post.statusTag.replace("_", " ")} severity={getStatusSeverity(post.statusTag)} />
                        </header>

                        <div>
                          <h2 className="text-2xl font-black text-main m-0 mb-3">{post.title}</h2>
                          {renderContent(post.content)}
                        </div>

                        <div className="flex align-items-center gap-2 text-xs text-muted border-top-1 border-white-alpha-10 pt-3">
                          <i className="pi pi-calendar" />
                          <span>{new Date(post.publishedAt).toLocaleDateString()}</span>
                          <span>•</span>
                          <span>{new Date(post.publishedAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</span>
                        </div>

                        <CivicEngagement
                          parentId={post.id}
                          parentType="BLOG"
                          initialReactions={post.reactions}
                          initialViewerReaction={post.viewerReaction}
                          initialCommentCount={commentCountsByPost[post.id] ?? 0}
                          autoloadComments={false}
                        />
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}
