import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "react-hot-toast";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { Dropdown } from "primereact/dropdown";
import { InputTextarea } from "primereact/inputtextarea";
import { InputText } from "primereact/inputtext";
import { CommunityMembership, CommunityThread } from "../types";
import { Layout } from "../components/Layout";
import { useCommunityStore } from "../store/useCommunityStore";
import apiClient from "../api/axios";

type ApiError = Error & { friendlyMessage?: string };

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
      toast.success("Thread created");
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
      toast.success("Message sent");
      loadThreads();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || "Failed to send message");
    }
  };

  const moderateMessage = async (threadId: string, messageId: string, hidden: boolean) => {
    try {
      await apiClient.patch(`community/threads/${threadId}/messages/${messageId}/moderate`, {
        hidden,
        reason: hidden ? "Hidden by moderator" : "Restored by moderator",
      });
      toast.success("Message moderation updated");
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
      <div className="grid">
        <div className="col-12 lg:col-4">
                    <Card title={<span className="uppercase text-sm font-bold tracking-widest text-cyan-500">Open Thread</span>} className="mb-4 shadow-4 border-1 border-white-alpha-10 bg-surface">
                      <div className="flex flex-column gap-3">
                        <div>
                          <label className="text-xs font-bold uppercase text-muted mb-1 block">Thread Topic</label>
                          <InputText
                            value={newThreadTitle}
                            onChange={(e) => setNewThreadTitle(e.target.value)}
                            placeholder="e.g. Infrastructure Coordination"
                            className="w-full"
                          />
                        </div>
                        <div>
                          <label className="text-xs font-bold uppercase text-muted mb-1 block">Target Community</label>
                          <Dropdown
                            value={targetCommunityId}
                            options={targetOptions}
                            onChange={(e) => setTargetCommunityId(e.value)}
                            placeholder="Select partner community"
                            className="w-full"
                            disabled={!activeCommunityId || targetOptions.length === 0}
                            emptyMessage="No other communities joined"
                          />
                        </div>
                        <Button
                          label="Create Thread"
                          icon="pi pi-comments"
                          onClick={createThread}
                          disabled={!canCreateThread}
                          className="w-full p-button-primary"
                          data-testid="create-thread-button"
                        />
                      </div>
                      {!activeCommunityId && (
                        <div className="mt-3 p-2 border-round bg-yellow-900-alpha-20 text-yellow-200 text-xs">
                          Select a community context to enable thread actions.
                        </div>
                      )}
                      {activeCommunityId && targetOptions.length === 0 && (
                        <div className="mt-3 p-2 border-round bg-blue-900-alpha-20 text-blue-200 text-xs">
                          Join at least one additional community to open cross-community threads.
                        </div>
                      )}
                    </Card>
                    
                    <Card title={<span className="uppercase text-sm font-bold tracking-widest text-cyan-500">Post Message</span>} className="shadow-4 border-1 border-white-alpha-10 bg-surface">
                      <div className="flex flex-column gap-3">
                        <div>
                          <label className="text-xs font-bold uppercase text-muted mb-1 block">Select Context</label>
                          <Dropdown
                            value={selectedThreadId}
                            options={threads.map((thread) => ({ label: thread.title, value: thread.id }))}
                            onChange={(e) => setSelectedThreadId(e.value)}
                            placeholder="Choose active thread"
                            className="w-full"
                            disabled={!activeCommunityId || threads.length === 0}
                            emptyMessage="No threads active"
                          />
                        </div>
                        <div>
                          <label className="text-xs font-bold uppercase text-muted mb-1 block">Message Content</label>
                          <InputTextarea
                            value={newMessage}
                            onChange={(e) => setNewMessage(e.target.value)}
                            rows={4}
                            className="w-full"
                            placeholder="Type your message here..."
                            disabled={!activeCommunityId || threads.length === 0}
                          />
                        </div>
                        <Button
                          label="Send Message"
                          icon="pi pi-send"
                          onClick={sendMessage}
                          disabled={!canSendMessage}
                          className="w-full"
                          data-testid="send-thread-message-button"
                        />
                      </div>
                      {threads.length === 0 && activeCommunityId && (
                        <div className="mt-3 p-2 border-round bg-gray-800 text-gray-400 text-xs">
                          Create a thread first to start the conversation.
                        </div>
                      )}
                    </Card>
        </div>
        <div className="col-12 lg:col-8">
          <Card title={`Thread Timeline (${activeMembership?.communityName ?? "No community"})`}>
            <div className="flex flex-column gap-4">
              {threads.map((thread) => (
                <div key={thread.id} className="border-1 border-round p-3 border-300">
                  <div className="font-bold text-lg mb-2">{thread.title}</div>
                  <div className="text-sm text-color-secondary mb-3">
                    Updated: {new Date(thread.updatedAt).toLocaleString()}
                  </div>
                  <div className="flex flex-column gap-2">
                    {thread.messages.map((message) => (
                      <div key={message.id} className="surface-100 border-round p-2">
                        <div>{message.hidden ? "[Hidden message]" : message.content}</div>
                        <div className="text-xs text-color-secondary">
                          {new Date(message.createdAt).toLocaleString()}
                        </div>
                        <div className="mt-2 flex gap-2">
                          <Button
                            size="small"
                            outlined
                            label={message.hidden ? "Unhide" : "Hide"}
                            onClick={() => moderateMessage(thread.id, message.id, !message.hidden)}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </Card>
        </div>
      </div>
    </Layout>
  );
}
