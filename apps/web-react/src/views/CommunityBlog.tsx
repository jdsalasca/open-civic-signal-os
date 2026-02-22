import { useCallback, useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { Dropdown } from "primereact/dropdown";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
import { Avatar } from "primereact/avatar";
import { CommunityBlogPost } from "../types";
import { Layout } from "../components/Layout";
import { useCommunityStore } from "../store/useCommunityStore";
import { useAuthStore } from "../store/useAuthStore";
import apiClient from "../api/axios";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicField } from "../components/ui/CivicField";
import { CivicEngagement } from "../components/CivicEngagement";

type ApiError = Error & { friendlyMessage?: string };

const statusTagOptions = [
  { label: "PLANNED", value: "PLANNED" },
  { label: "IN PROGRESS", value: "IN_PROGRESS" },
  { label: "COMPLETED", value: "COMPLETED" },
  { label: "BLOCKED", value: "BLOCKED" },
];

export function CommunityBlog() {
  const { activeCommunityId, memberships } = useCommunityStore();
  const { activeRole } = useAuthStore();
  const [posts, setPosts] = useState<CommunityBlogPost[]>([]);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [statusTag, setStatusTag] = useState("IN_PROGRESS");
  const [publishing, setPublishing] = useState(false);

  const isStaff = activeRole === "PUBLIC_SERVANT" || activeRole === "SUPER_ADMIN";
  const canPublish = Boolean(activeCommunityId && title.trim() && content.trim());
  const activeCommunityName = memberships.find(m => m.communityId === activeCommunityId)?.communityName;

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
    if (!canPublish) return;
    setPublishing(true);
    try {
      await apiClient.post("community/blog", {
        communityId: activeCommunityId,
        title,
        content,
        statusTag,
      });
      setTitle("");
      setContent("");
      toast.success("Intelligence update published");
      loadPosts();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || "Failed to publish update");
    } finally {
      setPublishing(false);
    }
  };

  const getStatusSeverity = (tag: string) => {
    switch (tag) {
      case 'COMPLETED': return 'resolved';
      case 'BLOCKED': return 'rejected';
      case 'IN_PROGRESS': return 'progress';
      default: return 'new';
    }
  };

  return (
    <Layout>
      <div className="animate-fade-up">
        <div className="mb-8">
          <h1 className="text-5xl font-black mb-2 text-main tracking-tighter">Public Record</h1>
          <p className="text-secondary text-lg font-medium">Official updates and progress reports from {activeCommunityName || 'community staff'}.</p>
        </div>

        <div className="grid">
          {isStaff && (
            <div className="col-12 lg:col-4">
              <CivicCard title="Dispatch Intelligence" variant="brand">
                <div className="flex flex-column gap-2">
                  <CivicField label="Headline">
                    <InputText
                      value={title}
                      onChange={(e) => setTitle(e.target.value)}
                      placeholder="e.g. Phase 1 Verification Complete"
                      className="w-full"
                      data-testid="blog-title-input"
                    />
                  </CivicField>
                  
                  <CivicField label="Detailed Context">
                    <InputTextarea
                      value={content}
                      onChange={(e) => setContent(e.target.value)}
                      rows={6}
                      className="w-full"
                      placeholder="Provide thorough details for the community..."
                      data-testid="blog-content-input"
                    />
                  </CivicField>

                  <CivicField label="Operation Status">
                    <Dropdown
                      value={statusTag}
                      options={statusTagOptions}
                      onChange={(e) => setStatusTag(e.value)}
                      className="w-full bg-black-alpha-20"
                      appendTo={document.body}
                    />
                  </CivicField>

                  <CivicButton
                    label="Publish Dispatch"
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

          <div className={isStaff ? "col-12 lg:col-8" : "col-12 lg:col-8 lg:col-offset-2"}>
            {posts.length === 0 ? (
              <CivicCard className="text-center p-8">
                <i className="pi pi-history text-4xl text-muted mb-4 block"></i>
                <h3 className="text-main text-2xl font-black m-0">No Archives Found</h3>
                <p className="text-secondary mt-2">Historical timeline is currently empty for this sector.</p>
              </CivicCard>
            ) : (
              <div className="flex flex-column gap-8">
                {posts.map((post) => (
                  <div key={post.id} className="flex flex-column gap-4">
                    <div className="glass-panel border-round-3xl p-6 hover:border-white-alpha-30 transition-all duration-300">
                      <div className="flex justify-content-between align-items-start mb-6">
                        <div className="flex align-items-center gap-3">
                          <Avatar label={post.authorUsername?.[0].toUpperCase()} shape="circle" className="bg-brand-primary text-white font-bold shadow-4" />
                          <div className="flex flex-column">
                            <span className="text-sm font-bold text-main">{post.authorUsername}</span>
                            <span className="text-xs text-muted font-bold uppercase tracking-tighter">{post.authorRole}</span>
                          </div>
                        </div>
                        <CivicBadge label={post.statusTag.replace('_', ' ')} severity={getStatusSeverity(post.statusTag)} />
                      </div>

                      <h2 className="text-3xl font-black text-main m-0 mb-4 tracking-tight leading-tight">{post.title}</h2>
                      <p className="text-secondary text-lg line-height-4 font-medium opacity-90 m-0 mb-6" style={{whiteSpace: 'pre-wrap'}}>{post.content}</p>
                      
                      <div className="flex justify-content-between align-items-center pt-6 border-top-1 border-white-alpha-10">
                        <div className="flex align-items-center gap-2 text-xs text-muted font-bold uppercase tracking-widest">
                          <i className="pi pi-calendar"></i>
                          <span>{new Date(post.publishedAt).toLocaleDateString()}</span>
                          <span className="mx-1">•</span>
                          <span>{new Date(post.publishedAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
                        </div>
                      </div>
                    </div>
                    
                    <div className="px-4">
                      <CivicEngagement 
                        parentId={post.id} 
                        parentType="BLOG" 
                        initialReactions={post.reactions} 
                      />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}
