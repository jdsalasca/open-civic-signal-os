import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputTextarea } from "primereact/inputtextarea";
import { InputText } from "primereact/inputtext";
import { Avatar } from "primereact/avatar";
import { Paginator, PaginatorPageChangeEvent } from "primereact/paginator";
import { useTranslation } from "react-i18next";
import { CommunityMembership, CommunityThread, CommunityThreadMessage, PageResponse, ThreadStatusFilter } from "../types";
import { Layout } from "../components/Layout";
import { useCommunityStore } from "../store/useCommunityStore";
import apiClient from "../api/axios";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicField } from "../components/ui/CivicField";
import { CivicSelect } from "../components/ui/CivicSelect";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";
import { CivicCharacterCount } from "../components/ui/CivicCharacterCount";
import { FORM_LIMITS } from "../constants/formLimits";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";
import { CivicActionBar } from "../components/ui/CivicActionBar";
import { extractFirstImageUrl, prependImageToContent, stripMarkdownImages, isValidImageUrl } from "../utils/communityContent";
import { isSubmitShortcut } from "../utils/keyboard";

type ApiError = Error & {
  friendlyMessage?: string;
  response?: { status?: number; data?: { message?: string } };
};

const REACTION_TYPES = ["👍", "🔥", "🙌", "📍", "👏", "🆘"];

