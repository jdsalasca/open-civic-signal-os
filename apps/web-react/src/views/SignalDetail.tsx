import { useEffect, useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { Signal, SignalStatusEntry } from "../types";
import { ProgressBar } from "primereact/progressbar";
import { Divider } from "primereact/divider";
import { useAuthStore } from "../store/useAuthStore";
import { Layout } from "../components/Layout";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicEngagement } from "../components/CivicEngagement";
import { PriorityRadar } from "../components/PriorityRadar";

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

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const [signalRes, historyRes] = await Promise.all([
        apiClient.get(`signals/${id}`),
        apiClient.get(`signals/${id}/history`)
      ]);
      
      if (signalRes.status === 200) {
        setSignal(signalRes.data);
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
      const res = await apiClient.patch(`signals/${id}/status`, { status: newStatus });
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

  if (loading) return <Layout><div className="p-6"><ProgressBar mode="indeterminate" style={{ height: '6px' }} /></div></Layout>;
  if (!signal) return null;

  const isStaff = activeRole === "PUBLIC_SERVANT" || activeRole === "SUPER_ADMIN";
  
  let severity: 'new' | 'progress' | 'resolved' | 'rejected' = 'new';
  if (signal.status === "IN_PROGRESS") severity = 'progress';
  if (signal.status === "RESOLVED") severity = 'resolved';
  if (signal.status === "REJECTED") severity = 'rejected';

  return (
    <Layout>
      <div className="animate-fade-up pb-8">
        <div className="flex flex-column md:flex-row align-items-start md:align-items-center justify-content-between mb-8 gap-4">
          <div className="flex align-items-center gap-4">
            <CivicButton
              icon="pi pi-arrow-left"
              variant="ghost"
              onClick={() => navigate('/')}
              className="p-3 border-round-circle"
            />
            <div>
              <div className="flex align-items-center gap-3 mb-2">
                <CivicBadge label={signal.status} severity={severity} />
                <span className="text-xs text-muted font-mono font-bold uppercase tracking-widest">Protocol ID: {signal.id.substring(0,8)}</span>
              </div>
              <h1 className="text-5xl font-black text-main m-0 tracking-tighter leading-tight">{signal.title}</h1>
            </div>
          </div>
          <CivicButton 
            label={voting ? "Processing..." : "Support Signal"} 
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
            <CivicCard className="mb-8">
              <div className="flex justify-content-between align-items-center mb-8">
                <h3 className="text-brand-primary uppercase text-xs font-black tracking-widest m-0">Intelligence Context</h3>
                <CivicButton 
                  icon="pi pi-download" 
                  label="Trust Packet" 
                  variant="ghost"
                  className="text-xs"
                  onClick={() => window.open(`/api/signals/${signal.id}/trust-packet`, '_blank')}
                />
              </div>
              <p className="text-xl line-height-4 m-0 text-primary font-medium leading-relaxed" style={{ whiteSpace: 'pre-wrap' }}>
                {signal.description}
              </p>
              
              <Divider className="my-8 opacity-10" />
              
              <div className="grid grid-nogutter">
                <div className="col-12 md:col-6 flex align-items-center gap-4 mb-4">
                  <div className="bg-brand-primary-alpha-10 border-round-xl p-4 flex align-items-center justify-content-center shadow-lg" style={{ width: '4.5rem', height: '4rem' }}>
                    <i className="pi pi-users text-brand-primary text-2xl"></i>
                  </div>
                  <div>
                    <div className="text-xs text-muted uppercase font-black tracking-widest mb-1">Affected Citizens</div>
                    <div className="text-2xl font-black text-main">{signal.scoreBreakdown?.affectedPeople * 10} Estimated</div>
                  </div>
                </div>
                <div className="col-12 md:col-6 flex align-items-center gap-4 mb-4">
                  <div className="bg-status-progress-alpha-10 border-round-xl p-4 flex align-items-center justify-content-center shadow-lg" style={{ width: '4.5rem', height: '4rem' }}>
                    <i className="pi pi-tag text-status-progress text-2xl"></i>
                  </div>
                  <div>
                    <div className="text-xs text-muted uppercase font-black tracking-widest mb-1">Classification</div>
                    <div className="text-2xl font-black text-main uppercase tracking-tighter">{t(`categories.${signal.category}`)}</div>
                  </div>
                </div>
              </div>
            </CivicCard>

            <CivicCard title="Operational Audit Trail" padding="none" className="mb-8">
              <div className="flex flex-column gap-px bg-white-alpha-10">
                {history.map((entry, idx) => (
                  <div key={entry.id} className="bg-surface p-5 hover:bg-white-alpha-5 transition-colors flex gap-4">
                    <div className="flex flex-column align-items-center gap-2">
                      <div className="bg-brand-primary-alpha-20 border-circle p-2 flex align-items-center justify-content-center" style={{ width: '32px', height: '32px' }}>
                        <i className={`pi ${idx === 0 ? 'pi-star-fill' : 'pi-history'} text-brand-primary text-xs`}></i>
                      </div>
                      {idx !== history.length - 1 && <div className="flex-grow-1 w-2px bg-white-alpha-10"></div>}
                    </div>
                    <div className="flex-grow-1">
                      <div className="flex justify-content-between align-items-start mb-2">
                        <div className="flex align-items-center gap-2">
                          <CivicBadge label={entry.statusTo} severity={entry.statusTo === 'RESOLVED' ? 'resolved' : 'progress'} />
                          <span className="text-xs font-bold text-muted">from {entry.statusFrom}</span>
                        </div>
                        <span className="text-xs font-mono text-muted">{new Date(entry.createdAt).toLocaleString()}</span>
                      </div>
                      <p className="m-0 text-secondary text-sm font-medium">{entry.reason}</p>
                      <div className="mt-3 text-xs font-black uppercase tracking-widest text-muted">
                        Authored by: <span className="text-brand-primary">{entry.changedBy}</span>
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
            />
          </div>

          <div className="col-12 lg:col-4">
            <CivicCard className="text-center mb-8" variant="brand" title="Intelligence Index">
              <div className="text-8xl font-black text-main mb-2 tracking-tighter">
                {signal.priorityScore?.toFixed(0)}
              </div>
              <p className="text-muted text-sm font-bold mb-8 uppercase tracking-widest">Public Priority Rank</p>
            </CivicCard>

            <PriorityRadar 
              urgency={signal.scoreBreakdown?.urgency || 0}
              impact={signal.scoreBreakdown?.impact || 0}
              votes={signal.scoreBreakdown?.communityVotes || 0}
              people={signal.scoreBreakdown?.affectedPeople || 0}
            />

            <div className="mb-8"></div>

            <CivicCard title={t('signals.why_ranked_title')} className="mb-8">
              <p className="text-sm text-secondary mt-0 mb-5 leading-relaxed">
                {t('signals.why_ranked_desc')}
              </p>
              <div className="flex flex-column gap-4">
                {[
                  { label: "Urgency", formula: "Urgency Factor * 30" },
                  { label: "Social Impact", formula: "Impact Score * 25" },
                  { label: "Affected Citizens", formula: "min(People/10, 30)" },
                  { label: "Community Support", formula: "min(Votes/5, 15)" }
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
              <CivicCard title="Protocol Management" variant="brand" className="mb-8">
                <div className="flex flex-column gap-3">
                  <CivicButton label="Restore to Pending" variant="secondary" className="text-xs w-full" onClick={() => updateStatus('NEW')} disabled={signal.status === 'NEW'} />
                  <CivicButton label="Authorize Progress" icon="pi pi-bolt" className="bg-status-progress text-black text-xs w-full" onClick={() => updateStatus('IN_PROGRESS')} disabled={signal.status === 'IN_PROGRESS'} />
                  <CivicButton label="Finalize Resolution" icon="pi pi-check" className="bg-status-resolved text-black text-xs w-full" onClick={() => updateStatus('RESOLVED')} disabled={signal.status === 'RESOLVED'} />
                </div>
              </CivicCard>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}
