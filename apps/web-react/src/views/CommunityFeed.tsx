import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { Dropdown } from "primereact/dropdown";
import { CommunityFeedItem } from "../types";
import { Layout } from "../components/Layout";
import { useCommunityStore } from "../store/useCommunityStore";
import apiClient from "../api/axios";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";

type ApiError = Error & { friendlyMessage?: string };

const typeOptions = [
  { label: "All Activities", value: "all" },
  { label: "Civic Signals", value: "signal" },
  { label: "Public Updates", value: "blog" },
  { label: "Community Dialogues", value: "thread-update" },
];

export function CommunityFeed() {
  const navigate = useNavigate();
  const { activeCommunityId, memberships } = useCommunityStore();
  const [items, setItems] = useState<CommunityFeedItem[]>([]);
  const [typeFilter, setTypeFilter] = useState<string>("all");
  const [days, setDays] = useState<number>(7);

  const activeCommunityName = memberships.find(m => m.communityId === activeCommunityId)?.communityName;

  const loadFeed = useCallback(async () => {
    if (!activeCommunityId) return;
    try {
      const res = await apiClient.get(
        `community/feed?communityId=${activeCommunityId}&days=${days}`
      );
      setItems(res.data || []);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || "Failed to load feed");
    }
  }, [activeCommunityId, days]);

  useEffect(() => {
    loadFeed();
  }, [loadFeed]);

  const filteredItems = items.filter((item) =>
    typeFilter === "all" ? true : item.type === typeFilter
  );

  const getItemIcon = (type: string) => {
    switch (type) {
      case 'signal': return 'pi-bolt text-status-new';
      case 'blog': return 'pi-megaphone text-status-progress';
      case 'thread-update': return 'pi-comments text-brand-primary';
      default: return 'pi-info-circle';
    }
  };

  const handleItemClick = (item: CommunityFeedItem) => {
    if (item.type === 'signal') navigate(`/signal/${item.id}`);
    if (item.type === 'blog') navigate(`/communities/blog`);
    if (item.type === 'thread-update') navigate(`/communities/threads`);
  };

  return (
    <Layout>
      <div className="animate-fade-up">
        <div className="flex flex-column md:flex-row justify-content-between align-items-start md:align-items-center mb-8 gap-4">
          <div>
            <h1 className="text-5xl font-black mb-2 text-main tracking-tighter">Live Ecosystem</h1>
            <p className="text-secondary text-lg font-medium">Real-time pulses from {activeCommunityName || 'your community'}.</p>
          </div>
          
          <div className="flex gap-3 bg-black-alpha-20 p-2 border-round-2xl border-1 border-white-alpha-10">
            <Dropdown
              value={typeFilter}
              options={typeOptions}
              onChange={(e) => setTypeFilter(e.value)}
              className="w-14rem bg-transparent border-none font-bold"
              disabled={!activeCommunityId}
            />
            <Dropdown
              value={days}
              options={[
                { label: "Past 7 days", value: 7 },
                { label: "Past 14 days", value: 14 },
                { label: "Past 30 days", value: 30 },
              ]}
              onChange={(e) => setDays(e.value)}
              className="w-12rem bg-transparent border-none font-bold"
              disabled={!activeCommunityId}
            />
          </div>
        </div>

        {!activeCommunityId ? (
          <CivicCard className="text-center p-8">
            <i className="pi pi-map-marker text-4xl text-muted mb-4 block"></i>
            <h3 className="text-main text-2xl font-black m-0">No Community Selected</h3>
            <p className="text-secondary mt-2 mb-6">Select a community context from the navigation bar to see live activity.</p>
            <CivicButton label="Explore Communities" variant="secondary" onClick={() => navigate('/communities')} />
          </CivicCard>
        ) : (
          <div className="grid">
            <div className="col-12 lg:col-8 lg:col-offset-2">
              {filteredItems.length === 0 ? (
                <CivicEmptyState 
                  icon="pi-moon"
                  title="No Pulse Detected"
                  description="This sector is currently quiet. No activity has been recorded in the selected time window."
                  actionLabel="Back to Dashboard"
                  onAction={() => navigate("/")}
                />
              ) : (
                <div className="flex flex-column gap-4">
                  {filteredItems.map((item) => (
                    <div 
                      key={`${item.type}-${item.id}`} 
                      onClick={() => handleItemClick(item)}
                      className="group cursor-pointer glass-panel p-5 border-round-3xl transition-all duration-300 hover:scale-[1.02] hover:border-white-alpha-30 relative overflow-hidden"
                    >
                      <div className="flex align-items-start gap-4">
                        <div className="bg-white-alpha-5 border-round-2xl p-3 flex align-items-center justify-content-center border-1 border-white-alpha-10 group-hover:bg-brand-primary-alpha-10 transition-colors">
                          <i className={`pi ${getItemIcon(item.type)} text-2xl`}></i>
                        </div>
                        <div className="flex-grow-1">
                          <div className="flex justify-content-between align-items-center mb-2">
                            <CivicBadge label={item.type.replace('-', ' ')} type="category" />
                            <span className="text-xs font-bold text-muted uppercase tracking-widest">{item.freshness}</span>
                          </div>
                          <h3 className="text-xl font-black text-main m-0 mb-2 group-hover:text-brand-primary transition-colors leading-tight">{item.title}</h3>
                          <p className="text-secondary m-0 line-height-3 text-sm font-medium opacity-80">{item.summary}</p>
                          <div className="mt-4 flex align-items-center gap-2 text-xs text-muted font-bold">
                            <i className="pi pi-calendar"></i>
                            <span>{new Date(item.happenedAt).toLocaleString()}</span>
                          </div>
                        </div>
                        <div className="flex align-self-center opacity-0 group-hover:opacity-100 transition-opacity">
                           <i className="pi pi-arrow-right text-brand-primary text-xl"></i>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
