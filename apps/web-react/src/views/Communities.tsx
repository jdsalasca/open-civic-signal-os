import { useCallback, useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { Dialog } from "primereact/dialog";
import { Dropdown } from "primereact/dropdown";
import { InputText } from "primereact/inputtext";
import { Community, CommunityMembership } from "../types";
import { Layout } from "../components/Layout";
import apiClient from "../api/axios";
import { useCommunityStore } from "../store/useCommunityStore";

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
      <div className="grid">
        <div className="col-12 lg:col-6">
          <Card title="Community Memberships" className="shadow-4">
            <div className="grid mb-3">
              <div className="col-12 lg:col-6">
                <Dropdown
                  value={selectedCommunityId}
                  options={communityOptions}
                  onChange={(e) => setSelectedCommunityId(e.value)}
                  placeholder="Select community"
                  className="w-full"
                />
              </div>
              <div className="col-12 md:col-6 lg:col-3">
                <Dropdown
                  value={joinRole}
                  options={roleOptions}
                  onChange={(e) => setJoinRole(e.value)}
                  className="w-full"
                />
              </div>
              <div className="col-12 md:col-6 lg:col-3">
                <Button
                  label="Join"
                  onClick={handleJoin}
                  disabled={!selectedCommunityId}
                  className="w-full"
                  data-testid="community-join-button"
                />
              </div>
            </div>
            {!selectedCommunityId && (
              <small className="block mb-3 text-color-secondary">
                Select a community first to enable joining.
              </small>
            )}
            <div className="flex flex-column gap-2">
              {memberships.map((membership) => (
                <div
                  key={`${membership.communityId}-${membership.userId}`}
                  className="surface-100 dark:surface-900 border-round p-3 flex justify-content-between align-items-center"
                >
                  <div>
                    <div className="font-bold">{membership.communityName}</div>
                    <small>{membership.communitySlug}</small>
                  </div>
                  <Dropdown
                    value={membership.role}
                    options={roleOptions}
                    onChange={(e) => roleUpdate(membership, e.value)}
                    className="w-16rem"
                  />
                </div>
              ))}
            </div>
          </Card>
        </div>
        <div className="col-12 lg:col-6">
          <Card title="Community Registry" className="shadow-4">
            <Button
              label="Create Community"
              icon="pi pi-plus"
              onClick={() => setCreating(true)}
              data-testid="open-create-community-button"
            />
            <ul className="mt-4">
              {communities.map((community) => (
                <li key={community.id}>
                  <strong>{community.name}</strong> - {community.slug}
                </li>
              ))}
            </ul>
          </Card>
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
            <Button label="Cancel" text onClick={() => setCreating(false)} className="text-muted" />
            <Button
              label="Create Community"
              icon="pi pi-check"
              onClick={handleCreate}
              disabled={!newName.trim() || !newSlug.trim()}
              className="p-button-primary px-4 shadow-4"
              data-testid="create-community-submit-button"
            />
          </div>
        </div>
      </Dialog>
    </Layout>
  );
}
