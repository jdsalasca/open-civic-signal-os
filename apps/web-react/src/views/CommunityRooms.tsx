import { useCallback, useEffect, useMemo, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { formatStamp } from "../utils/formatStamp";
import { Layout } from "../components/Layout";
import { CivicActionBar } from "../components/ui/CivicActionBar";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicCharacterCount } from "../components/ui/CivicCharacterCount";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";
import { CivicField } from "../components/ui/CivicField";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";
import { CivicStatCard } from "../components/ui/CivicStatCard";
import { useAuthStore } from "../store/useAuthStore";
import { useCommunityStore } from "../store/useCommunityStore";
import type {
  CommunityPermissionPolicy,
  CommunityRoomDetail,
  CommunityRoomWorkspace,
} from "../types";

type ApiError = Error & { friendlyMessage?: string };

const MESSAGE_MAX = 2000;
/**
 * Matches the API's own default so the first request asks for exactly what it would have sent
 * anyway, and older messages are loaded in steps rather than all at once.
 */
const ROOM_PAGE_SIZE = 50;
const ROOM_PAGE_STEP = 50;
const ROOM_PAGE_MAX = 200;

type MessageForm = { body: string };
type RoomForm = { name: string; topic: string };

const defaultMessageValues: MessageForm = { body: "" };
const defaultRoomValues: RoomForm = { name: "", topic: "" };

