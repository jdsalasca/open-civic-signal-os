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
          <Card title="Open Thread" className="mb-4">
            <InputText
              value={newThreadTitle}
              onChange={(e) => setNewThreadTitle(e.target.value)}
              placeholder="Thread title"
              className="w-full mb-2"
            />
            <Dropdown
              value={targetCommunityId}
              options={targetOptions}
              onChange={(e) => setTargetCommunityId(e.value)}
              placeholder="Target community"
              className="w-full mb-2"
              disabled={!activeCommunityId || targetOptions.length === 0}
            />
            <Button
              label="Create Thread"
              onClick={createThread}
              disabled={!canCreateThread}
              data-testid="create-thread-button"
            />
            {!activeCommunityId && (
              <small className="block mt-2 text-color-secondary">
                Select a community context to enable thread actions.
              </small>
            )}
            {activeCommunityId && targetOptions.length === 0 && (
              <small className="block mt-2 text-color-secondary">
                Join at least one additional community to open cross-community threads.
              </small>
            )}
          </Card>
          <Card title="Post Message">
            <Dropdown
              value={selectedThreadId}
              options={threads.map((thread) => ({ label: thread.title, value: thread.id }))}
              onChange={(e) => setSelectedThreadId(e.value)}
              placeholder="Select thread"
              className="w-full mb-2"
              disabled={!activeCommunityId || threads.length === 0}
            />
            <InputTextarea
              value={newMessage}
              onChange={(e) => setNewMessage(e.target.value)}
              rows={4}
              className="w-full mb-2"
              placeholder="Message"
              disabled={!activeCommunityId || threads.length === 0}
            />
            <Button
              label="Send"
              onClick={sendMessage}
              disabled={!canSendMessage}
              data-testid="send-thread-message-button"
            />
            {threads.length === 0 && (
              <small className="block mt-2 text-color-secondary">
                Create a thread first before posting a message.
              </small>
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
