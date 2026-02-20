import { useEffect, useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { Signal } from "../types";
import { Card } from "primereact/card";
import { Button } from "primereact/button";
import { Tag } from "primereact/tag";
import { ProgressBar } from "primereact/progressbar";
import { Divider } from "primereact/divider";
import { useAuthStore } from "../store/useAuthStore";
import { Layout } from "../components/Layout";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";

interface ApiError extends Error {
  friendlyMessage?: string;
}

export function SignalDetail() {
  const { t } = useTranslation();
  const { id } = useParams();
  const navigate = useNavigate();
  const { activeRole } = useAuthStore();
  const [signal, setSignal] = useState<Signal | null>(null);
  const [loading, setLoading] = useState(true);
  const [voting, setVoting] = useState(false);

  const fetchSignal = useCallback(async () => {
    try {
      setLoading(true);
      const res = await apiClient.get(`signals/${id}`);
      if (res.status === 200) {
        setSignal(res.data);
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
    fetchSignal();
  }, [fetchSignal]);

  const updateStatus = async (newStatus: string) => {
    try {
      const res = await apiClient.patch(`signals/${id}/status`, { status: newStatus });
      if (res.status === 200) {
        toast.success(t('signals.lifecycle_success', { status: newStatus }));
        fetchSignal();
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
        fetchSignal();
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
  const getStatusSeverity = (s: string) => {
    if (s === 'NEW') return 'info';
    if (s === 'IN_PROGRESS') return 'warning';
    return 'success';
  };

  return (
    <Layout>
      <div className="animate-fade-in pb-6" data-testid="signal-detail-view">
        <div className="flex flex-column md:flex-row align-items-start md:align-items-center justify-content-between mb-5 gap-3">
          <div className="flex align-items-center gap-3">
            <Button
              icon="pi pi-arrow-left"
              rounded
              text
              className="text-muted hover:text-main"
              onClick={() => navigate('/')}
              aria-label={t('common.back')}
              data-testid="signal-detail-back-button"
            />
            <div>
              <div className="flex align-items-center gap-2 mb-1">
                <Tag value={signal.status} severity={getStatusSeverity(signal.status)} className="px-2" data-testid="signal-status-tag" />
                <span className="text-xs text-muted font-mono font-bold uppercase tracking-widest">{t('signals.ref')}: {signal.id.substring(0,8)}</span>
              </div>
              <h1 className="text-4xl font-black text-main m-0 tracking-tight line-height-1" data-testid="signal-title">{signal.title}</h1>
            </div>
          </div>
          <Button 
            label={voting ? t('signals.support_loading') : t('signals.support_button')} 
            icon="pi pi-heart-fill" 
            className="p-button-primary px-5 py-3 shadow-4 font-bold text-lg bg-red-600 border-none hover:bg-red-500" 
            loading={voting}
            onClick={handleVote} 
            data-testid="support-signal-button"
            aria-label={t('signals.support_button')}
          />
        </div>

        <div className="grid">
          <div className="col-12 lg:col-8">
            <Card className="mb-4 shadow-6" data-testid="signal-description-card">
              <h3 className="text-muted mb-4 uppercase text-xs font-black tracking-widest">{t('signals.problem_definition')}</h3>
              <p className="text-xl line-height-4 m-0 text-main font-medium" style={{ whiteSpace: 'pre-wrap' }}>
                {signal.description || "No descriptive context provided for this signal."}
              </p>
              
              <Divider className="my-5 opacity-10" />
              
              <div className="grid grid-nogutter">
                <div className="col-12 md:col-6 flex align-items-center gap-3 mb-3">
                  <div className="bg-cyan-900 border-round p-3 flex align-items-center justify-content-center" style={{ width: '3rem', height: '3rem' }}>
                    <i className="pi pi-users text-cyan-400 text-xl"></i>
                  </div>
                  <div>
                    <div className="text-xs text-muted uppercase font-black tracking-widest">{t('signals.affected_estimation')}</div>
                    <div className="text-xl font-black text-main" data-testid="affected-people-value">{(signal.scoreBreakdown?.affectedPeople || 0) * 10} {t('signals.citizens')}</div>
                  </div>
                </div>
                <div className="col-12 md:col-6 flex align-items-center gap-3 mb-3">
                  <div className="bg-purple-900 border-round p-3 flex align-items-center justify-content-center" style={{ width: '3rem', height: '3rem' }}>
                    <i className="pi pi-tag text-purple-400 text-xl"></i>
                  </div>
                  <div>
                    <div className="text-xs text-muted uppercase font-black tracking-widest">{t('signals.civic_category')}</div>
                    <div className="text-xl font-black text-main uppercase tracking-tighter" data-testid="signal-category-value">{t(`categories.${signal.category}`)}</div>
                  </div>
                </div>
              </div>
            </Card>

            {isStaff && (
              <Card title={<span className="text-sm font-black uppercase tracking-widest text-cyan-500">{t('signals.lifecycle_admin')}</span>} className="border-1 border-dashed border-cyan-800 bg-black-alpha-30 shadow-2" data-testid="staff-operations-card">
                <div className="flex flex-wrap gap-3 mt-2">
                  <Button label={t('signals.reset_new')} outlined severity="info" size="small" className="font-bold border-gray-700" onClick={() => updateStatus('NEW')} disabled={signal.status === 'NEW'} data-testid="status-new-button" />
                  <Button label={t('signals.mark_inprogress')} severity="warning" size="small" icon="pi pi-bolt" className="font-bold shadow-2" onClick={() => updateStatus('IN_PROGRESS')} disabled={signal.status === 'IN_PROGRESS'} data-testid="status-inprogress-button" />
                  <Button label={t('signals.mark_resolved')} severity="success" size="small" icon="pi pi-check" className="font-bold shadow-2" onClick={() => updateStatus('RESOLVED')} disabled={signal.status === 'RESOLVED'} data-testid="status-resolved-button" />
                </div>
              </Card>
            )}
          </div>

          <div className="col-12 lg:col-4">
            <Card className="text-center shadow-8 mb-4 border-cyan-900" header={
              <div className="pt-4 px-4"><span className="text-xs font-black text-muted uppercase tracking-widest">{t('signals.intel_index')}</span></div>
            } data-testid="priority-score-card">
              <div className="text-7xl font-black text-cyan-400 mb-2 glow-cyan" data-testid="priority-score-value">
                {signal.priorityScore?.toFixed(0)}
              </div>
              <p className="text-muted text-sm font-bold mb-4 uppercase tracking-tighter">{t('signals.priority_rank')}</p>
              
              <Divider className="opacity-10" />
              
              <div className="text-left px-2" role="list" aria-label="Score breakdown details">
                <div className="mb-4" role="listitem">
                  <div className="flex justify-content-between mb-2">
                    <span className="text-xs font-black text-muted uppercase tracking-widest">{t('signals.urgency_factor')}</span>
                    <span className="text-sm font-black text-main">{(signal.scoreBreakdown?.urgency || 0).toFixed(0)} / 150</span>
                  </div>
                  <ProgressBar value={((signal.scoreBreakdown?.urgency || 0) / 150) * 100} showValue={false} style={{ height: '6px' }} color="#06b6d4" />
                </div>
                
                <div className="mb-4" role="listitem">
                  <div className="flex justify-content-between mb-2">
                    <span className="text-xs font-black text-muted uppercase tracking-widest">{t('signals.social_impact')}</span>
                    <span className="text-sm font-black text-main">{(signal.scoreBreakdown?.impact || 0).toFixed(0)} / 125</span>
                  </div>
                  <ProgressBar value={((signal.scoreBreakdown?.impact || 0) / 125) * 100} showValue={false} style={{ height: '6px' }} color="#f59e0b" />
                </div>

                <div className="mb-2" role="listitem">
                  <div className="flex justify-content-between mb-2">
                    <span className="text-xs font-black text-muted uppercase tracking-widest">{t('signals.community_trust')}</span>
                    <span className="text-sm font-black text-main">{(signal.scoreBreakdown?.communityVotes || 0).toFixed(0)} / 15</span>
                  </div>
                  <ProgressBar value={((signal.scoreBreakdown?.communityVotes || 0) / 15) * 100} showValue={false} style={{ height: '6px' }} color="#10b981" />
                </div>
              </div>
            </Card>

            <Card className="shadow-4 border-1 border-white-alpha-10 bg-surface" data-testid="why-ranked-panel">
              <h3 className="text-xs font-black text-cyan-500 uppercase tracking-widest mt-0 mb-3">
                {t('signals.why_ranked_title')}
              </h3>
              <p className="text-sm text-muted mt-0 mb-3 line-height-3">
                {t('signals.why_ranked_desc')}
              </p>
              <div className="flex flex-column gap-3">
                <div className="p-3 border-round bg-card border-1 border-white-alpha-10">
                  <div className="text-xs font-black text-main uppercase tracking-widest">
                    {t('signals.urgency_factor')}
                  </div>
                  <div className="text-xs text-muted mt-1">
                    {t('signals.urgency_formula')}
                  </div>
                </div>
                <div className="p-3 border-round bg-card border-1 border-white-alpha-10">
                  <div className="text-xs font-black text-main uppercase tracking-widest">
                    {t('signals.social_impact')}
                  </div>
                  <div className="text-xs text-muted mt-1">
                    {t('signals.impact_formula')}
                  </div>
                </div>
                <div className="p-3 border-round bg-card border-1 border-white-alpha-10">
                  <div className="text-xs font-black text-main uppercase tracking-widest">
                    {t('signals.affected_estimation')}
                  </div>
                  <div className="text-xs text-muted mt-1">
                    {t('signals.affected_formula')}
                  </div>
                </div>
                <div className="p-3 border-round bg-card border-1 border-white-alpha-10">
                  <div className="text-xs font-black text-main uppercase tracking-widest">
                    {t('signals.community_trust')}
                  </div>
                  <div className="text-xs text-muted mt-1">
                    {t('signals.votes_formula')}
                  </div>
                </div>
              </div>
            </Card>
          </div>
        </div>
      </div>
    </Layout>
  );
}
