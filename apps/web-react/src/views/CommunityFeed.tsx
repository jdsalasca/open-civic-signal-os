import { useCallback, useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { Card } from "primereact/card";
import { Dropdown } from "primereact/dropdown";
import { CommunityFeedItem } from "../types";
import { Layout } from "../components/Layout";
import { useCommunityStore } from "../store/useCommunityStore";
import apiClient from "../api/axios";

type ApiError = Error & { friendlyMessage?: string };

const typeOptions = [
  { label: "All", value: "all" },
  { label: "Signals", value: "signal" },
  { label: "Blogs", value: "blog" },
  { label: "Thread Updates", value: "thread-update" },
];

export function CommunityFeed() {
  const { activeCommunityId } = useCommunityStore();
  const [items, setItems] = useState<CommunityFeedItem[]>([]);
  const [typeFilter, setTypeFilter] = useState<string>("all");
  const [days, setDays] = useState<number>(7);

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

  return (
    <Layout>
      <Card title="Cross-Community Feed">
        <div className="flex gap-2 mb-4">
          <Dropdown
            value={typeFilter}
            options={typeOptions}
            onChange={(e) => setTypeFilter(e.value)}
            className="w-14rem"
          />
          <Dropdown
            value={days}
            options={[
              { label: "7 days", value: 7 },
              { label: "14 days", value: 14 },
              { label: "30 days", value: 30 },
            ]}
            onChange={(e) => setDays(e.value)}
            className="w-10rem"
          />
        </div>
        {filteredItems.length === 0 ? (
          <div className="text-color-secondary">No updates for selected filters.</div>
        ) : (
          <div className="flex flex-column gap-3">
            {filteredItems.map((item) => (
              <div key={`${item.type}-${item.id}`} className="border-1 border-round p-3 border-300">
                <div className="flex justify-content-between align-items-center">
                  <strong>{item.title}</strong>
                  <span className="text-sm uppercase">{item.type}</span>
                </div>
                <p className="my-2">{item.summary}</p>
                <small>
                  {new Date(item.happenedAt).toLocaleString()} | {item.freshness}
                </small>
              </div>
            ))}
          </div>
        )}
      </Card>
    </Layout>
  );
}
