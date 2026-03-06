import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
import { Checkbox } from "primereact/checkbox";
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
import { isSubmitShortcut } from "../utils/keyboard";

type ApiError = Error & {
  friendlyMessage?: string;
  response?: { status?: number; data?: { message?: string } };
};

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
  const [pinned, setPinned] = useState(false);
  const [publishPermissionReason, setPublishPermissionReason] = useState("");
  const [archivingPostId, setArchivingPostId] = useState<string | null>(null);
  const [archivePosts, setArchivePosts] = useState<CommunityBlogPost[]>([]);
  const [archiveQuery, setArchiveQuery] = useState("");
  const [archiveDateFrom, setArchiveDateFrom] = useState("");
  const [archiveDateTo, setArchiveDateTo] = useState("");
  const [loadingArchive, setLoadingArchive] = useState(false);
  const [commentCountsByPost, setCommentCountsByPost] = useState<Record<string, number>>({});
  const timelineLoadInFlightRef = useRef<string | null>(null);

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
  const pinnedPosts = posts.filter((post) => post.pinned);
  const timelinePosts = posts.filter((post) => !post.pinned);

  const loadPosts = useCallback(async () => {
    if (!activeCommunityId) return;
    if (timelineLoadInFlightRef.current === activeCommunityId) return;
    timelineLoadInFlightRef.current = activeCommunityId;
    try {
      const res = await apiClient.get(`community/blog?communityId=${activeCommunityId}`);
      const timeline = res.data || [];
      setPosts(timeline);

      if (timeline.length === 0) {
        setCommentCountsByPost({});
        return;
      }

      const missingPostIds = timeline
        .map((post: CommunityBlogPost) => post.id)
        .filter((postId: string) => commentCountsByPost[postId] === undefined);

      if (missingPostIds.length > 0) {
        const countRes = await apiClient.get(`community/blog/comments/count?postIds=${missingPostIds.join(",")}`);
        setCommentCountsByPost((current) => ({ ...current, ...(countRes.data || {}) }));
      }
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_blog.load_error"));
    } finally {
      timelineLoadInFlightRef.current = null;
    }
  }, [activeCommunityId, commentCountsByPost, t]);

  useEffect(() => {
    loadPosts();
  }, [loadPosts]);

  useEffect(() => {
    setPublishPermissionReason("");
  }, [activeCommunityId, canPublishByRole]);

  const loadArchive = useCallback(async () => {
    if (!activeCommunityId) return;
    setLoadingArchive(true);
    try {
      const params = new URLSearchParams({ communityId: activeCommunityId });
      if (archiveQuery.trim()) params.set("query", archiveQuery.trim());
      if (archiveDateFrom) params.set("dateFrom", archiveDateFrom);
      if (archiveDateTo) params.set("dateTo", archiveDateTo);
      const res = await apiClient.get(`community/blog/archive?${params.toString()}`);
      setArchivePosts(res.data || []);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_blog.archive_load_error"));
    } finally {
      setLoadingArchive(false);
    }
  }, [activeCommunityId, archiveDateFrom, archiveDateTo, archiveQuery, t]);

  useEffect(() => {
    if (!activeCommunityId) {
      setArchivePosts([]);
      return;
    }
    loadArchive();
  }, [activeCommunityId, loadArchive]);

  const resolvePermissionReason = (error: ApiError) =>
    (error.friendlyMessage || error.response?.data?.message || t("community_blog.permission_reason_fallback")).trim();

  const createPost = async () => {
    if (!canPublish) return;
    setPublishing(true);
    setPublishPermissionReason("");
    try {
      await apiClient.post("community/blog", {
        communityId: activeCommunityId,
        title,
        content: prependImageToContent(content, imageUrl, t("community_blog.image_alt")),
        statusTag,
        pinned,
      });
      setTitle("");
      setContent("");
      setImageUrl("");
      setPinned(false);
      toast.success(t("community_blog.publish_success"));
      loadPosts();
      loadArchive();
    } catch (err) {
      const apiErr = err as ApiError;
      if (apiErr.response?.status === 403) {
        const reason = resolvePermissionReason(apiErr);
        setPublishPermissionReason(reason);
        toast.error(t("community_blog.permission_denied"));
      } else {
        toast.error(apiErr.friendlyMessage || t("community_blog.publish_error"));
      }
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

  const toggleArchive = async (post: CommunityBlogPost, archived: boolean) => {
    setArchivingPostId(post.id);
    try {
      await apiClient.patch(`community/blog/${post.id}/archive`, { archived });
      toast.success(archived ? t("community_blog.archive_success") : t("community_blog.restore_success"));
      await Promise.all([loadPosts(), loadArchive()]);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_blog.archive_error"));
    } finally {
      setArchivingPostId(null);
    }
  };

  const renderPostCard = (post: CommunityBlogPost, archived = false) => {
    const coverImage = extractFirstImageUrl(post.content);
    return (
      <article
        key={post.id}
        className={`glass-panel border-round-3xl overflow-hidden h-full ${post.pinned && !archived ? "border-2 border-brand-primary" : ""}`}
        data-testid={archived ? `archived-blog-post-${post.id}` : `blog-post-${post.id}`}
      >
        {coverImage && (
          <img
            src={coverImage}
            alt={post.title}
            loading="lazy"
            className="w-full"
            style={{ maxHeight: "320px", objectFit: "cover" }}
          />
        )}

        <div className="p-5 md:p-6 flex flex-column gap-4 h-full">
          <header className="u-card-split-header">
            <div className="flex align-items-center gap-3 u-card-copy">
              <Avatar label={post.authorUsername?.[0]?.toUpperCase()} shape="circle" className="bg-brand-primary text-white font-bold" />
              <div className="flex flex-column u-card-copy">
                <span className="text-sm font-bold text-main">{post.authorUsername}</span>
                <span className="text-xs text-muted">{toRoleListLabel(post.authorRole, t)}</span>
              </div>
            </div>
            <div className="u-card-meta-row">
              <CivicBadge label={t("community_blog.official_badge")} severity="progress" />
              {post.pinned && !archived && <CivicBadge label={t("community_blog.pinned_badge")} severity="new" />}
              {archived && <CivicBadge label={t("community_blog.archived_badge")} severity="neutral" />}
              <CivicBadge label={post.statusTag.replace("_", " ")} severity={getStatusSeverity(post.statusTag)} />
            </div>
          </header>

          <div className="u-card-title-wrap">
            <h2 className="text-2xl font-black text-main m-0 mb-3 u-card-title-xl">{post.title}</h2>
            {renderContent(post.content)}
          </div>

          <div className="u-card-meta-row text-xs text-muted border-top-1 border-surface-soft pt-3 mt-auto">
            <i className="pi pi-calendar" />
            <span>{new Date(post.publishedAt).toLocaleDateString()}</span>
            <span>•</span>
            <span>{new Date(post.publishedAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</span>
            {archived && post.archivedAt && (
              <>
                <span>•</span>
                <span>{t("community_blog.archived_on", { date: new Date(post.archivedAt).toLocaleDateString() })}</span>
              </>
            )}
          </div>

          {canPublishByRole && (
            <div className="flex justify-content-end">
              <CivicButton
                type="button"
                size="small"
                variant="ghost"
                icon={archived ? "pi pi-replay" : "pi pi-inbox"}
                label={archived ? t("community_blog.restore_action") : t("community_blog.archive_action")}
                onClick={() => toggleArchive(post, !archived)}
                disabled={archivingPostId === post.id}
                data-testid={`${archived ? "restore" : "archive"}-blog-post-${post.id}`}
              />
            </div>
          )}

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
          <CivicBadge label={t("community_blog.channel_badge")} severity="progress" />
          <CivicButton type="button" icon="pi pi-comments" label="Threads" variant="secondary" onClick={() => navigate("/communities/threads")} />
          <CivicButton type="button" icon="pi pi-bolt" label="Live feed" variant="ghost" onClick={() => navigate("/communities/feed")} />
          {activeCommunityId && !canPublishByRole && (
            <div className="flex flex-column gap-1">
              <span className="text-sm text-muted font-semibold" data-testid="blog-create-permission-note">
                {t("community_blog.permission_note")}
              </span>
              {publishPermissionReason && (
                <small className="p-error text-xs" data-testid="blog-create-permission-reason">
                  {t("community_blog.permission_reason_label", { reason: publishPermissionReason })}
                </small>
              )}
            </div>
          )}
        </CivicActionBar>

        <div className="grid">
          {canPublishByRole && (
            <div className="col-12 lg:col-4">
              <CivicCard id="blog-compose-card" title={t("community_blog.dispatch_title")} variant="brand" fullHeight>
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
                        onKeyDown={(e) => {
                          if (isSubmitShortcut(e) && canPublish && !publishing) {
                            e.preventDefault();
                            createPost();
                          }
                        }}
                        rows={8}
                        className="w-full"
                        placeholder={t("community_blog.context_placeholder")}
                        data-testid="blog-content-input"
                        maxLength={FORM_LIMITS.blog.contentMax}
                      />
                      <small className="text-muted text-xs">{t("community_blog.image_hint")}</small>
                      <small className="text-muted text-xs">{t("community_blog.submit_shortcut_hint")}</small>
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

                  <div className="u-surface-note">
                    <div className="flex align-items-start gap-3">
                      <Checkbox
                        inputId="blog-pinned-checkbox"
                        checked={pinned}
                        onChange={(e) => setPinned(Boolean(e.checked))}
                      />
                      <div className="flex flex-column gap-1">
                        <label htmlFor="blog-pinned-checkbox" className="text-sm font-bold text-main cursor-pointer">
                          {t("community_blog.pin_label")}
                        </label>
                        <small className="text-xs text-muted">{t("community_blog.pin_help")}</small>
                      </div>
                    </div>
                  </div>

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
                  {publishPermissionReason && (
                    <small className="p-error text-xs mt-2" data-testid="blog-create-permission-reason">
                      {t("community_blog.permission_reason_label", { reason: publishPermissionReason })}
                    </small>
                  )}
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
                {pinnedPosts.length > 0 && (
                  <section className="flex flex-column gap-3" data-testid="pinned-announcements-section">
                    <div>
                      <div className="u-eyebrow">{t("community_blog.pinned_section_label")}</div>
                      <h2 className="u-section-title-lg m-0 mt-2">{t("community_blog.pinned_section_title")}</h2>
                    </div>
                    {pinnedPosts.map((post) => renderPostCard(post))}
                  </section>
                )}

                <section className="flex flex-column gap-3" data-testid="official-timeline-section">
                  <div>
                    <div className="u-eyebrow">{t("community_blog.timeline_section_label")}</div>
                    <h2 className="u-section-title-lg m-0 mt-2">{t("community_blog.timeline_section_title")}</h2>
                    <p className="u-section-copy text-sm m-0 mt-2">{t("community_blog.timeline_section_desc")}</p>
                  </div>
                  {timelinePosts.map((post) => renderPostCard(post))}
                </section>

                <section className="flex flex-column gap-3" data-testid="official-archive-section">
                  <CivicCard title={t("community_blog.archive_title")}>
                    <div className="flex flex-column gap-4">
                      <p className="text-sm text-secondary m-0">{t("community_blog.archive_desc")}</p>
                      <div className="grid">
                        <div className="col-12 lg:col-5">
                          <CivicField label={t("community_blog.archive_search_label")}>
                            <InputText
                              value={archiveQuery}
                              onChange={(e) => setArchiveQuery(e.target.value)}
                              placeholder={t("community_blog.archive_search_placeholder")}
                              className="w-full"
                              data-testid="blog-archive-search-input"
                            />
                          </CivicField>
                        </div>
                        <div className="col-12 md:col-6 lg:col-3">
                          <CivicField label={t("community_blog.archive_date_from")}>
                            <InputText
                              type="date"
                              value={archiveDateFrom}
                              onChange={(e) => setArchiveDateFrom(e.target.value)}
                              className="w-full"
                              data-testid="blog-archive-date-from-input"
                            />
                          </CivicField>
                        </div>
                        <div className="col-12 md:col-6 lg:col-3">
                          <CivicField label={t("community_blog.archive_date_to")}>
                            <InputText
                              type="date"
                              value={archiveDateTo}
                              onChange={(e) => setArchiveDateTo(e.target.value)}
                              className="w-full"
                              data-testid="blog-archive-date-to-input"
                            />
                          </CivicField>
                        </div>
                        <div className="col-12 lg:col-1 flex align-items-end">
                          <CivicButton
                            type="button"
                            label={t("community_blog.archive_search_action")}
                            icon="pi pi-search"
                            onClick={loadArchive}
                            className="w-full"
                            data-testid="blog-archive-search-button"
                          />
                        </div>
                      </div>

                      {loadingArchive ? (
                        <p className="text-sm text-secondary m-0">{t("common.loading")}</p>
                      ) : archivePosts.length === 0 ? (
                        <CivicEmptyState
                          icon="pi-inbox"
                          title={t("community_blog.archive_empty_title")}
                          description={t("community_blog.archive_empty_desc")}
                        />
                      ) : (
                        <div className="flex flex-column gap-3">
                          {archivePosts.map((post) => renderPostCard(post, true))}
                        </div>
                      )}
                    </div>
                  </CivicCard>
                </section>
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}
