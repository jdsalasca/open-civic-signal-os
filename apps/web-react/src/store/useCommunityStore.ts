import { create } from "zustand";
import { persist } from "zustand/middleware";
import { CommunityMembership } from "../types";

interface CommunityState {
  activeCommunityId: string | null;
  memberships: CommunityMembership[];
  membershipsLoadedAt: number | null;
  setMemberships: (memberships: CommunityMembership[]) => void;
  setActiveCommunityId: (communityId: string | null) => void;
  shouldRefreshMemberships: (maxAgeMs: number) => boolean;
  markMembershipsStale: () => void;
  clear: () => void;
}

export const useCommunityStore = create<CommunityState>()(
  persist(
    (set, get) => ({
      activeCommunityId: null,
      memberships: [],
      membershipsLoadedAt: null,
      setMemberships: (memberships) =>
        set((state) => ({
          memberships,
          membershipsLoadedAt: Date.now(),
          activeCommunityId:
            memberships.find((m) => m.communityId === state.activeCommunityId)?.communityId ??
            memberships[0]?.communityId ??
            null,
        })),
      setActiveCommunityId: (communityId) => set({ activeCommunityId: communityId }),
      shouldRefreshMemberships: (maxAgeMs) => {
        const { membershipsLoadedAt } = get();
        if (membershipsLoadedAt === null) {
          return true;
        }
        return Date.now() - membershipsLoadedAt > maxAgeMs;
      },
      markMembershipsStale: () => set({ membershipsLoadedAt: null }),
      clear: () => set({ activeCommunityId: null, memberships: [], membershipsLoadedAt: null }),
    }),
    { name: "community-storage" }
  )
);
