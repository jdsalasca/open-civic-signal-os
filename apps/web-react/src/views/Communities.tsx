import { useCallback, useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { Dialog } from "primereact/dialog";
import { InputText } from "primereact/inputtext";
import { Community, CommunityMembership } from "../types";
import { Layout } from "../components/Layout";
import apiClient from "../api/axios";
import { useCommunityStore } from "../store/useCommunityStore";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicSelect } from "../components/ui/CivicSelect";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";

type ApiError = Error & { friendlyMessage?: string };

const roleOptions = [
  { label: "MEMBER", value: "MEMBER" },
  { label: "MODERATOR", value: "MODERATOR" },
  { label: "COORDINATOR", value: "COORDINATOR" },
  { label: "PUBLIC_SERVANT_LIAISON", value: "PUBLIC_SERVANT_LIAISON" },
];

export function Communities() {
  const { memberships, setMemberships, setActiveCommunityId } = useCommunityStore();
  const [communities, setCommunities] = useState<Community[]>([]);
  const [selectedCommunityId, setSelectedCommunityId] = useState<string>("");
  const [joinRole, setJoinRole] = useState<string>("MEMBER");
  const [creating, setCreating] = useState(false);
  const [newName, setNewName] = useState("");
  const [newSlug, setNewSlug] = useState("");
  const [newDescription, setNewDescription] = useState("");

  const loadData = useCallback(async () => {
    try {
      const [communityRes, membershipRes] = await Promise.all([
        apiClient.get("communities"),
        apiClient.get("communities/my"),
      ]);
      setCommunities(communityRes.data || []);
      setMemberships(membershipRes.data || []);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || "Failed to load communities");
    }
  }, [setMemberships]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleJoin = async () => {
    if (!selectedCommunityId) return;
    try {
      await apiClient.post(`communities/${selectedCommunityId}/join`, { role: joinRole });
      setActiveCommunityId(selectedCommunityId);
      toast.success("Community joined");
      loadData();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || "Join failed");
    }
  };

  const handleCreate = async () => {
    try {
      await apiClient.post("communities", {
        name: newName,
        slug: newSlug,
        description: newDescription,
      });
      toast.success("Community created");
      setCreating(false);
      setNewName("");
      setNewSlug("");
      setNewDescription("");
      loadData();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || "Create failed");
    }
  };

  const roleUpdate = async (membership: CommunityMembership, role: string) => {
    try {
      await apiClient.patch(
        `communities/${membership.communityId}/memberships/${membership.userId}/role`,
        { role }
      );
      toast.success("Role updated");
      loadData();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || "Role update failed");
    }
  };

  const communityOptions = communities.map((community) => ({
    label: `${community.name} (${community.slug})`,
    value: community.id,
  }));

  return (
    <Layout>
      <div className="animate-fade-up">
        <CivicPageHeader title="Community Hub" description="Manage your civic ecosystem and regional memberships." />

        <div className="grid">
          <div className="col-12 lg:col-7">
            <CivicCard title="My Memberships" className="h-full">
              <div className="flex flex-column gap-4">
                <div className="grid grid-nogutter gap-3 mb-4 p-4 border-round-xl bg-white-alpha-5 border-1 border-white-alpha-10">
                  <div className="col-12 lg:col-5">
                    <CivicSelect
                      value={selectedCommunityId}
                      options={communityOptions}
                      onChange={(e) => setSelectedCommunityId(e.value)}
                      placeholder="Select a community"
                      className="w-full"
                      data-testid="join-community-dropdown"
                    />
                  </div>
                  <div className="col-12 md:col-6 lg:col-4">
                    <CivicSelect
                      value={joinRole}
                      options={roleOptions}
                      onChange={(e) => setJoinRole(e.value)}
                      className="w-full"
                      data-testid="join-role-dropdown"
                    />
                  </div>
                  <div className="col-12 md:col-6 lg:col-2 flex-grow-1">
                    <CivicButton
                      label="Join"
                      icon="pi pi-user-plus"
                      onClick={handleJoin}
                      disabled={!selectedCommunityId}
                      className="w-full"
                      glow
                      data-testid="join-community-button"
                    />
                  </div>
                </div>

                <div className="flex flex-column gap-3">
                  {memberships.map((membership) => (
                    <div
                      key={`${membership.communityId}-${membership.userId}`}
                      className="flex justify-content-between align-items-center p-4 border-round-xl bg-white-alpha-5 border-1 border-white-alpha-10 hover:border-white-alpha-20 transition-colors"
                    >
                      <div>
                        <div className="font-black text-main uppercase tracking-tight">{membership.communityName}</div>
                        <div className="text-xs font-mono text-muted mt-1">/{membership.communitySlug}</div>
                      </div>
                      <CivicSelect
                        value={membership.role}
                        options={roleOptions}
                        onChange={(e) => roleUpdate(membership, e.value)}
                        className="w-14rem bg-black-alpha-30"
                        data-testid={`membership-role-dropdown-${membership.communityId}`}
                      />
                    </div>
                  ))}
                  {memberships.length === 0 && (
                    <div className="text-center p-8 text-muted border-2 border-dashed border-white-alpha-10 border-round-2xl">
                      <i className="pi pi-users text-4xl mb-3 block"></i>
                      You are not a member of any community yet.
                    </div>
                  )}
                </div>
              </div>
            </CivicCard>
          </div>

          <div className="col-12 lg:col-5">
            <CivicCard title="Registry Explorer" variant="brand">
              <div className="flex justify-content-between align-items-center mb-6">
                <p className="text-secondary text-sm font-medium m-0">Public directory of verified communities.</p>
                <CivicButton
                  label="Create"
                  icon="pi pi-plus"
                  onClick={() => setCreating(true)}
                  variant="secondary"
                  size="small"
                  data-testid="open-create-community-button"
                />
              </div>
              <div className="flex flex-column gap-3">
                {communities.map((community) => (
                  <div key={community.id} className="p-4 border-round-xl border-1 border-white-alpha-10 bg-black-alpha-20 flex justify-content-between align-items-center">
                    <div className="font-bold text-main">{community.name}</div>
                    <CivicBadge label={community.slug} type="category" />
                  </div>
                ))}
              </div>
            </CivicCard>
          </div>
        </div>
      </div>

      <Dialog 
        header={<div className="text-xl font-black uppercase tracking-tight text-main">Create New Community</div>} 
        visible={creating} 
        onHide={() => setCreating(false)} 
        className="w-full max-w-30rem mx-3"
        breakpoints={{ '960px': '75vw', '641px': '90vw' }}
      >
        <div className="flex flex-column gap-4 pt-2">
          <div className="flex flex-column gap-2">
            <label htmlFor="comm-name" className="text-xs font-bold uppercase text-muted tracking-widest">Community Name</label>
            <InputText id="comm-name" value={newName} onChange={(e) => setNewName(e.target.value)} placeholder="e.g. Springfield Heights" className="w-full p-inputtext-lg" />
          </div>
          <div className="flex flex-column gap-2">
            <label htmlFor="comm-slug" className="text-xs font-bold uppercase text-muted tracking-widest">URL Slug</label>
            <InputText id="comm-slug" value={newSlug} onChange={(e) => setNewSlug(e.target.value)} placeholder="e.g. springfield-heights" className="w-full font-mono" />
          </div>
          <div className="flex flex-column gap-2">
            <label htmlFor="comm-desc" className="text-xs font-bold uppercase text-muted tracking-widest">Description</label>
            <InputText
              id="comm-desc"
              value={newDescription}
              onChange={(e) => setNewDescription(e.target.value)}
              placeholder="Provide a brief context for this community"
              className="w-full"
            />
          </div>
          <div className="flex gap-2 justify-content-end mt-2">
            <CivicButton label="Cancel" variant="ghost" onClick={() => setCreating(false)} />
            <CivicButton
              label="Create Community"
              icon="pi pi-check"
              onClick={handleCreate}
              disabled={!newName.trim() || !newSlug.trim()}
              data-testid="create-community-submit-button"
              glow
            />
          </div>
        </div>
      </Dialog>
    </Layout>
  );
}
