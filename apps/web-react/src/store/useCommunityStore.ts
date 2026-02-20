import { create } from "zustand";
import { persist } from "zustand/middleware";
import { CommunityMembership } from "../types";

interface CommunityState {
  activeCommunityId: string | null;
  memberships: CommunityMembership[];
  setMemberships: (memberships: CommunityMembership[]) => void;
  setActiveCommunityId: (communityId: string | null) => void;
  clear: () => void;
}

export const useCommunityStore = create<CommunityState>()(
  persist(
    (set) => ({
      activeCommunityId: null,
      memberships: [],
      setMemberships: (memberships) =>
        set((state) => ({
          memberships,
          activeCommunityId: state.activeCommunityId ?? memberships[0]?.communityId ?? null,
        })),
      setActiveCommunityId: (communityId) => set({ activeCommunityId: communityId }),
      clear: () => set({ activeCommunityId: null, memberships: [] }),
    }),
    { name: "community-storage" }
  )
);
