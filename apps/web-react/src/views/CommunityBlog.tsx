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
          <Card title="Publish Community Update">
            <InputText
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full mb-2"
              placeholder="Title"
              disabled={!activeCommunityId}
            />
            <InputTextarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              rows={6}
              className="w-full mb-2"
              placeholder="What changed for this community?"
              disabled={!activeCommunityId}
            />
            <Dropdown
              value={statusTag}
              options={statusTagOptions}
              onChange={(e) => setStatusTag(e.value)}
              className="w-full mb-2"
              disabled={!activeCommunityId}
            />
            <Button
              label="Publish"
              onClick={createPost}
              disabled={!canPublish}
              data-testid="publish-blog-button"
            />
            {!activeCommunityId && (
              <small className="block mt-2 text-color-secondary">
                Select a community context to publish updates.
              </small>
            )}
          </Card>
        </div>
        <div className="col-12 lg:col-8">
          <Card title="Public-Servant Timeline">
            <div className="flex flex-column gap-3">
              {posts.map((post) => (
                <div key={post.id} className="border-1 border-round p-3 border-300">
                  <div className="flex justify-content-between align-items-center mb-2">
                    <strong>{post.title}</strong>
                    <span className="text-sm">{post.statusTag}</span>
                  </div>
                  <p className="my-2">{post.content}</p>
                  <small>
                    By {post.authorUsername} ({post.authorRole}) at{" "}
                    {new Date(post.publishedAt).toLocaleString()}
                  </small>
                </div>
              ))}
            </div>
          </Card>
        </div>
      </div>
    </Layout>
  );
}
