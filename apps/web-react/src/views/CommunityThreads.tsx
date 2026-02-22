import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "react-hot-toast";
import { InputTextarea } from "primereact/inputtextarea";
import { InputText } from "primereact/inputtext";
import { Avatar } from "primereact/avatar";
import { useTranslation } from "react-i18next";
import { CommunityMembership, CommunityThread } from "../types";
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

type ApiError = Error & { friendlyMessage?: string };

const REACTION_TYPES = ["👍", "🔥", "🙌", "📍", "👏", "🆘"];

export function CommunityThreads() {
  const { t } = useTranslation();
  const { memberships, activeCommunityId } = useCommunityStore();
  const [threads, setThreads] = useState<CommunityThread[]>([]);
  const [targetCommunityId, setTargetCommunityId] = useState<string>("");
  const [newThreadTitle, setNewThreadTitle] = useState("");
  const [selectedThreadId, setSelectedThreadId] = useState<string>("");
  const [newMessage, setNewMessage] = useState("");
  const threadTitleLength = newThreadTitle.trim().length;
  const messageLength = newMessage.trim().length;

  const activeMembership = useMemo(
    () => memberships.find((m) => m.communityId === activeCommunityId),
    [memberships, activeCommunityId]
  );

  const loadThreads = useCallback(async () => {
    if (!activeCommunityId) return;
    try {
      const res = await apiClient.get(`community/threads?communityId=${activeCommunityId}`);
      setThreads(res.data || []);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('community_threads.load_error'));
    }
  }, [activeCommunityId, t]);

  useEffect(() => {
    loadThreads();
  }, [loadThreads]);

  const createThread = async () => {
    if (!activeCommunityId || !targetCommunityId || threadTitleLength < FORM_LIMITS.threads.titleMin) return;
    try {
      await apiClient.post("community/threads", {
        sourceCommunityId: activeCommunityId,
        targetCommunityId,
        title: newThreadTitle,
      });
      setNewThreadTitle("");
      setTargetCommunityId("");
      toast.success(t('community_threads.create_success'));
      loadThreads();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('community_threads.create_error'));
    }
  };

  const sendMessage = async () => {
    if (!selectedThreadId || !activeCommunityId || messageLength < FORM_LIMITS.threads.messageMin) return;
    try {
      await apiClient.post(`community/threads/${selectedThreadId}/messages`, {
        sourceCommunityId: activeCommunityId,
        content: newMessage,
      });
      setNewMessage("");
      toast.success(t('community_threads.message_success'));
      loadThreads();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('community_threads.message_error'));
    }
  };

  const reactToMessage = async (threadId: string, messageId: string, type: string) => {
    try {
      await apiClient.post(`community/threads/${threadId}/messages/${messageId}/react`, { type });
      loadThreads();
    } catch (err) {
      console.error("Reaction failed", err);
    }
  };

  const moderateMessage = async (threadId: string, messageId: string, hidden: boolean) => {
    try {
      await apiClient.patch(`community/threads/${threadId}/messages/${messageId}/moderate`, {
        hidden,
        reason: hidden ? t('community_threads.hidden_reason') : t('community_threads.restored_reason'),
      });
      toast.success(t('community_threads.moderation_success'));
      loadThreads();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('community_threads.moderation_error'));
    }
  };

  const targetOptions = memberships
    .filter((m: CommunityMembership) => m.communityId !== activeCommunityId)
    .map((m: CommunityMembership) => ({ label: m.communityName, value: m.communityId }));
  
  const canCreateThread = Boolean(activeCommunityId && targetCommunityId && threadTitleLength >= FORM_LIMITS.threads.titleMin);
  const canSendMessage = Boolean(selectedThreadId && activeCommunityId && messageLength >= FORM_LIMITS.threads.messageMin);

  return (
    <Layout>
      <div className="animate-fade-up">
        <CivicPageHeader title={t('community_threads.title')} description={t('community_threads.desc')} />

        <div className="grid">
          <div className="col-12 lg:col-4">
            <CivicCard title={t('community_threads.channel_title')} className="mb-6" variant="brand">
              <div className="flex flex-column gap-2">
                <CivicField label={t('community_threads.topic')}>
                  <div className="flex flex-column gap-2">
                    <InputText
                      value={newThreadTitle}
                      onChange={(e) => setNewThreadTitle(e.target.value)}
                      placeholder={t('community_threads.topic_placeholder')}
                      className="w-full"
                      data-testid="thread-title-input"
                      maxLength={FORM_LIMITS.threads.titleMax}
                    />
                    <CivicCharacterCount current={newThreadTitle.length} max={FORM_LIMITS.threads.titleMax} min={FORM_LIMITS.threads.titleMin} />
                  </div>
                </CivicField>
                <CivicField label={t('community_threads.target_sector')}>
                  <CivicSelect
                    value={targetCommunityId}
                    options={targetOptions}
                    onChange={(e) => setTargetCommunityId(e.value)}
                    placeholder={t('community_threads.select_community')}
                    className="w-full bg-black-alpha-20"
                    disabled={!activeCommunityId || targetOptions.length === 0}
                    emptyMessage={t('community_threads.join_other')}
                    data-testid="thread-target-dropdown"
                  />
                </CivicField>
                <CivicButton
                  label={t('community_threads.create')}
                  icon="pi pi-plus-circle"
                  onClick={createThread}
                  disabled={!canCreateThread}
                  className="w-full py-4 mt-2"
                  glow
                  data-testid="create-thread-button"
                />
              </div>
            </CivicCard>
            
            <CivicCard title={t('community_threads.transmission_title')}>
              <div className="flex flex-column gap-2">
                <CivicField label={t('community_threads.active_thread')}>
                  <CivicSelect
                    value={selectedThreadId}
                    options={threads.map((thread) => ({ label: thread.title, value: thread.id }))}
                    onChange={(e) => setSelectedThreadId(e.value)}
                    placeholder={t('community_threads.select_dialogue')}
                    className="w-full bg-black-alpha-20"
                    disabled={!activeCommunityId || threads.length === 0}
                    data-testid="thread-select-dropdown"
                  />
                </CivicField>
                <CivicField label={t('community_threads.message')}>
                  <div className="flex flex-column gap-2">
                    <InputTextarea
                      value={newMessage}
                      onChange={(e) => setNewMessage(e.target.value)}
                      rows={4}
                      className="w-full"
                      placeholder={t('community_threads.message_placeholder')}
                      disabled={!activeCommunityId || threads.length === 0}
                      data-testid="thread-message-input"
                      maxLength={FORM_LIMITS.threads.messageMax}
                    />
                    <CivicCharacterCount current={newMessage.length} max={FORM_LIMITS.threads.messageMax} min={FORM_LIMITS.threads.messageMin} />
                  </div>
                </CivicField>
                <CivicButton
                  label={t('community_threads.send')}
                  icon="pi pi-send"
                  onClick={sendMessage}
                  disabled={!canSendMessage}
                  className="w-full py-4 mt-2"
                  data-testid="send-thread-message-button"
                />
              </div>
            </CivicCard>
          </div>

          <div className="col-12 lg:col-8">
            <CivicCard title={t('community_threads.feed_title', { community: activeMembership?.communityName || t('community_threads.none') })} padding="none">
              {threads.length === 0 ? (
                <div className="text-center p-8 text-muted">
                  <i className="pi pi-comments text-4xl mb-3 block"></i>
                  {t('community_threads.empty')}
                </div>
              ) : (
                <div className="flex flex-column gap-px bg-white-alpha-10">
                  {threads.map((thread) => (
                    <div key={thread.id} className="bg-surface p-6 hover:bg-white-alpha-5 transition-colors">
                      <div className="flex justify-content-between align-items-center mb-6">
                        <div>
                          <h3 className="text-2xl font-black text-main m-0 tracking-tight leading-none mb-2">{thread.title}</h3>
                          <span className="text-xs text-muted font-bold uppercase tracking-widest">
                            {t('community_threads.link_label')}: {thread.id.substring(0,8)}
                          </span>
                        </div>
                        <div className="flex align-items-center gap-2">
                           <CivicBadge label={t('community_threads.verified_channel')} severity="progress" />
                        </div>
                      </div>

                      <div className="flex flex-column gap-6 pl-4 border-left-2 border-brand-primary-alpha-20">
                        {thread.messages.map((message) => (
                          <div key={message.id} className={`group flex flex-column gap-2 p-5 border-round-3xl transition-all ${message.hidden ? 'bg-status-rejected-alpha-10 opacity-60' : 'bg-white-alpha-5 hover:bg-white-alpha-10 border-1 border-white-alpha-10'}`}>
                            <div className="flex justify-content-between align-items-center mb-2">
                              <div className="flex align-items-center gap-3">
                                <Avatar label="U" shape="circle" className="bg-brand-primary text-white font-bold" />
                                <div className="flex flex-column">
                                  <span className="text-sm font-black text-main uppercase">Identity {message.authorId.substring(0,4)}</span>
                                  <span className="text-min font-bold text-muted uppercase tracking-tighter" style={{fontSize: '8px'}}>{new Date(message.createdAt).toLocaleString()}</span>
                                </div>
                              </div>
                              
                              <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                                {REACTION_TYPES.map(emoji => (
                                  <button 
                                    key={emoji}
                                    type="button"
                                    onClick={() => reactToMessage(thread.id, message.id, emoji)}
                                    aria-label={t('community_threads.react_with', { emoji })}
                                    className="p-2 border-round-lg bg-black-alpha-40 border-none text-lg hover:bg-brand-primary-alpha-20 transition-colors cursor-pointer"
                                  >
                                    {emoji}
                                  </button>
                                ))}
                              </div>
                            </div>

                            <div className={`text-lg font-medium leading-relaxed ${message.hidden ? 'italic text-muted' : 'text-secondary'}`}>
                              {message.hidden ? `[${t('community_threads.hidden_label')}: ${message.moderationReason}]` : message.content}
                            </div>

                            <div className="mt-4 flex flex-wrap gap-2">
                              {Object.entries(message.reactions || {}).map(([emoji, count]) => (
                                <button 
                                  key={emoji} 
                                  type="button"
                                  onClick={() => reactToMessage(thread.id, message.id, emoji)}
                                  aria-label={t('community_threads.react_with', { emoji })}
                                  className="flex align-items-center gap-2 px-3 py-1 bg-brand-primary-alpha-10 border-1 border-brand-primary-alpha-20 border-round-xl hover:bg-brand-primary-alpha-20 transition-colors cursor-pointer"
                                >
                                  <span className="text-sm">{emoji}</span>
                                  <span className="text-xs font-black text-brand-primary">{count}</span>
                                </button>
                              ))}
                            </div>

                            <div className="mt-4 flex justify-content-end gap-2">
                              <CivicButton
                                variant="ghost"
                                size="small"
                                icon={message.hidden ? "pi pi-eye" : "pi pi-eye-slash"}
                                label={message.hidden ? t('community_threads.restore') : t('community_threads.hide')}
                                className="text-min font-black opacity-20 hover:opacity-100"
                                onClick={() => moderateMessage(thread.id, message.id, !message.hidden)}
                              />
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CivicCard>
          </div>
        </div>
      </div>
    </Layout>
  );
}
