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
import { CivicEmptyState } from "../components/ui/CivicEmptyState";
import { CivicActionBar } from "../components/ui/CivicActionBar";
import { useTranslation } from "react-i18next";

type ApiError = Error & { friendlyMessage?: string };

const roleOptions = [
  { label: "MEMBER", value: "MEMBER" },
  { label: "MODERATOR", value: "MODERATOR" },
  { label: "COORDINATOR", value: "COORDINATOR" },
  { label: "PUBLIC_SERVANT_LIAISON", value: "PUBLIC_SERVANT_LIAISON" },
];

export function Communities() {
  const { t } = useTranslation();
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
      <div className="animate-fade-up motion-page">
        <CivicPageHeader title={t('communities_hub.title')} description={t('communities_hub.desc')} />

        <div className="grid">
          <div className="col-12 lg:col-7">
            <CivicCard title={t('communities_hub.memberships_title')} className="h-full">
              <div className="flex flex-column gap-4">
                <CivicActionBar className="mb-4 p-4">
                  <div className="flex-1" style={{ minWidth: "14rem" }}>
                    <CivicSelect
                      value={selectedCommunityId}
                      options={communityOptions}
                      onChange={(e) => setSelectedCommunityId(e.value)}
                      placeholder={t('communities_hub.select_placeholder')}
                      className="w-full"
                      data-testid="join-community-dropdown"
                    />
                  </div>
                  <div className="flex-1" style={{ minWidth: "12rem" }}>
                    <CivicSelect
                      value={joinRole}
                      options={roleOptions}
                      onChange={(e) => setJoinRole(e.value)}
                      className="w-full"
                      data-testid="join-role-dropdown"
                    />
                  </div>
                  <div className="flex-1" style={{ minWidth: "10rem" }}>
                    <CivicButton
                      label={t('communities_hub.join_action')}
                      icon="pi pi-user-plus"
                      onClick={handleJoin}
                      disabled={!selectedCommunityId}
                      className="w-full"
                      glow
                      data-testid="join-community-button"
                    />
                  </div>
                </CivicActionBar>

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
                        className="w-full md:w-14rem"
                        data-testid={`membership-role-dropdown-${membership.communityId}`}
                      />
                    </div>
                  ))}
                  {memberships.length === 0 && (
                    <CivicEmptyState
                      icon="pi-users"
                      title={t('communities_hub.empty_memberships_title')}
                      description={t('communities_hub.empty_memberships_desc')}
                      actionLabel={t('communities_hub.empty_memberships_action')}
                      onAction={() => {
                        const firstCommunity = communities[0];
                        if (!firstCommunity) return;
                        setSelectedCommunityId(firstCommunity.id);
                      }}
                    />
                  )}
                </div>
              </div>
            </CivicCard>
          </div>

          <div className="col-12 lg:col-5">
            <CivicCard title={t('communities_hub.registry_title')} variant="brand">
              <div className="flex justify-content-between align-items-center mb-6">
                <p className="text-secondary text-sm font-medium m-0">{t('communities_hub.registry_desc')}</p>
                <CivicButton
                  label={t('communities_hub.create_short')}
                  icon="pi pi-plus"
                  onClick={() => setCreating(true)}
                  variant="secondary"
                  size="small"
                  data-testid="open-create-community-button"
                />
              </div>
              {communities.length === 0 ? (
                <CivicEmptyState
                  icon="pi-globe"
                  title={t('communities_hub.empty_registry_title')}
                  description={t('communities_hub.empty_registry_desc')}
                  actionLabel={t('communities_hub.empty_registry_action')}
                  onAction={() => setCreating(true)}
                />
              ) : (
                <div className="flex flex-column gap-3">
                  {communities.map((community) => (
                    <div key={community.id} className="p-4 border-round-xl border-1 border-white-alpha-10 bg-white-alpha-5 flex justify-content-between align-items-center">
                      <div className="font-bold text-main">{community.name}</div>
                      <CivicBadge label={community.slug} type="category" />
                    </div>
                  ))}
                </div>
              )}
            </CivicCard>
          </div>
        </div>
      </div>

      <Dialog 
        header={<div className="text-xl font-black uppercase tracking-tight text-main">{t('communities_hub.create_dialog_title')}</div>} 
        visible={creating} 
        onHide={() => setCreating(false)} 
        className="w-full max-w-30rem mx-3"
        breakpoints={{ '960px': '75vw', '641px': '90vw' }}
      >
        <div className="flex flex-column gap-4 pt-2">
          <div className="flex flex-column gap-2">
            <label htmlFor="comm-name" className="text-xs font-bold uppercase text-muted tracking-widest">{t('communities_hub.create_name')}</label>
            <InputText id="comm-name" value={newName} onChange={(e) => setNewName(e.target.value)} placeholder={t('communities_hub.create_name_placeholder')} className="w-full p-inputtext-lg" />
          </div>
          <div className="flex flex-column gap-2">
            <label htmlFor="comm-slug" className="text-xs font-bold uppercase text-muted tracking-widest">{t('communities_hub.create_slug')}</label>
            <InputText id="comm-slug" value={newSlug} onChange={(e) => setNewSlug(e.target.value)} placeholder={t('communities_hub.create_slug_placeholder')} className="w-full font-mono" />
          </div>
          <div className="flex flex-column gap-2">
            <label htmlFor="comm-desc" className="text-xs font-bold uppercase text-muted tracking-widest">{t('communities_hub.create_description')}</label>
            <InputText
              id="comm-desc"
              value={newDescription}
              onChange={(e) => setNewDescription(e.target.value)}
              placeholder={t('communities_hub.create_description_placeholder')}
              className="w-full"
            />
          </div>
          <div className="flex gap-2 justify-content-end mt-2">
            <CivicButton label={t('common.cancel')} variant="ghost" onClick={() => setCreating(false)} />
            <CivicButton
              label={t('communities_hub.create_action')}
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
