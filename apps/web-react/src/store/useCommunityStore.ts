import { create } from "zustand";
import { persist } from "zustand/middleware";
import { CommunityMembership } from "../types";

type CommunityThreadListState = {
  page: number;
  rows: number;
  status: "ALL" | "ACTIVE" | "STALE";
};

interface CommunityState {
  activeCommunityId: string | null;
  memberships: CommunityMembership[];
  membershipsLoadedAt: number | null;
  threadListStateByCommunity: Record<string, CommunityThreadListState>;
  setMemberships: (memberships: CommunityMembership[]) => void;
  setActiveCommunityId: (communityId: string | null) => void;
  setThreadListState: (communityId: string, state: CommunityThreadListState) => void;
  getThreadListState: (communityId: string) => CommunityThreadListState;
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
      threadListStateByCommunity: {},
      setMemberships: (memberships) =>
        set((state) => ({
          threadListStateByCommunity: Object.fromEntries(
            Object.entries(state.threadListStateByCommunity).filter(([communityId]) =>
              memberships.some((membership) => membership.communityId === communityId)
            )
          ),
          memberships,
          membershipsLoadedAt: Date.now(),
          activeCommunityId:
            memberships.find((m) => m.communityId === state.activeCommunityId)?.communityId ??
            memberships[0]?.communityId ??
            null,
        })),
      setActiveCommunityId: (communityId) =>
        set((state) => {
          if (communityId === null) {
            return { activeCommunityId: null };
          }
          const isValidMembership = state.memberships.some((membership) => membership.communityId === communityId);
          if (!isValidMembership) {
            return {};
          }
          return { activeCommunityId: communityId };
        }),
      setThreadListState: (communityId, state) =>
        set((current) => ({
          threadListStateByCommunity: {
            ...current.threadListStateByCommunity,
            [communityId]: state,
          },
        })),
      getThreadListState: (communityId) =>
        get().threadListStateByCommunity[communityId] ?? { page: 0, rows: 10, status: "ALL" },
      shouldRefreshMemberships: (maxAgeMs) => {
        const { membershipsLoadedAt } = get();
        if (membershipsLoadedAt === null) {
          return true;
        }
        return Date.now() - membershipsLoadedAt > maxAgeMs;
      },
      markMembershipsStale: () => set({ membershipsLoadedAt: null }),
      clear: () =>
        set({
          activeCommunityId: null,
          memberships: [],
          membershipsLoadedAt: null,
          threadListStateByCommunity: {},
        }),
    }),
    { name: "community-storage" }
  )
);
