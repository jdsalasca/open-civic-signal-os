import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "react-hot-toast";
import { Dropdown } from "primereact/dropdown";
import { InputTextarea } from "primereact/inputtextarea";
import { InputText } from "primereact/inputtext";
import { Avatar } from "primereact/avatar";
import { CommunityMembership, CommunityThread } from "../types";
import { Layout } from "../components/Layout";
import { useCommunityStore } from "../store/useCommunityStore";
import apiClient from "../api/axios";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicField } from "../components/ui/CivicField";

type ApiError = Error & { friendlyMessage?: string };

const REACTION_TYPES = ["👍", "🔥", "🙌", "📍", "👏", "🆘"];

export function CommunityThreads() {
  const { memberships, activeCommunityId } = useCommunityStore();
  const [threads, setThreads] = useState<CommunityThread[]>([]);
  const [targetCommunityId, setTargetCommunityId] = useState<string>("");
  const [newThreadTitle, setNewThreadTitle] = useState("");
  const [selectedThreadId, setSelectedThreadId] = useState<string>("");
  const [newMessage, setNewMessage] = useState("");

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
      toast.error(apiErr.friendlyMessage || "Failed to load threads");
    }
  }, [activeCommunityId]);

  useEffect(() => {
    loadThreads();
  }, [loadThreads]);

  const createThread = async () => {
    if (!activeCommunityId || !targetCommunityId || !newThreadTitle.trim()) return;
    try {
      await apiClient.post("community/threads", {
        sourceCommunityId: activeCommunityId,
        targetCommunityId,
        title: newThreadTitle,
      });
      setNewThreadTitle("");
      setTargetCommunityId("");
      toast.success("Dialogue established");
      loadThreads();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || "Failed to create thread");
    }
  };

  const sendMessage = async () => {
    if (!selectedThreadId || !activeCommunityId || !newMessage.trim()) return;
    try {
      await apiClient.post(`community/threads/${selectedThreadId}/messages`, {
        sourceCommunityId: activeCommunityId,
        content: newMessage,
      });
      setNewMessage("");
      toast.success("Intelligence shared");
      loadThreads();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || "Failed to send message");
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
        reason: hidden ? "Hidden by protocol" : "Restored by protocol",
      });
      toast.success("Security status updated");
      loadThreads();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || "Moderation failed");
    }
  };

  const targetOptions = memberships
    .filter((m: CommunityMembership) => m.communityId !== activeCommunityId)
    .map((m: CommunityMembership) => ({ label: m.communityName, value: m.communityId }));
  
  const canCreateThread = Boolean(activeCommunityId && targetCommunityId && newThreadTitle.trim());
  const canSendMessage = Boolean(selectedThreadId && activeCommunityId && newMessage.trim());

  return (
    <Layout>
      <div className="animate-fade-up">
        <div className="mb-8">
          <h1 className="text-5xl font-black mb-2 text-main tracking-tighter">Strategic Dialogues</h1>
          <p className="text-secondary text-lg font-medium">Real-time cross-community coordination and engagement.</p>
        </div>

        <div className="grid">
          <div className="col-12 lg:col-4">
            <CivicCard title="Establish Channel" className="mb-6" variant="brand">
              <div className="flex flex-column gap-2">
                <CivicField label="Dialogue Topic">
                  <InputText
                    value={newThreadTitle}
                    onChange={(e) => setNewThreadTitle(e.target.value)}
                    placeholder="e.g. Joint Security Patrol"
                    className="w-full"
                    data-testid="thread-title-input"
                  />
                </CivicField>
                <CivicField label="Target Sector">
                  <Dropdown
                    value={targetCommunityId}
                    options={targetOptions}
                    onChange={(e) => setTargetCommunityId(e.value)}
                    placeholder="Select community"
                    className="w-full bg-black-alpha-20"
                    disabled={!activeCommunityId || targetOptions.length === 0}
                    emptyMessage="Join other communities to sync"
                    data-testid="thread-target-dropdown"
                  />
                </CivicField>
                <CivicButton
                  label="Initialize Thread"
                  icon="pi pi-plus-circle"
                  onClick={createThread}
                  disabled={!canCreateThread}
                  className="w-full py-4 mt-2"
                  glow
                  data-testid="create-thread-button"
                />
              </div>
            </CivicCard>
            
            <CivicCard title="Transmission Unit">
              <div className="flex flex-column gap-2">
                <CivicField label="Active Thread">
                  <Dropdown
                    value={selectedThreadId}
                    options={threads.map((thread) => ({ label: thread.title, value: thread.id }))}
                    onChange={(e) => setSelectedThreadId(e.value)}
                    placeholder="Select dialogue"
                    className="w-full bg-black-alpha-20"
                    disabled={!activeCommunityId || threads.length === 0}
                    data-testid="thread-select-dropdown"
                  />
                </CivicField>
                <CivicField label="Message Data">
                  <InputTextarea
                    value={newMessage}
                    onChange={(e) => setNewMessage(e.target.value)}
                    rows={4}
                    className="w-full"
                    placeholder="Compose intelligence dispatch..."
                    disabled={!activeCommunityId || threads.length === 0}
                    data-testid="thread-message-input"
                  />
                </CivicField>
                <CivicButton
                  label="Dispatch Signal"
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
            <CivicCard title={`Operational Feed: ${activeMembership?.communityName || 'None'}`} padding="none">
              {threads.length === 0 ? (
                <div className="text-center p-8 text-muted">
                  <i className="pi pi-comments text-4xl mb-3 block"></i>
                  No active strategic threads.
                </div>
              ) : (
                <div className="flex flex-column gap-px bg-white-alpha-10">
                  {threads.map((thread) => (
                    <div key={thread.id} className="bg-surface p-6 hover:bg-white-alpha-5 transition-colors">
                      <div className="flex justify-content-between align-items-center mb-6">
                        <div>
                          <h3 className="text-2xl font-black text-main m-0 tracking-tight leading-none mb-2">{thread.title}</h3>
                          <span className="text-xs text-muted font-bold uppercase tracking-widest">
                            Intelligence Link: {thread.id.substring(0,8)}
                          </span>
                        </div>
                        <div className="flex align-items-center gap-2">
                           <CivicBadge label="Verified Channel" severity="progress" />
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
                                    onClick={() => reactToMessage(thread.id, message.id, emoji)}
                                    className="p-2 border-round-lg bg-black-alpha-40 border-none text-lg hover:bg-brand-primary-alpha-20 transition-colors cursor-pointer"
                                  >
                                    {emoji}
                                  </button>
                                ))}
                              </div>
                            </div>

                            <div className={`text-lg font-medium leading-relaxed ${message.hidden ? 'italic text-muted' : 'text-secondary'}`}>
                              {message.hidden ? `[Security protocol: Message hidden. Reason: ${message.moderationReason}]` : message.content}
                            </div>

                            <div className="mt-4 flex flex-wrap gap-2">
                              {Object.entries(message.reactions || {}).map(([emoji, count]) => (
                                <button 
                                  key={emoji} 
                                  onClick={() => reactToMessage(thread.id, message.id, emoji)}
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
                                label={message.hidden ? "Restore" : "Protocol Hide"}
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
