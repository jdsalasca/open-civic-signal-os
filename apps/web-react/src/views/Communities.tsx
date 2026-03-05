import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "react-hot-toast";
import { Dialog } from "primereact/dialog";
import { InputText } from "primereact/inputtext";
import { Community, CommunityMembership, CommunityTreeNode } from "../types";
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

function CommunityTreeBranch({
  nodes,
  activeCommunityId,
  onActivate,
  activeBadgeLabel,
  activeActionLabel,
  activateActionLabel,
  noDescriptionLabel,
  level = 0,
}: {
  nodes: CommunityTreeNode[];
  activeCommunityId: string | null;
  onActivate: (communityId: string) => void;
  activeBadgeLabel: string;
  activeActionLabel: string;
  activateActionLabel: string;
  noDescriptionLabel: string;
  level?: number;
}) {
  return (
    <div className="flex flex-column gap-3">
      {nodes.map((node) => {
        const isActive = node.id === activeCommunityId;
        return (
          <div key={node.id} className="flex flex-column gap-2">
            <div
              className="border-round-xl border-1 p-3 bg-white-alpha-5"
              style={{ marginLeft: `${level * 1.25}rem` }}
            >
              <div className="flex justify-content-between align-items-start gap-3 flex-wrap">
                <div className="flex-1 min-w-0">
                  <div className="flex align-items-center gap-2 flex-wrap">
                    <span className="font-black text-main">{node.name}</span>
                    <CivicBadge label={node.slug} type="category" />
                    {isActive && <CivicBadge label={activeBadgeLabel} severity="progress" />}
                  </div>
                  <p className="text-sm text-secondary mt-2 mb-0 leading-relaxed">
                    {node.description || noDescriptionLabel}
                  </p>
                </div>
                <CivicButton
                  label={isActive ? activeActionLabel : activateActionLabel}
                  icon={isActive ? 'pi pi-check' : 'pi pi-arrow-right'}
                  variant={isActive ? 'secondary' : 'ghost'}
                  size="small"
                  type="button"
                  onClick={() => onActivate(node.id)}
                />
              </div>
            </div>
            {node.children.length > 0 && (
              <CommunityTreeBranch
                nodes={node.children}
                activeCommunityId={activeCommunityId}
                onActivate={onActivate}
                activeBadgeLabel={activeBadgeLabel}
                activeActionLabel={activeActionLabel}
                activateActionLabel={activateActionLabel}
                noDescriptionLabel={noDescriptionLabel}
                level={level + 1}
              />
            )}
          </div>
        );
      })}
    </div>
  );
}

