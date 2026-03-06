import { useEffect, useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { Signal, SignalStatusEntry } from "../types";
import { ProgressBar } from "primereact/progressbar";
import { Divider } from "primereact/divider";
import { InputText } from "primereact/inputtext";
import { useAuthStore } from "../store/useAuthStore";
import { Layout } from "../components/Layout";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicEngagement } from "../components/CivicEngagement";
import { PriorityRadar } from "../components/PriorityRadar";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";
import { CivicStatCard } from "../components/ui/CivicStatCard";

interface ApiError extends Error {
  friendlyMessage?: string;
}

export function SignalDetail() {
  const { t } = useTranslation();
  const { id } = useParams();
  const navigate = useNavigate();
  const { activeRole } = useAuthStore();
  const [signal, setSignal] = useState<Signal | null>(null);
  const [history, setHistory] = useState<SignalStatusEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [voting, setVoting] = useState(false);
  const [assignmentUser, setAssignmentUser] = useState("");
  const [assigning, setAssigning] = useState(false);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const [signalRes, historyRes] = await Promise.all([
        apiClient.get(`signals/${id}`),
        apiClient.get(`signals/${id}/history`)
      ]);
      
      if (signalRes.status === 200) {
        setSignal(signalRes.data);
        setAssignmentUser(signalRes.data.assignedToUsername || "");
      }
      if (historyRes.status === 200) {
        setHistory(historyRes.data || []);
      }
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('common.error'));
      navigate("/");
    } finally {
      setLoading(false);
    }
  }, [id, navigate, t]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const updateStatus = async (newStatus: string) => {
    try {
      const res = await apiClient.patch(`signals/${id}/status`, {
        status: newStatus,
        reason: t("signals.timeline_status_reason", { status: newStatus }),
      });
      if (res.status === 200) {
        toast.success(t('signals.lifecycle_success', { status: newStatus }));
        fetchData();
      }
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('common.error'));
    }
  };

  const handleVote = async () => {
    setVoting(true);
    try {
      const res = await apiClient.post(`signals/${id}/vote`);
      if (res.status === 200) {
        toast.success(t('signals.support_success'));
        fetchData();
      }
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('common.error'));
    } finally {
      setVoting(false);
    }
  };

  const handleAssign = async () => {
    if (!assignmentUser.trim()) {
      toast.error(t("common.required"));
      return;
    }
    setAssigning(true);
    try {
      const res = await apiClient.patch(`signals/${id}/assign`, {
        assigneeUsername: assignmentUser.trim(),
        reason: t("signals.timeline_assignment_reason", { assignee: assignmentUser.trim() }),
      });
      if (res.status === 200) {
        toast.success(t("signals.assignment_success", { assignee: assignmentUser.trim() }));
        fetchData();
      }
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('common.error'));
    } finally {
      setAssigning(false);
    }
  };

  if (loading) return <Layout><div className="p-6"><ProgressBar mode="indeterminate" style={{ height: '6px' }} /></div></Layout>;
  if (!signal) return null;

  const evidenceUrls = (signal.evidenceUrls?.length ? signal.evidenceUrls : signal.imageUrl ? [signal.imageUrl] : []).filter(Boolean) as string[];

  const isStaff = activeRole === "PUBLIC_SERVANT" || activeRole === "SUPER_ADMIN";
  
  let severity: 'new' | 'progress' | 'resolved' | 'rejected' = 'new';
  if (signal.status === "IN_PROGRESS") severity = 'progress';
  if (signal.status === "RESOLVED") severity = 'resolved';
  if (signal.status === "REJECTED") severity = 'rejected';

  const renderTimelineLabel = (entry: SignalStatusEntry) => {
    if (entry.eventType === "ASSIGNED") {
      return t("signals.timeline_event_assigned");
    }
    if (entry.eventType === "CREATED") {
      return t("signals.timeline_event_created");
    }
    return t("signals.timeline_event_status_changed");
  };

  return (
    <Layout>
      <div className="animate-fade-up motion-page pb-8">
        <div className="flex flex-column md:flex-row align-items-start md:align-items-center justify-content-between mb-8 gap-4">
          <div className="flex align-items-start gap-4 min-w-0">
            <CivicButton
              icon="pi pi-arrow-left"
              variant="ghost"
              onClick={() => navigate('/')}
              className="p-3 border-round-circle"
            />
            <div className="min-w-0">
              <div className="u-card-meta-row mb-2">
                <CivicBadge label={signal.status} severity={severity} />
                <span className="text-xs text-muted font-mono font-bold uppercase tracking-widest">
                  {t("signals.protocol_id")}: {signal.id.substring(0,8)}
                </span>
              </div>
              <CivicPageHeader
                title={signal.title}
                description={t("signals.context_header")}
                className="mb-0"
                eyebrow={t("signals.priority_rank")}
              />
            </div>
          </div>
          <CivicButton 
            label={voting ? t("signals.support_loading") : t("signals.support_button")} 
            icon="pi pi-heart-fill" 
            variant="danger"
            className="py-4 px-6 text-lg shadow-xl"
            loading={voting}
            onClick={handleVote} 
            glow
          />
        </div>

        <div className="grid">
          <div className="col-12 lg:col-8">
            <CivicCard
              className="mb-8"
              title={t("signals.context_header")}
              headerActions={(
                <CivicButton
                  icon="pi pi-download"
                  label={t("signals.trust_packet")}
                  variant="ghost"
                  className="text-xs"
                  onClick={() => window.open(`/api/signals/${signal.id}/trust-packet`, '_blank')}
                />
              )}
            >
              <p className="text-xl line-height-4 m-0 text-secondary font-medium leading-relaxed" style={{ whiteSpace: 'pre-wrap' }}>
                {signal.description}
              </p>

              {(signal.locationLabel || evidenceUrls.length > 0) && (
                <div className="mt-5 flex flex-column gap-4">
                  {signal.locationLabel && (
                  <div className="u-pill w-fit" data-testid="signal-detail-location-label">
                    <i className="pi pi-map-marker text-brand-primary"></i>
                    {signal.locationLabel}
                  </div>
                  )}
                  {evidenceUrls.length > 0 && (
                    <div className="grid">
                      {evidenceUrls.map((url, index) => (
                        <div key={`${url}-${index}`} className="col-12 md:col-6">
                          <div className="u-media-frame h-full">
                            <img
                              src={url}
                              alt={t("signals.evidence_alt", { title: signal.title, index: index + 1 })}
                              loading="lazy"
                              className="w-full h-full"
                              style={{ maxHeight: "20rem", objectFit: "cover" }}
                            />
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
              
              <Divider className="my-8 opacity-10" />
              
              <div className="civic-stat-grid civic-stat-grid-comfortable">
                <CivicStatCard
                  label={t("signals.affected_estimation")}
                  value={`${(signal.scoreBreakdown?.affectedPeople || 0) * 10} ${t("signals.citizens")}`}
                  supportingText={t("signals.affected_summary")}
                  icon={<i className="pi pi-users text-brand-primary"></i>}
                />
                <CivicStatCard
                  label={t("signals.civic_category")}
                  value={t(`categories.${signal.category}`)}
                  supportingText={t("signals.classification_summary")}
                  icon={<i className="pi pi-tag text-status-progress"></i>}
                />
              </div>
            </CivicCard>

            <CivicCard title={t("signals.timeline_title")} padding="none" className="mb-8" data-testid="signal-detail-timeline">
              <div className="flex flex-column gap-px bg-white-alpha-10">
                {history.map((entry, idx) => (
                  <div key={entry.id} className="bg-surface p-5 hover:bg-white-alpha-5 transition-colors flex gap-4" data-testid={`signal-timeline-entry-${idx}`}>
                    <div className="flex flex-column align-items-center gap-2">
                      <div className="bg-brand-primary-alpha-20 border-circle p-2 flex align-items-center justify-content-center" style={{ width: '32px', height: '32px' }}>
                        <i className={`pi ${
                          entry.eventType === "ASSIGNED"
                            ? "pi-user-edit"
                            : idx === 0
                              ? "pi-star-fill"
                              : "pi-history"
                        } text-brand-primary text-xs`}></i>
                      </div>
                      {idx !== history.length - 1 && <div className="flex-grow-1 w-2px bg-white-alpha-10"></div>}
                    </div>
                    <div className="flex-grow-1">
                      <div className="flex justify-content-between align-items-start mb-2">
                        <div className="flex align-items-center gap-2">
                          <CivicBadge
                            label={entry.eventType === "ASSIGNED" ? (entry.assignedToUsername || t("signals.timeline_unassigned")) : entry.statusTo}
                            severity={entry.statusTo === 'RESOLVED' ? 'resolved' : 'progress'}
                          />
                          <span className="text-xs font-bold text-muted">{renderTimelineLabel(entry)}</span>
                        </div>
                        <span className="text-xs font-mono text-muted">{new Date(entry.createdAt).toLocaleString()}</span>
                      </div>
                      {entry.eventType === "STATUS_CHANGED" && (
                        <div className="text-xs font-bold text-muted mb-2">
                          {t("signals.timeline_transition", { from: entry.statusFrom, to: entry.statusTo })}
                        </div>
                      )}
                      {entry.eventType === "ASSIGNED" && entry.assignedToUsername && (
                        <div className="text-xs font-bold text-muted mb-2">
                          {t("signals.timeline_assignee_line", { assignee: entry.assignedToUsername })}
                        </div>
                      )}
                      <p className="m-0 text-secondary text-sm font-medium">{entry.reason}</p>
                      <div className="mt-3 text-xs font-black uppercase tracking-widest text-muted">
                        {t("signals.timeline_actor_label")}: <span className="text-brand-primary">{entry.changedBy}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </CivicCard>

            <CivicEngagement 
              parentId={signal.id} 
              parentType="SIGNAL" 
              initialReactions={signal.reactions} 
              initialViewerReaction={signal.viewerReaction}
            />
          </div>

          <div className="col-12 lg:col-4">
            <CivicCard title={t("signals.assignment_title")} className="mb-8" data-testid="signal-detail-assignment-card">
              <div className="flex flex-column gap-4">
                <div>
                  <div className="text-xs text-muted uppercase font-black tracking-widest mb-1">{t("signals.assignment_current_label")}</div>
                  <div className="text-xl font-black text-main" data-testid="signal-assignee-current">
                    {signal.assignedToUsername || t("signals.timeline_unassigned")}
                  </div>
                </div>
                <div className="p-3 border-round-xl border-1 border-subtle bg-surface">
                  <div className="text-xs text-muted uppercase font-black tracking-widest mb-1">{t("signals.assignment_status_label")}</div>
                  <div className="text-sm text-secondary">
                    {signal.assignedToUsername
                      ? t("signals.assignment_status_assigned", { assignee: signal.assignedToUsername })
                      : t("signals.assignment_status_unassigned")}
                  </div>
                </div>
                {isStaff && (
                  <div className="flex flex-column gap-3">
                    <label htmlFor="signal-assignee-input" className="text-xs font-black uppercase tracking-widest text-main">
                      {t("signals.assignment_input_label")}
                    </label>
                    <InputText
                      id="signal-assignee-input"
                      value={assignmentUser}
                      onChange={(e) => setAssignmentUser(e.target.value)}
                      className="w-full"
                      placeholder={t("signals.assignment_input_placeholder")}
                      data-testid="signal-assignee-input"
                    />
                    <CivicButton
                      type="button"
                      label={t("signals.assignment_cta")}
                      icon="pi pi-user-edit"
                      onClick={handleAssign}
                      loading={assigning}
                      data-testid="signal-assignee-submit"
                    />
                  </div>
                )}
              </div>
            </CivicCard>

            <CivicCard className="text-center mb-8" variant="brand" title={t("signals.intel_index")}>
              <div className="text-8xl font-black text-main mb-2 tracking-tighter">
                {signal.priorityScore?.toFixed(0)}
              </div>
              <p className="text-muted text-sm font-bold mb-8 uppercase tracking-widest">{t("signals.priority_rank")}</p>
            </CivicCard>

            <PriorityRadar 
              urgency={signal.scoreBreakdown?.urgency || 0}
              impact={signal.scoreBreakdown?.impact || 0}
              votes={signal.scoreBreakdown?.communityVotes || 0}
              people={signal.scoreBreakdown?.affectedPeople || 0}
            />

            <div className="mb-8"></div>

            <CivicCard title={t('signals.why_ranked_title')} className="mb-8" data-testid="signal-detail-why-ranked">
              <p className="text-sm text-secondary mt-0 mb-5 leading-relaxed">
                {t('signals.why_ranked_desc')}
              </p>
              <div className="flex flex-column gap-4">
                {[
                  { label: t("signals.urgency_factor"), formula: t("signals.urgency_formula") },
                  { label: t("signals.social_impact"), formula: t("signals.impact_formula") },
                  { label: t("signals.affected_estimation"), formula: t("signals.affected_formula") },
                  { label: t("signals.community_trust"), formula: t("signals.votes_formula") }
                ].map((item, idx) => (
                  <div key={idx} className="p-4 border-round-xl bg-white-alpha-5 border-1 border-white-alpha-10 shadow-sm">
                    <div className="text-xs font-black text-main uppercase tracking-widest mb-1">
                      {item.label}
                    </div>
                    <div className="text-xs text-muted font-mono leading-tight">
                      {item.formula}
                    </div>
                  </div>
                ))}
              </div>
            </CivicCard>

            {isStaff && (
              <CivicCard title={t("signals.lifecycle_admin")} variant="brand" className="mb-8">
                <div className="flex flex-column gap-3">
                  <CivicButton label={t("signals.reset_new")} variant="secondary" className="text-xs w-full" onClick={() => updateStatus('NEW')} disabled={signal.status === 'NEW'} />
                  <CivicButton label={t("signals.mark_inprogress")} icon="pi pi-bolt" className="bg-status-progress text-on-brand text-xs w-full" onClick={() => updateStatus('IN_PROGRESS')} disabled={signal.status === 'IN_PROGRESS'} />
                  <CivicButton label={t("signals.mark_resolved")} icon="pi pi-check" className="bg-status-resolved text-on-brand text-xs w-full" onClick={() => updateStatus('RESOLVED')} disabled={signal.status === 'RESOLVED'} />
                </div>
              </CivicCard>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}