export function CommunityRooms() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const accessToken = useAuthStore((state) => state.accessToken);
  const { activeCommunityId, memberships } = useCommunityStore();
  const activeMembership = memberships.find((m) => m.communityId === activeCommunityId) ?? null;

  const [workspace, setWorkspace] = useState<CommunityRoomWorkspace | null>(null);
  const [policies, setPolicies] = useState<CommunityPermissionPolicy[]>([]);
  const [activeRoomId, setActiveRoomId] = useState<string | null>(null);
  const [room, setRoom] = useState<CommunityRoomDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadingRoom, setLoadingRoom] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [creating, setCreating] = useState(false);
  const [muting, setMuting] = useState(false);
  const [live, setLive] = useState(false);

  const {
    control: messageControl,
    handleSubmit: submitMessage,
    reset: resetMessage,
    watch: watchMessage,
    formState: { errors: messageErrors },
  } = useForm<MessageForm>({ mode: "onChange", defaultValues: defaultMessageValues });

  const {
    control: roomControl,
    handleSubmit: submitRoom,
    reset: resetRoom,
    formState: { errors: roomErrors },
  } = useForm<RoomForm>({ mode: "onChange", defaultValues: defaultRoomValues });

  const watchedBody = watchMessage("body") ?? "";

  const managePolicy = useMemo(
    () => policies.find((policy) => policy.scope === "MANAGE_ROOMS"),
    [policies]
  );
  const postPolicy = useMemo(
    () => policies.find((policy) => policy.scope === "POST_ROOM_MESSAGE"),
    [policies]
  );
  const canManageRooms = Boolean(
    activeMembership && managePolicy?.allowedRoles.includes(activeMembership.role)
  );
  const canPost = Boolean(
    activeMembership && postPolicy?.allowedRoles.includes(activeMembership.role)
  );

  const loadPolicies = useCallback(async () => {
    if (!activeCommunityId) {
      setPolicies([]);
      return;
    }
    try {
      const response = await apiClient.get<CommunityPermissionPolicy[]>(
        `communities/${activeCommunityId}/permissions`
      );
      setPolicies(response.data ?? []);
    } catch {
      setPolicies([]);
    }
  }, [activeCommunityId]);

  const loadWorkspace = useCallback(async () => {
    if (!activeCommunityId) {
      setWorkspace(null);
      return;
    }
    setLoading(true);
    try {
      const response = await apiClient.get<CommunityRoomWorkspace>(
        `community/rooms/workspace?communityId=${activeCommunityId}`
      );
      setWorkspace(response.data);
      setActiveRoomId((current) => current ?? response.data.rooms[0]?.id ?? null);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_rooms.load_error"));
      setWorkspace(null);
    } finally {
      setLoading(false);
    }
  }, [activeCommunityId, t]);

  const [messageLimit, setMessageLimit] = useState(ROOM_PAGE_SIZE);

  const loadRoom = useCallback(async () => {
    if (!activeCommunityId || !activeRoomId) {
      setRoom(null);
      return;
    }
    setLoadingRoom(true);
    try {
      // The limit is raised when a reader asks for older messages, so the screen is bounded on a
      // phone without being a dead end for anyone who wants the rest of the conversation.
      const response = await apiClient.get<CommunityRoomDetail>(
        `community/rooms/${activeRoomId}?communityId=${activeCommunityId}&limit=${messageLimit}`,
      );
      setRoom(response.data);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_rooms.load_error"));
      setRoom(null);
    } finally {
      setLoadingRoom(false);
    }
  }, [activeCommunityId, activeRoomId, messageLimit, t]);

  useEffect(() => {
    loadPolicies();
  }, [loadPolicies]);

  useEffect(() => {
    loadWorkspace();
  }, [loadWorkspace]);

  useEffect(() => {
    loadRoom();
  }, [loadRoom]);

  useEffect(() => {
    if (!activeCommunityId || !activeRoomId || !accessToken) {
      setLive(false);
      return;
    }
    const base = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, "") ?? "/api";
    const controller = new AbortController();

    (async () => {
      try {
        const response = await fetch(
          `${base}/community/rooms/${activeRoomId}/stream?communityId=${activeCommunityId}`,
          { headers: { Authorization: `Bearer ${accessToken}`, Accept: "text/event-stream" }, signal: controller.signal }
        );
        if (!response.ok || !response.body) {
          return;
        }
        setLive(true);
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        for (;;) {
          const { done, value } = await reader.read();
          if (done) break;
          if (decoder.decode(value).includes("room-message")) {
            loadRoom();
            loadWorkspace();
          }
        }
      } catch {
        // Stream closed by navigation or network drop; polling state stays truthful.
      } finally {
        setLive(false);
      }
    })();

    return () => controller.abort();
  }, [accessToken, activeCommunityId, activeRoomId, loadRoom, loadWorkspace]);

  const onSendMessage = async (values: MessageForm) => {
    if (!activeCommunityId || !activeRoomId) {
      return;
    }
    setSubmitting(true);
    try {
      await apiClient.post("community/rooms/messages", {
        communityId: activeCommunityId,
        roomId: activeRoomId,
        body: values.body.trim(),
      });
      toast.success(t("community_rooms.compose_success"));
      resetMessage(defaultMessageValues);
      await loadRoom();
      await loadWorkspace();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_rooms.compose_error"));
    } finally {
      setSubmitting(false);
    }
  };

  const onCreateRoom = async (values: RoomForm) => {
    if (!activeCommunityId) {
      return;
    }
    setCreating(true);
    try {
      const response = await apiClient.post<CommunityRoomWorkspace>("community/rooms", {
        communityId: activeCommunityId,
        name: values.name.trim(),
        topic: values.topic.trim(),
      });
      setWorkspace(response.data);
      setActiveRoomId(response.data.rooms[0]?.id ?? null);
      toast.success(t("community_rooms.create_success"));
      resetRoom(defaultRoomValues);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_rooms.create_error"));
    } finally {
      setCreating(false);
    }
  };

  const toggleMute = async () => {
    if (!activeCommunityId || !activeRoomId || !room) {
      return;
    }
    setMuting(true);
    try {
      await apiClient.patch(
        `community/rooms/${activeRoomId}/mute?communityId=${activeCommunityId}&muted=${!room.muted}`
      );
      toast.success(t("community_rooms.mute_success"));
      await loadRoom();
      await loadWorkspace();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_rooms.mute_error"));
    } finally {
      setMuting(false);
    }
  };

  const markMentionsRead = async () => {
    if (!activeCommunityId) {
      return;
    }
    try {
      await apiClient.patch(`community/rooms/mentions/read?communityId=${activeCommunityId}`);
      toast.success(t("community_rooms.mark_read_success"));
      await loadWorkspace();
      await loadRoom();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t("community_rooms.mark_read_error"));
    }
  };

  const formatDateTime = (value?: string | null) => formatStamp(value) ?? "-";

  if (!activeCommunityId || !activeMembership) {
    return (
      <Layout>
        <CivicCard>
          <CivicEmptyState
            icon="pi pi-comments"
            title={t("community_rooms.no_context_title")}
            description={t("community_rooms.no_context_desc")}
            actionLabel={t("nav.communities")}
            onAction={() => navigate("/communities")}
          />
        </CivicCard>
      </Layout>
    );
  }

  const rooms = workspace?.rooms ?? [];
  const unreadTotal = workspace?.mentionInbox.unreadTotal ?? 0;
  const mutedCount = rooms.filter((item) => item.muted).length;
  const totalMessages = rooms.reduce((sum, item) => sum + item.messageCount, 0);

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <div className="flex flex-column xl:flex-row justify-content-between align-items-start gap-4 mb-8">
          <CivicPageHeader
            title={t("community_rooms.title")}
            description={t("community_rooms.desc", { community: activeMembership.communityName })}
            className="mb-0"
          />
          <CivicActionBar className="w-full xl:w-auto">
            <div className="community-home-action-copy">
              <div className="u-eyebrow">{t("community_rooms.kicker")}</div>
              <p className="u-section-copy text-sm m-0">{t("community_rooms.kicker_desc")}</p>
            </div>
            <div className="dashboard-action-cluster">
              <CivicButton
                type="button"
                icon="pi pi-refresh"
                label={t("community_rooms.refresh")}
                variant="secondary"
                onClick={loadWorkspace}
                data-testid="community-rooms-refresh"
              />
            </div>
          </CivicActionBar>
        </div>

        <div className="civic-stat-grid civic-stat-grid-comfortable" data-testid="community-rooms-stats-grid">
          <CivicStatCard compact label={t("community_rooms.rooms_title")} value={rooms.length} supportingText={t("community_rooms.stats_support")} />
          <CivicStatCard compact label={t("community_rooms.messages")} value={totalMessages} supportingText={t("community_rooms.stats_support")} />
          <CivicStatCard compact label={t("community_rooms.unread_mentions")} value={unreadTotal} supportingText={t("community_rooms.stats_support")} />
          <CivicStatCard compact label={t("community_rooms.muted_rooms")} value={mutedCount} supportingText={t("community_rooms.stats_support")} />
        </div>

        <div className="grid mt-6">
          <div className="col-12 xl:col-4 flex flex-column gap-6">
            <CivicCard title={t("community_rooms.rooms_title")} data-testid="community-rooms-list-card">
              {loading && rooms.length === 0 ? (
                <p className="text-secondary m-0">{t("common.loading")}</p>
              ) : rooms.length === 0 ? (
                <CivicEmptyState
                  icon="pi pi-comments"
                  title={t("community_rooms.empty_title")}
                  description={t("community_rooms.empty_desc")}
                />
              ) : (
                <div className="flex flex-column gap-3">
                  {rooms.map((item) => (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => setActiveRoomId(item.id)}
                      data-testid={`community-rooms-room-${item.id}`}
                      aria-current={item.id === activeRoomId}
                      className={`border-round-xl border-1 p-4 text-left w-full ${
                        item.id === activeRoomId
                          ? "border-brand-primary bg-surface-soft"
                          : "border-surface-soft bg-transparent"
                      }`}
                    >
                      <div className="flex align-items-center gap-2 flex-wrap">
                        <span className="font-black text-main">{item.name}</span>
                        {item.muted && <CivicBadge label={t("community_rooms.muted_rooms")} severity="neutral" />}
                        {item.unreadMentionCount > 0 && (
                          <CivicBadge label={String(item.unreadMentionCount)} severity="new" />
                        )}
                      </div>
                      <p className="text-sm text-secondary mt-2 mb-0 line-height-3">{item.topic}</p>
                      <small className="text-muted">{formatDateTime(item.lastActivityAt)}</small>
                    </button>
                  ))}
                </div>
              )}
            </CivicCard>

            {canManageRooms && (
              <CivicCard title={t("community_rooms.create_title")} data-testid="community-rooms-create-card">
                <form className="flex flex-column gap-2" onSubmit={submitRoom(onCreateRoom)}>
                  <CivicField label={t("community_rooms.create_name_label")} error={roomErrors.name?.message}>
                    <Controller
                      name="name"
                      control={roomControl}
                      rules={{
                        required: t("community_rooms.name_required"),
                        minLength: { value: 3, message: t("community_rooms.name_too_short") },
                        maxLength: { value: 120, message: t("community_rooms.name_too_long") },
                      }}
                      render={({ field }) => (
                        <InputText
                          {...field}
                          value={field.value ?? ""}
                          onChange={(e) => field.onChange(e.target.value)}
                          className="w-full"
                          placeholder={t("community_rooms.create_name_placeholder")}
                          data-testid="community-rooms-name"
                        />
                      )}
                    />
                  </CivicField>
                  <CivicField label={t("community_rooms.create_topic_label")} error={roomErrors.topic?.message}>
                    <Controller
                      name="topic"
                      control={roomControl}
                      rules={{
                        required: t("community_rooms.topic_required"),
                        minLength: { value: 3, message: t("community_rooms.topic_too_short") },
                        maxLength: { value: 280, message: t("community_rooms.topic_too_long") },
                      }}
                      render={({ field }) => (
                        <InputText
                          {...field}
                          value={field.value ?? ""}
                          onChange={(e) => field.onChange(e.target.value)}
                          className="w-full"
                          placeholder={t("community_rooms.create_topic_placeholder")}
                          data-testid="community-rooms-topic"
                        />
                      )}
                    />
                  </CivicField>
                  <CivicButton
                    type="submit"
                    icon="pi pi-plus"
                    label={t("community_rooms.create_submit")}
                    loading={creating}
                    data-testid="community-rooms-create-submit"
                  />
                </form>
              </CivicCard>
            )}
          </div>

          <div className="col-12 xl:col-8 flex flex-column gap-6">
            <CivicCard title={t("community_rooms.mention_inbox_title")} data-testid="community-rooms-mention-inbox">
              {unreadTotal === 0 ? (
                <p className="text-secondary m-0">{t("community_rooms.mention_inbox_empty")}</p>
              ) : (
                <div className="flex flex-column gap-3">
                  {workspace?.mentionInbox.items.map((mention) => (
                    <div key={mention.id} className="border-round-xl border-1 border-surface-soft p-3">
                      <div className="flex align-items-center gap-2 flex-wrap">
                        <span className="font-black text-main">{mention.roomName}</span>
                        <span className="text-xs text-muted">{formatDateTime(mention.createdAt)}</span>
                      </div>
                      <p className="text-sm text-secondary mt-2 mb-0 line-height-3">{mention.messagePreview}</p>
                      <small className="text-muted">
                        {t("community_rooms.mention_inbox_title")}: {mention.mentionedByName}
                      </small>
                    </div>
                  ))}
                  <CivicButton
                    type="button"
                    icon="pi pi-check"
                    label={t("community_rooms.mark_read")}
                    variant="secondary"
                    onClick={markMentionsRead}
                    data-testid="community-rooms-mark-read"
                  />
                </div>
              )}
            </CivicCard>

            <CivicCard
              title={room ? room.name : t("community_rooms.room_messages_title")}
              data-testid="community-rooms-messages-card"
            >
              {loadingRoom ? (
                <p className="text-secondary m-0">{t("common.loading")}</p>
              ) : !room ? (
                <CivicEmptyState
                  icon="pi pi-comments"
                  title={t("community_rooms.room_messages_empty_title")}
                  description={t("community_rooms.room_messages_empty_desc")}
                />
              ) : (
                <div className="flex flex-column gap-4">
                  <div className="flex align-items-center gap-2 flex-wrap">
                    <CivicBadge
                      label={live ? t("community_rooms.connected") : t("community_rooms.disconnected")}
                      severity={live ? "resolved" : "neutral"}
                    />
                    <CivicButton
                      type="button"
                      icon={room.muted ? "pi pi-volume-up" : "pi pi-volume-off"}
                      label={room.muted ? t("community_rooms.mute_off") : t("community_rooms.mute_on")}
                      variant="ghost"
                      loading={muting}
                      onClick={toggleMute}
                      data-testid="community-rooms-mute-toggle"
                    />
                  </div>

                  {room.messages.length === 0 ? (
                    <CivicEmptyState
                      icon="pi pi-comment"
                      title={t("community_rooms.room_messages_empty_title")}
                      description={t("community_rooms.room_messages_empty_desc")}
                    />
                  ) : (
                    <>
                      {/*
                        The API bounds a room read and says so. Rendering the page as though it were
                        the room would leave a coordinator deciding whether a group is still active
                        with the wrong answer: "nothing since March" and "we fetched the last 50" are
                        different facts.
                      */}
                      {room.hasMoreMessages && (
                        <div
                          className="flex align-items-center justify-content-between gap-3 flex-wrap text-sm text-secondary"
                          data-testid="community-rooms-history-note"
                        >
                          <span>
                            {t("community_rooms.history_truncated", {
                              shown: room.messages.length,
                              total: room.messageCount,
                            })}
                          </span>
                          {messageLimit < ROOM_PAGE_MAX && (
                            <CivicButton
                              type="button"
                              variant="ghost"
                              onClick={() => setMessageLimit((limit) => Math.min(limit + ROOM_PAGE_STEP, ROOM_PAGE_MAX))}
                              data-testid="community-rooms-load-older"
                            >
                              {t("community_rooms.load_older")}
                            </CivicButton>
                          )}
                        </div>
                      )}
                      <div className="flex flex-column gap-3" data-testid="community-rooms-message-list">
                        {room.messages.map((message) => (
                          <div
                            key={message.id}
                            className={`border-round-xl border-1 p-3 ${
                              message.mentionsCurrentUser ? "border-brand-primary" : "border-surface-soft"
                            }`}
                            data-testid={`community-rooms-message-${message.id}`}
                          >
                            <div className="flex align-items-center gap-2 flex-wrap">
                              <span className="font-black text-main">{message.authorName}</span>
                              <span className="text-xs text-muted">{formatDateTime(message.createdAt)}</span>
                              {message.mentionsCurrentUser && (
                                <CivicBadge label={t("community_rooms.unread_mentions")} severity="new" />
                              )}
                            </div>
                            <p className="text-sm text-secondary mt-2 mb-0 line-height-3">{message.body}</p>
                          </div>
                        ))}
                      </div>
                    </>
                  )}

                  {canPost && (
                    <form className="flex flex-column gap-2" onSubmit={submitMessage(onSendMessage)}>
                      <CivicField
                        label={t("community_rooms.compose_label")}
                        helpText={t("community_rooms.compose_help", { max: MESSAGE_MAX })}
                        error={messageErrors.body?.message}
                      >
                        <Controller
                          name="body"
                          control={messageControl}
                          rules={{
                            required: t("community_rooms.empty_body_error"),
                            maxLength: { value: MESSAGE_MAX, message: t("community_rooms.compose_help", { max: MESSAGE_MAX }) },
                          }}
                          render={({ field }) => (
                            <InputTextarea
                              {...field}
                              value={field.value ?? ""}
                              onChange={(e) => field.onChange(e.target.value)}
                              className="w-full"
                              rows={3}
                              placeholder={t("community_rooms.compose_placeholder")}
                              data-testid="community-rooms-message-input"
                            />
                          )}
                        />
                      </CivicField>
                      <div className="flex justify-content-between align-items-center gap-2 flex-wrap">
                        <CivicCharacterCount current={watchedBody.length} max={MESSAGE_MAX} min={1} />
                        {workspace && workspace.mentionableUsernames.length > 0 && (
                          <small className="text-muted">
                            {t("community_rooms.mentionable_hint", {
                              handles: workspace.mentionableUsernames.join(", "),
                            })}
                          </small>
                        )}
                      </div>
                      <CivicButton
                        type="submit"
                        icon="pi pi-send"
                        label={t("community_rooms.compose_submit")}
                        loading={submitting}
                        data-testid="community-rooms-send"
                      />
                    </form>
                  )}
                </div>
              )}
            </CivicCard>
          </div>
        </div>
      </div>
    </Layout>
  );
}