export function Communities() {
  const { t } = useTranslation();
  const { memberships, setMemberships, setActiveCommunityId, activeCommunityId, getActiveMembership } = useCommunityStore();
  const [communities, setCommunities] = useState<Community[]>([]);
  const [communityTree, setCommunityTree] = useState<CommunityTreeNode[]>([]);
  const [selectedCommunityId, setSelectedCommunityId] = useState<string>("");
  const [joinRole, setJoinRole] = useState<string>("MEMBER");
  const [creating, setCreating] = useState(false);
  const [newName, setNewName] = useState("");
  const [newSlug, setNewSlug] = useState("");
  const [newDescription, setNewDescription] = useState("");
  const [newParentCommunityId, setNewParentCommunityId] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    try {
      const [communityRes, treeRes, membershipRes] = await Promise.all([
        apiClient.get("communities"),
        apiClient.get("communities/tree"),
        apiClient.get("communities/my"),
      ]);
      setCommunities(communityRes.data || []);
      setCommunityTree(treeRes.data || []);
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
      toast.success(t('communities_hub.join_success'));
      loadData();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('communities_hub.join_error'));
    }
  };

  const handleCreate = async () => {
    try {
      const response = await apiClient.post("communities", {
        name: newName,
        slug: newSlug,
        description: newDescription,
        parentCommunityId: newParentCommunityId,
      });
      toast.success(t('communities_hub.create_success'));
      setCreating(false);
      setNewName("");
      setNewSlug("");
      setNewDescription("");
      setNewParentCommunityId(null);
      setActiveCommunityId(response.data.id);
      loadData();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('communities_hub.create_error'));
    }
  };

  const roleUpdate = async (membership: CommunityMembership, role: string) => {
    try {
      await apiClient.patch(
        `communities/${membership.communityId}/memberships/${membership.userId}/role`,
        { role }
      );
      toast.success(t('communities_hub.role_update_success'));
      loadData();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('communities_hub.role_update_error'));
    }
  };

  const communityOptions = communities.map((community) => ({
    label: `${community.name} (${community.slug})`,
    value: community.id,
  }));

  const roleOptions = useMemo(
    () => [
      { label: t("settings.roles.MEMBER"), value: "MEMBER" },
      { label: t("settings.roles.MODERATOR"), value: "MODERATOR" },
      { label: t("settings.roles.COORDINATOR"), value: "COORDINATOR" },
      { label: t("settings.roles.PUBLIC_SERVANT_LIAISON"), value: "PUBLIC_SERVANT_LIAISON" },
    ],
    [t]
  );

  const parentOptions = [
    { label: t('communities_hub.parent_none'), value: null },
    ...communities.map((community) => ({
      label: `${community.name} / ${community.slug}`,
      value: community.id,
    })),
  ];

  const activeMembership = getActiveMembership();
  const activeBreadcrumb = activeMembership?.breadcrumb ?? [];
  const roleLabels = useMemo(
    () => Object.fromEntries(roleOptions.map((option) => [option.value, option.label])),
    [roleOptions]
  );
  const activeRoleLabel = activeMembership ? roleLabels[activeMembership.role] ?? activeMembership.role : null;

  const unjoinedCommunityOptions = useMemo(() => {
    const joinedIds = new Set(memberships.map((membership) => membership.communityId));
    return communityOptions.filter((option) => !joinedIds.has(option.value));
  }, [communityOptions, memberships]);

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <CivicPageHeader title={t('communities_hub.title')} description={t('communities_hub.desc')} />

        <div className="grid">
          <div className="col-12 lg:col-7">
            <CivicCard title={t('communities_hub.memberships_title')} className="h-full mb-6">
              <div className="flex flex-column gap-4">
                <div className="border-round-xl border-1 border-white-alpha-10 bg-white-alpha-5 p-4" data-testid="active-community-breadcrumb-card">
                  <div className="text-xs font-bold uppercase text-muted tracking-widest mb-2">
                    {t('communities_hub.active_path_label')}
                  </div>
                  {activeBreadcrumb.length > 0 ? (
                    <div className="flex flex-wrap align-items-center gap-2">
                      {activeBreadcrumb.map((item, index) => (
                        <div key={item.id} className="flex align-items-center gap-2">
                          <span className={`font-bold ${index === activeBreadcrumb.length - 1 ? 'text-main' : 'text-secondary'}`}>
                            {item.name}
                          </span>
                          {index < activeBreadcrumb.length - 1 && <span className="text-muted">/</span>}
                        </div>
                      ))}
                      {activeRoleLabel && <CivicBadge label={activeRoleLabel} severity="progress" />}
                    </div>
                  ) : (
                    <p className="text-secondary text-sm m-0">{t('communities_hub.active_path_empty')}</p>
                  )}
                </div>

                <CivicActionBar className="mb-4 p-4">
                  <div className="flex-1" style={{ minWidth: "14rem" }}>
                    <CivicSelect
                      value={selectedCommunityId}
                      options={unjoinedCommunityOptions}
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
                      className="flex justify-content-between align-items-center p-4 border-round-xl bg-white-alpha-5 border-1 border-white-alpha-10 hover:border-white-alpha-20 transition-colors gap-3 flex-wrap"
                    >
                      <div className="flex-1 min-w-0">
                        <div className="font-black text-main uppercase tracking-tight">{membership.communityName}</div>
                        <div className="text-xs font-mono text-muted mt-1">/{membership.communitySlug}</div>
                        <div className="text-xs text-secondary mt-2">
                          {membership.breadcrumb.map((item) => item.name).join(' / ')}
                        </div>
                      </div>
                      <div className="flex gap-2 align-items-center flex-wrap">
                        <CivicButton
                          label={activeCommunityId === membership.communityId ? t('communities_hub.context_active') : t('communities_hub.context_open')}
                          icon={activeCommunityId === membership.communityId ? 'pi pi-check' : 'pi pi-arrow-right'}
                          variant={activeCommunityId === membership.communityId ? 'secondary' : 'ghost'}
                          size="small"
                          type="button"
                          onClick={() => setActiveCommunityId(membership.communityId)}
                          data-testid={`activate-community-button-${membership.communityId}`}
                        />
                        <CivicSelect
                          value={membership.role}
                          options={roleOptions}
                          onChange={(e) => roleUpdate(membership, e.value)}
                          className="w-full md:w-14rem"
                          data-testid={`membership-role-dropdown-${membership.communityId}`}
                        />
                      </div>
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
              <div className="flex justify-content-between align-items-center mb-6 gap-3 flex-wrap">
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
              {communityTree.length === 0 ? (
                <CivicEmptyState
                  icon="pi-globe"
                  title={t('communities_hub.empty_registry_title')}
                  description={t('communities_hub.empty_registry_desc')}
                  actionLabel={t('communities_hub.empty_registry_action')}
                  onAction={() => setCreating(true)}
                />
              ) : (
                <div data-testid="community-tree-view">
                  <CommunityTreeBranch
                    nodes={communityTree}
                    activeCommunityId={activeCommunityId}
                    onActivate={setActiveCommunityId}
                    activeBadgeLabel={t('communities_hub.tree_active_badge')}
                    activeActionLabel={t('communities_hub.context_active')}
                    activateActionLabel={t('communities_hub.context_open')}
                    noDescriptionLabel={t('communities_hub.tree_no_description')}
                  />
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
            <label htmlFor="comm-parent" className="text-xs font-bold uppercase text-muted tracking-widest">{t('communities_hub.create_parent')}</label>
            <div id="comm-parent" data-testid="parent-community-dropdown">
              <CivicSelect
                value={newParentCommunityId}
                options={parentOptions}
                onChange={(e) => setNewParentCommunityId(e.value as string | null)}
                className="w-full"
              />
            </div>
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
            <CivicButton label={t('common.cancel')} variant="ghost" type="button" onClick={() => setCreating(false)} />
            <CivicButton
              label={t('communities_hub.create_action')}
              icon="pi pi-check"
              type="button"
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