export function CommunityThreads() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { memberships, activeCommunityId, getThreadListState, setThreadListState } = useCommunityStore();
  const [threads, setThreads] = useState<CommunityThread[]>([]);
  const [totalRecords, setTotalRecords] = useState(0);
  const [threadPage, setThreadPage] = useState(0);
  const [threadRows, setThreadRows] = useState(10);
  const [threadStatusFilter, setThreadStatusFilter] = useState<ThreadStatusFilter>("ALL");
  const [targetCommunityId, setTargetCommunityId] = useState<string>("");
  const [newThreadTitle, setNewThreadTitle] = useState("");
  const [threadQuery, setThreadQuery] = useState("");
  const [messageDraftByThread, setMessageDraftByThread] = useState<Record<string, string>>({});
  const [messageImageByThread, setMessageImageByThread] = useState<Record<string, string>>({});
  const [replyTargetByThread, setReplyTargetByThread] = useState<Record<string, CommunityThreadMessage | null>>({});
  const [sendingByThread, setSendingByThread] = useState<Record<string, boolean>>({});
  const [reactingByMessage, setReactingByMessage] = useState<Record<string, boolean>>({});
  const [threadCreatePermissionReason, setThreadCreatePermissionReason] = useState("");

  const threadTitleLength = newThreadTitle.trim().length;

  const activeMembership = useMemo(
    () => memberships.find((m) => m.communityId === activeCommunityId),
    [memberships, activeCommunityId]
  );

  const canModerate = activeMembership?.role === "MODERATOR" || activeMembership?.role === "COORDINATOR";
  const threadFilterOptions = [
    { label: t("community_threads.filter_all"), value: "ALL" },
    { label: t("community_threads.filter_active"), value: "ACTIVE" },
    { label: t("community_threads.filter_stale"), value: "STALE" },
  ];

  useEffect(() => {
    if (!activeCommunityId) return;
    const persisted = getThreadListState(activeCommunityId);
    setThreadPage(persisted.page);
    setThreadRows(persisted.rows);
    setThreadStatusFilter(persisted.status);
  }, [activeCommunityId, getThreadListState]);

  const loadThreads = useCallback(async () => {
    if (!activeCommunityId) return;
    try {
      const statusQuery = threadStatusFilter === "ALL" ? "" : `&status=${threadStatusFilter}`;
      const res = await apiClient.get<PageResponse<CommunityThread>>(
        `community/threads?communityId=${activeCommunityId}&page=${threadPage}&size=${threadRows}${statusQuery}`
      );
      setThreads(res.data?.content || []);
      setTotalRecords(res.data?.totalElements || 0);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_threads.load_error"));
    }
  }, [activeCommunityId, t, threadPage, threadRows, threadStatusFilter]);

  useEffect(() => {
    loadThreads();
  }, [loadThreads]);

  useEffect(() => {
    setThreadCreatePermissionReason("");
  }, [activeCommunityId, targetCommunityId]);

  const resolvePermissionReason = (error: ApiError) =>
    (error.friendlyMessage || error.response?.data?.message || t("community_threads.permission_reason_fallback")).trim();

  useEffect(() => {
    if (!activeCommunityId) return;
    setThreadListState(activeCommunityId, { page: threadPage, rows: threadRows, status: threadStatusFilter });
  }, [activeCommunityId, threadPage, threadRows, threadStatusFilter, setThreadListState]);

  const createThread = async () => {
    if (!activeCommunityId || !targetCommunityId || threadTitleLength < FORM_LIMITS.threads.titleMin) return;
    setThreadCreatePermissionReason("");
    try {
      await apiClient.post("community/threads", {
        sourceCommunityId: activeCommunityId,
        targetCommunityId,
        title: newThreadTitle,
      });
      setNewThreadTitle("");
      setTargetCommunityId("");
      toast.success(t("community_threads.create_success"));
      loadThreads();
    } catch (err) {
      const apiErr = err as ApiError;
      if (apiErr.response?.status === 403) {
        const reason = resolvePermissionReason(apiErr);
        setThreadCreatePermissionReason(reason);
        toast.error(t("community_threads.permission_denied"));
      } else {
        toast.error(apiErr.friendlyMessage || t("community_threads.create_error"));
      }
    }
  };

  const reactToMessage = async (threadId: string, messageId: string, type: string) => {
    const reactionKey = `${threadId}:${messageId}`;
    if (reactingByMessage[reactionKey]) return;
    setReactingByMessage((prev) => ({ ...prev, [reactionKey]: true }));
    try {
      const res = await apiClient.post<CommunityThreadMessage>(`community/threads/${threadId}/messages/${messageId}/react`, { type });
      const nextMessage = res.data;
      setThreads((prev) =>
        prev.map((thread) =>
          thread.id === threadId
            ? {
                ...thread,
                messages: (thread.messages || []).map((message) => (message.id === messageId ? { ...message, ...nextMessage } : message)),
              }
            : thread
        )
      );
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_threads.reaction_error"));
    } finally {
      setReactingByMessage((prev) => ({ ...prev, [reactionKey]: false }));
    }
  };

  const moderateMessage = async (threadId: string, messageId: string, hidden: boolean) => {
    try {
      await apiClient.patch(`community/threads/${threadId}/messages/${messageId}/moderate`, {
        hidden,
        reason: hidden ? t("community_threads.hidden_reason") : t("community_threads.restored_reason"),
      });
      toast.success(t("community_threads.moderation_success"));
      loadThreads();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_threads.moderation_error"));
    }
  };

  const sendMessage = async (threadId: string) => {
    const draft = (messageDraftByThread[threadId] || "").trim();
    const imageUrl = (messageImageByThread[threadId] || "").trim();
    if (!activeCommunityId || draft.length < FORM_LIMITS.threads.messageMin || !isValidImageUrl(imageUrl)) return;

    setSendingByThread((prev) => ({ ...prev, [threadId]: true }));
    try {
      const replyTarget = replyTargetByThread[threadId];
      await apiClient.post(`community/threads/${threadId}/messages`, {
        sourceCommunityId: activeCommunityId,
        content: prependImageToContent(draft, imageUrl, t("community_threads.image_alt")),
        parentMessageId: replyTarget?.id,
      });
      setMessageDraftByThread((prev) => ({ ...prev, [threadId]: "" }));
      setMessageImageByThread((prev) => ({ ...prev, [threadId]: "" }));
      setReplyTargetByThread((prev) => ({ ...prev, [threadId]: null }));
      toast.success(t("community_threads.message_success"));
      loadThreads();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_threads.message_error"));
    } finally {
      setSendingByThread((prev) => ({ ...prev, [threadId]: false }));
    }
  };

  const setReplyTarget = (threadId: string, message: CommunityThreadMessage) => {
    setReplyTargetByThread((prev) => ({ ...prev, [threadId]: message }));
    setMessageDraftByThread((prev) => ({ ...prev, [threadId]: prev[threadId] || `@${message.authorId.slice(0, 4)} ` }));
  };

  const clearReplyTarget = (threadId: string) => {
    setReplyTargetByThread((prev) => ({ ...prev, [threadId]: null }));
  };

  const targetOptions = memberships
    .filter((m: CommunityMembership) => m.communityId !== activeCommunityId)
    .map((m: CommunityMembership) => ({ label: m.communityName, value: m.communityId }));

  const canOpenCrossCommunityThread = Boolean(activeCommunityId && targetOptions.length > 0);
  const canCreateThread = Boolean(activeCommunityId && targetCommunityId && threadTitleLength >= FORM_LIMITS.threads.titleMin);

  const filteredThreads = useMemo(() => {
    const query = threadQuery.trim().toLowerCase();
    if (!query) return threads;
    return threads.filter((thread) => {
      if (thread.title.toLowerCase().includes(query)) return true;
      return (thread.messages || []).some((message) => message.content.toLowerCase().includes(query));
    });
  }, [threadQuery, threads]);

  const onPageChange = (event: PaginatorPageChangeEvent) => {
    setThreadPage(event.page);
    setThreadRows(event.rows);
  };

  const onStatusFilterChange = (value: ThreadStatusFilter) => {
    setThreadStatusFilter(value);
    setThreadPage(0);
  };

  const buildThreadTree = (messages: CommunityThreadMessage[]) => {
    const childrenByParent: Record<string, CommunityThreadMessage[]> = {};
    const roots: CommunityThreadMessage[] = [];

    messages.forEach((message) => {
      if (!message.parentMessageId) {
        roots.push(message);
        return;
      }
      if (!childrenByParent[message.parentMessageId]) {
        childrenByParent[message.parentMessageId] = [];
      }
      childrenByParent[message.parentMessageId].push(message);
    });

    return { roots, childrenByParent };
  };

  const renderMessageNode = (
    thread: CommunityThread,
    message: CommunityThreadMessage,
    childrenByParent: Record<string, CommunityThreadMessage[]>,
    depth = 0
  ): JSX.Element => {
    const children = childrenByParent[message.id] || [];
    const leftPadding = Math.min(depth, 4) * 1.2;

    return (
      <div key={message.id} className="flex flex-column gap-3" style={{ marginLeft: `${leftPadding}rem` }}>
        <div className={`p-4 border-round-2xl border-1 ${depth === 0 ? "bg-surface border-subtle" : "bg-white-alpha-5 border-white-alpha-10"} ${message.hidden ? "opacity-60" : ""}`}>
          <div className="flex justify-content-between align-items-start gap-3 mb-3">
            <div className="flex align-items-center gap-3">
              <Avatar label={depth === 0 ? "OP" : "R"} shape="circle" className="bg-brand-primary text-white font-bold text-xs" />
              <div className="flex flex-column">
                <span className="text-sm font-black text-main">
                  {t("community_threads.member_id", { id: message.authorId.slice(0, 4) })}
                </span>
                <span className="text-min text-muted">{new Date(message.createdAt).toLocaleString()}</span>
              </div>
            </div>
            <div className="flex align-items-center gap-2 flex-wrap justify-content-end">
              <CivicButton
                type="button"
                variant="ghost"
                size="small"
                icon="pi pi-reply"
                label={t("community_threads.reply")}
                onClick={() => setReplyTarget(thread.id, message)}
              />
              {canModerate && (
                <CivicButton
                  type="button"
                  variant="ghost"
                  size="small"
                  icon={message.hidden ? "pi pi-eye" : "pi pi-eye-slash"}
                  label={message.hidden ? t("community_threads.restore") : t("community_threads.hide")}
                  onClick={() => moderateMessage(thread.id, message.id, !message.hidden)}
                />
              )}
            </div>
          </div>

          <p className={`m-0 line-height-3 ${message.hidden ? "text-muted italic" : "text-secondary"}`}>
            {message.hidden
              ? `[${t("community_threads.hidden_label")}: ${message.moderationReason}]`
              : stripMarkdownImages(message.content) || t("community_threads.image_only_message")}
          </p>
          {extractFirstImageUrl(message.content) && !message.hidden && (
            <img
              src={extractFirstImageUrl(message.content) ?? ""}
              alt={t("community_threads.image_alt")}
              className="w-full border-round-xl mt-3 border-1 border-subtle"
              style={{ maxHeight: "18rem", objectFit: "cover" }}
              loading="lazy"
            />
          )}

          <div className="mt-3 flex flex-wrap gap-2">
            {REACTION_TYPES.map((emoji) => (
              <button
                key={`${message.id}-${emoji}`}
                type="button"
                onClick={() => reactToMessage(thread.id, message.id, emoji)}
                aria-label={t("community_threads.react_with", { emoji })}
                disabled={Boolean(reactingByMessage[`${thread.id}:${message.id}`])}
                className={`flex align-items-center gap-2 px-2 py-1 border-round-lg border-1 ${
                  message.viewerReaction === emoji
                    ? "border-brand-primary-alpha-60 bg-brand-primary-alpha-20 shadow-1"
                    : "border-white-alpha-10 bg-black-alpha-20 hover:border-brand-primary-alpha-30"
                }`}
              >
                <span>{emoji}</span>
                <span className="text-xs font-bold text-main">{message.reactions?.[emoji] || 0}</span>
              </button>
            ))}
          </div>
        </div>

        {children.length > 0 && (
          <div className="flex flex-column gap-3">
            {children.map((child) => renderMessageNode(thread, child, childrenByParent, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <CivicPageHeader title={t("community_threads.title")} description={t("community_threads.desc")} />
        <CivicActionBar className="mb-5">
          <CivicButton
            type="button"
            icon="pi pi-plus-circle"
            label="New thread"
            onClick={() => document.getElementById("thread-compose-card")?.scrollIntoView({ behavior: "smooth", block: "start" })}
          />
          <CivicButton type="button" icon="pi pi-megaphone" label="Blog" variant="secondary" onClick={() => navigate("/communities/blog")} />
          <CivicButton type="button" icon="pi pi-bolt" label="Live feed" variant="ghost" onClick={() => navigate("/communities/feed")} />
          <div className="w-full md:w-16rem">
            <CivicSelect
              value={threadStatusFilter}
              options={threadFilterOptions}
              onChange={(e) => onStatusFilterChange(e.value as ThreadStatusFilter)}
              className="w-full"
              placeholder={t("community_threads.filter_label")}
              data-testid="threads-status-filter"
            />
          </div>
          <div className="w-full md:w-20rem">
            <span className="p-input-icon-left w-full">
              <i className="pi pi-search" />
              <InputText
                value={threadQuery}
                onChange={(e) => setThreadQuery(e.target.value)}
                className="w-full"
                placeholder={t("community_threads.search_placeholder")}
                data-testid="threads-search-input"
              />
            </span>
          </div>
        </CivicActionBar>

        {!activeCommunityId && (
          <CivicCard className="mb-6">
            <CivicEmptyState
              icon="pi-map-marker"
              title={t("community_threads.none")}
              description={t("report.community_required")}
              actionLabel={t("nav.communities")}
              onAction={() => navigate("/communities")}
            />
          </CivicCard>
        )}

        <div className="grid">
          <div className="col-12 lg:col-4">
            <CivicCard id="thread-compose-card" title={t("community_threads.channel_title")} className="mb-6" variant="brand">
              <div className="flex flex-column gap-2">
                <CivicField label={t("community_threads.topic")}>
                  <div className="flex flex-column gap-2">
                    <InputText
                      value={newThreadTitle}
                      onChange={(e) => setNewThreadTitle(e.target.value)}
                      placeholder={t("community_threads.topic_placeholder")}
                      className="w-full"
                      data-testid="thread-title-input"
                      maxLength={FORM_LIMITS.threads.titleMax}
                    />
                    <CivicCharacterCount current={newThreadTitle.length} max={FORM_LIMITS.threads.titleMax} min={FORM_LIMITS.threads.titleMin} />
                  </div>
                </CivicField>
                <CivicField label={t("community_threads.target_sector")}>
                  <CivicSelect
                    value={targetCommunityId}
                    options={targetOptions}
                    onChange={(e) => setTargetCommunityId(e.value)}
                    placeholder={t("community_threads.select_community")}
                    className="w-full"
                    disabled={!activeCommunityId || targetOptions.length === 0}
                    emptyMessage={t("community_threads.join_other")}
                    data-testid="thread-target-dropdown"
                  />
                </CivicField>
                {!canOpenCrossCommunityThread && activeCommunityId && (
                  <small className="text-muted text-xs" data-testid="thread-create-permission-note">
                    {t("community_threads.permission_note")}
                  </small>
                )}
                {threadCreatePermissionReason && (
                  <small className="p-error text-xs" data-testid="thread-create-permission-reason">
                    {t("community_threads.permission_reason_label", { reason: threadCreatePermissionReason })}
                  </small>
                )}
                <CivicButton
                  type="button"
                  label={t("community_threads.create")}
                  icon="pi pi-plus-circle"
                  onClick={createThread}
                  disabled={!canCreateThread || !canOpenCrossCommunityThread}
                  className="w-full py-4 mt-2"
                  glow
                  data-testid="create-thread-button"
                />
              </div>
            </CivicCard>
          </div>

          <div className="col-12 lg:col-8">
            <CivicCard title={t("community_threads.feed_title", { community: activeMembership?.communityName || t("community_threads.none") })} padding="none">
              {filteredThreads.length === 0 ? (
                <CivicEmptyState
                  icon="pi-comments"
                  title={threadQuery.trim() ? t("community_threads.search_empty_title") : t("community_threads.empty")}
                  description={threadQuery.trim() ? t("community_threads.search_empty_desc") : t("community_threads.join_other")}
                />
              ) : (
                <>
                  <div className="px-5 pt-4 text-sm text-muted font-semibold">
                    {threadQuery.trim()
                      ? t("community_threads.search_summary", { total: filteredThreads.length })
                      : t("community_threads.page_summary", {
                          from: totalRecords === 0 ? 0 : threadPage * threadRows + 1,
                          to: Math.min((threadPage + 1) * threadRows, totalRecords),
                          total: totalRecords,
                        })}
                  </div>
                  <div className="flex flex-column gap-px bg-white-alpha-10">
                    {filteredThreads.map((thread) => {
                      const { roots, childrenByParent } = buildThreadTree(thread.messages || []);
                      const draft = messageDraftByThread[thread.id] || "";
                      const draftImageUrl = messageImageByThread[thread.id] || "";
                      const hasValidDraftImage = isValidImageUrl(draftImageUrl);
                      const replyTarget = replyTargetByThread[thread.id];
                      const canSend = draft.trim().length >= FORM_LIMITS.threads.messageMin && hasValidDraftImage;

                      return (
                        <div key={thread.id} className="bg-surface p-5 md:p-6 flex flex-column gap-4">
                          <div className="flex justify-content-between align-items-start gap-3">
                            <div>
                              <h3 className="text-xl md:text-2xl font-black text-main m-0 mb-2">{thread.title}</h3>
                              <span className="text-xs text-muted font-bold uppercase tracking-wider">
                                {t("community_threads.link_label")}: {thread.id.substring(0, 8)}
                              </span>
                            </div>
                            <CivicBadge label={t("community_threads.verified_channel")} severity="progress" />
                          </div>

                          <div className="flex flex-column gap-3">
                            {roots.length === 0 ? (
                              <div className="p-4 border-round-xl bg-white-alpha-5 text-muted text-sm">
                                {t("community_threads.no_messages")}
                              </div>
                            ) : (
                              roots.map((root) => renderMessageNode(thread, root, childrenByParent, 0))
                            )}
                          </div>

                          <div className="p-4 border-round-2xl bg-black-alpha-20 border-1 border-white-alpha-10">
                            {replyTarget && (
                              <div className="mb-3 flex align-items-center justify-content-between gap-3 bg-brand-primary-alpha-10 border-1 border-brand-primary-alpha-20 border-round-xl px-3 py-2">
                                <span className="text-sm text-main">
                                  {t("community_threads.replying_to", { id: replyTarget.authorId.slice(0, 4) })}
                                </span>
                                <CivicButton
                                  type="button"
                                  variant="ghost"
                                  size="small"
                                  label={t("community_threads.cancel_reply")}
                                  onClick={() => clearReplyTarget(thread.id)}
                                />
                              </div>
                            )}

                            <div className="flex flex-column gap-2">
                              <InputTextarea
                                value={draft}
                                onChange={(e) => setMessageDraftByThread((prev) => ({ ...prev, [thread.id]: e.target.value }))}
                                onKeyDown={(e) => {
                                  if (isSubmitShortcut(e) && canSend && !sendingByThread[thread.id]) {
                                    e.preventDefault();
                                    sendMessage(thread.id);
                                  }
                                }}
                                rows={3}
                                className="w-full"
                                placeholder={t("community_threads.message_placeholder")}
                                data-testid={`thread-message-input-${thread.id}`}
                                maxLength={FORM_LIMITS.threads.messageMax}
                              />
                              <CivicCharacterCount
                                current={draft.length}
                                max={FORM_LIMITS.threads.messageMax}
                                min={FORM_LIMITS.threads.messageMin}
                              />
                            </div>
                            <div className="flex flex-column gap-2 mt-2">
                              <InputText
                                value={draftImageUrl}
                                onChange={(e) => setMessageImageByThread((prev) => ({ ...prev, [thread.id]: e.target.value }))}
                                className="w-full"
                                placeholder={t("community_threads.image_url_placeholder")}
                                maxLength={1200}
                                data-testid={`thread-image-input-${thread.id}`}
                              />
                              {!hasValidDraftImage ? (
                                <small className="p-error">{t("community_threads.image_url_invalid")}</small>
                              ) : (
                                <small className="text-muted text-xs">{t("community_threads.image_url_help")}</small>
                              )}
                            </div>
                            <small className="text-muted text-xs">{t("community_threads.submit_shortcut_hint")}</small>
                            {draftImageUrl.trim() && hasValidDraftImage && (
                              <div className="border-round-xl overflow-hidden border-1 border-subtle mt-2">
                                <img
                                  src={draftImageUrl.trim()}
                                  alt={t("community_threads.image_alt")}
                                  className="w-full"
                                  style={{ maxHeight: "12rem", objectFit: "cover" }}
                                  loading="lazy"
                                />
                              </div>
                            )}

                            <div className="flex justify-content-end mt-3">
                              <CivicButton
                                type="button"
                                label={t("community_threads.send")}
                                icon="pi pi-send"
                                onClick={() => sendMessage(thread.id)}
                                disabled={!canSend}
                                loading={Boolean(sendingByThread[thread.id])}
                                data-testid={`send-thread-message-button-${thread.id}`}
                              />
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                  <div className="p-4 border-top-1 border-white-alpha-10">
                    <Paginator
                      first={threadPage * threadRows}
                      rows={threadRows}
                      totalRecords={totalRecords}
                      rowsPerPageOptions={[5, 10, 20]}
                      onPageChange={onPageChange}
                      className="justify-content-end"
                      template="RowsPerPageDropdown FirstPageLink PrevPageLink CurrentPageReport NextPageLink LastPageLink"
                      currentPageReportTemplate="{first} - {last} / {totalRecords}"
                    />
                  </div>
                </>
              )}
            </CivicCard>
          </div>
        </div>
      </div>
    </Layout>
  );
}
