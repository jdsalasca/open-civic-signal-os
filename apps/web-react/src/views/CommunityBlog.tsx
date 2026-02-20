import { useCallback, useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { Dropdown } from "primereact/dropdown";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
import { CommunityBlogPost } from "../types";
import { Layout } from "../components/Layout";
import { useCommunityStore } from "../store/useCommunityStore";
import apiClient from "../api/axios";

type ApiError = Error & { friendlyMessage?: string };

const statusTagOptions = [
  { label: "PLANNED", value: "PLANNED" },
  { label: "IN_PROGRESS", value: "IN_PROGRESS" },
  { label: "COMPLETED", value: "COMPLETED" },
  { label: "BLOCKED", value: "BLOCKED" },
];

export function CommunityBlog() {
  const { activeCommunityId } = useCommunityStore();
  const [posts, setPosts] = useState<CommunityBlogPost[]>([]);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [statusTag, setStatusTag] = useState("IN_PROGRESS");
  const canPublish = Boolean(activeCommunityId && title.trim() && content.trim());

  const loadPosts = useCallback(async () => {
    if (!activeCommunityId) return;
    try {
      const res = await apiClient.get(`community/blog?communityId=${activeCommunityId}`);
      setPosts(res.data || []);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || "Failed to load blog timeline");
    }
  }, [activeCommunityId]);

  useEffect(() => {
    loadPosts();
  }, [loadPosts]);

  const createPost = async () => {
    if (!activeCommunityId || !title.trim() || !content.trim()) return;
    try {
      await apiClient.post("community/blog", {
        communityId: activeCommunityId,
        title,
        content,
        statusTag,
      });
      setTitle("");
      setContent("");
      toast.success("Update published");
      loadPosts();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || "Failed to publish update");
    }
  };

  return (
    <Layout>
      <div className="grid">
        <div className="col-12 lg:col-4">
          <Card title={<span className="uppercase text-sm font-bold tracking-widest text-purple-500">Publish Update</span>} className="shadow-4 border-1 border-white-alpha-10 bg-surface">
            <div className="flex flex-column gap-3">
              <div>
                <label className="text-xs font-bold uppercase text-muted mb-1 block">Headline</label>
                <InputText
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className="w-full"
                  placeholder="e.g. Park Renovation Started"
                  disabled={!activeCommunityId}
                />
              </div>
              <div>
                <label className="text-xs font-bold uppercase text-muted mb-1 block">Details</label>
                <InputTextarea
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  rows={6}
                  className="w-full"
                  placeholder="Share progress details with the community..."
                  disabled={!activeCommunityId}
                />
              </div>
              <div>
                <label className="text-xs font-bold uppercase text-muted mb-1 block">Status</label>
                <Dropdown
                  value={statusTag}
                  options={statusTagOptions}
                  onChange={(e) => setStatusTag(e.value)}
                  className="w-full"
                  disabled={!activeCommunityId}
                />
              </div>
              <Button
                label="Publish Update"
                icon="pi pi-megaphone"
                onClick={createPost}
                disabled={!canPublish}
                className="w-full p-button-help"
                data-testid="publish-blog-button"
              />
            </div>
            {!activeCommunityId && (
              <div className="mt-3 p-2 border-round bg-gray-800 text-gray-400 text-xs">
                Select a community context to publish updates.
              </div>
            )}
          </Card>
        </div>
        <div className="col-12 lg:col-8">
          <Card title={<span className="uppercase text-sm font-bold tracking-widest text-main">Public-Servant Timeline</span>} className="shadow-4 border-1 border-white-alpha-10 bg-surface">
            {posts.length === 0 ? (
              <div className="text-center p-5 text-muted">
                <i className="pi pi-inbox text-2xl mb-3 block"></i>
                No updates published yet.
              </div>
            ) : (
              <div className="flex flex-column gap-3">
                {posts.map((post) => (
                  <div key={post.id} className="surface-0 border-1 border-round p-4 border-300 dark:bg-black-alpha-20 dark:border-white-alpha-10">
                    <div className="flex justify-content-between align-items-center mb-2">
                      <strong className="text-xl text-main">{post.title}</strong>
                      <span className={`text-xs font-bold px-2 py-1 border-round ${
                        post.statusTag === 'COMPLETED' ? 'bg-green-900 text-green-400' :
                        post.statusTag === 'BLOCKED' ? 'bg-red-900 text-red-400' :
                        'bg-blue-900 text-blue-400'
                      }`}>{post.statusTag}</span>
                    </div>
                    <p className="my-3 line-height-3 text-lg text-color-secondary" style={{whiteSpace: 'pre-wrap'}}>{post.content}</p>
                    <div className="flex align-items-center gap-2 text-xs text-muted">
                      <i className="pi pi-user"></i>
                      <span>{post.authorUsername} ({post.authorRole})</span>
                      <span>•</span>
                      <span>{new Date(post.publishedAt).toLocaleString()}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>
      </div>
    </Layout>
  );
}
