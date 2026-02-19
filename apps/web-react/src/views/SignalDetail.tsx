import { useEffect, useState } from "react";
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
import apiClient from "../api/axios";

export function SignalDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { role } = useAuthStore();
  const [signal, setSignal] = useState<Signal | null>(null);
  const [loading, setLoading] = useState(true);
  const [voting, setVoting] = useState(false);

  const fetchSignal = async () => {
    try {
      setLoading(true);
      const res = await apiClient.get(`signals/${id}`);
      if (res.status === 200) {
        setSignal(res.data);
      }
    } catch (err: any) {
      toast.error(err.friendlyMessage || "Signal intelligence data unavailable.");
      navigate("/");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSignal();
  }, [id]);

  const updateStatus = async (newStatus: string) => {
    try {
      const res = await apiClient.patch(`signals/${id}/status`, { status: newStatus });
      if (res.status === 200) {
        toast.success(`Lifecycle updated to ${newStatus}`);
        fetchSignal();
      }
    } catch (err: any) {
      toast.error(err.friendlyMessage || "Lifecycle transition failed.");
    }
  };

  const handleVote = async () => {
    setVoting(true);
    try {
      const res = await apiClient.post(`signals/${id}/vote`);
      if (res.status === 200) {
        toast.success("Community support registered!");
        fetchSignal();
      }
    } catch (err: any) {
      toast.error(err.friendlyMessage || "Vote registration failed.");
    } finally {
      setVoting(false);
    }
  };

  if (loading) return <Layout><div className="p-6"><ProgressBar mode="indeterminate" style={{ height: '6px' }} /></div></Layout>;
  if (!signal) return null;

  const isStaff = role === "PUBLIC_SERVANT" || role === "SUPER_ADMIN";
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
            <Button icon="pi pi-arrow-left" rounded text className="text-gray-400 hover:text-white" onClick={() => navigate('/')} aria-label="Back to dashboard" />
            <div>
              <div className="flex align-items-center gap-2 mb-1">
                <Tag value={signal.status} severity={getStatusSeverity(signal.status)} className="px-2" data-testid="signal-status-tag" />
                <span className="text-xs text-gray-500 font-mono font-bold uppercase tracking-widest">SIGNAL REF: {signal.id.substring(0,8)}</span>
              </div>
              <h1 className="text-4xl font-black text-white m-0 tracking-tight line-height-1" data-testid="signal-title">{signal.title}</h1>
            </div>
          </div>
          <Button 
            label={voting ? "Registering..." : "Support this Issue"} 
            icon="pi pi-heart-fill" 
            className="p-button-primary px-5 py-3 shadow-4 font-bold text-lg bg-red-600 border-none hover:bg-red-500" 
            loading={voting}
            onClick={handleVote} 
            data-testid="support-signal-button"
            aria-label="Support this civic issue"
          />
        </div>

        <div className="grid">
          <div className="col-12 lg:col-8">
            <Card className="mb-4 shadow-6" data-testid="signal-description-card">
              <h3 className="text-gray-500 mb-4 uppercase text-xs font-black tracking-widest">Problem Definition</h3>
              <p className="text-xl line-height-4 m-0 text-gray-100 font-medium" style={{ whiteSpace: 'pre-wrap' }}>
                {signal.description || "No descriptive context provided for this signal."}
              </p>
              
              <Divider className="my-5 opacity-10" />
              
              <div className="grid grid-nogutter">
                <div className="col-12 md:col-6 flex align-items-center gap-3 mb-3">
                  <div className="bg-cyan-900 border-round p-3 flex align-items-center justify-content-center" style={{ width: '3rem', height: '3rem' }}>
                    <i className="pi pi-users text-cyan-400 text-xl"></i>
                  </div>
                  <div>
                    <div className="text-xs text-gray-500 uppercase font-black tracking-widest">Affected Estimation</div>
                    <div className="text-xl font-black text-white" data-testid="affected-people-value">{signal.scoreBreakdown.affectedPeople * 10} Citizens</div>
                  </div>
                </div>
                <div className="col-12 md:col-6 flex align-items-center gap-3 mb-3">
                  <div className="bg-purple-900 border-round p-3 flex align-items-center justify-content-center" style={{ width: '3rem', height: '3rem' }}>
                    <i className="pi pi-tag text-purple-400 text-xl"></i>
                  </div>
                  <div>
                    <div className="text-xs text-gray-500 uppercase font-black tracking-widest">Civic Category</div>
                    <div className="text-xl font-black text-white uppercase tracking-tighter" data-testid="signal-category-value">{signal.category}</div>
                  </div>
                </div>
              </div>
            </Card>

            {isStaff && (
              <Card title={<span className="text-sm font-black uppercase tracking-widest text-cyan-500">Lifecycle Administration</span>} className="border-1 border-dashed border-cyan-800 bg-black-alpha-30 shadow-2" data-testid="staff-operations-card">
                <div className="flex flex-wrap gap-3 mt-2">
                  <Button label="Reset to NEW" outlined severity="info" size="small" className="font-bold border-gray-700" onClick={() => updateStatus('NEW')} disabled={signal.status === 'NEW'} data-testid="status-new-button" />
                  <Button label="Mark IN PROGRESS" severity="warning" size="small" icon="pi pi-bolt" className="font-bold shadow-2" onClick={() => updateStatus('IN_PROGRESS')} disabled={signal.status === 'IN_PROGRESS'} data-testid="status-inprogress-button" />
                  <Button label="Mark RESOLVED" severity="success" size="small" icon="pi pi-check" className="font-bold shadow-2" onClick={() => updateStatus('RESOLVED')} disabled={signal.status === 'RESOLVED'} data-testid="status-resolved-button" />
                </div>
              </Card>
            )}
          </div>

          <div className="col-12 lg:col-4">
            <Card className="text-center shadow-8 mb-4 border-cyan-900" header={
              <div className="pt-4 px-4"><span className="text-xs font-black text-gray-600 uppercase tracking-widest">Intelligence Index</span></div>
            } data-testid="priority-score-card">
              <div className="text-7xl font-black text-cyan-400 mb-2 glow-cyan" data-testid="priority-score-value">
                {signal.priorityScore.toFixed(0)}
              </div>
              <p className="text-gray-400 text-sm font-bold mb-4 uppercase tracking-tighter">Priority Rank</p>
              
              <Divider className="opacity-10" />
              
              <div className="text-left px-2" role="list" aria-label="Score breakdown details">
                <div className="mb-4" role="listitem">
                  <div className="flex justify-content-between mb-2">
                    <span className="text-xs font-black text-gray-500 uppercase tracking-widest">Urgency Factor</span>
                    <span className="text-sm font-black text-gray-200">{signal.scoreBreakdown.urgency} / 150</span>
                  </div>
                  <ProgressBar value={(signal.scoreBreakdown.urgency / 150) * 100} showValue={false} style={{ height: '6px' }} color="#06b6d4" />
                </div>
                
                <div className="mb-4" role="listitem">
                  <div className="flex justify-content-between mb-2">
                    <span className="text-xs font-black text-gray-500 uppercase tracking-widest">Social Impact</span>
                    <span className="text-sm font-black text-gray-200">{signal.scoreBreakdown.impact} / 125</span>
                  </div>
                  <ProgressBar value={(signal.scoreBreakdown.impact / 125) * 100} showValue={false} style={{ height: '6px' }} color="#f59e0b" />
                </div>

                <div className="mb-2" role="listitem">
                  <div className="flex justify-content-between mb-2">
                    <span className="text-xs font-black text-gray-500 uppercase tracking-widest">Community Trust</span>
                    <span className="text-sm font-black text-gray-200">{signal.scoreBreakdown.communityVotes} / 15</span>
                  </div>
                  <ProgressBar value={(signal.scoreBreakdown.communityVotes / 15) * 100} showValue={false} style={{ height: '6px' }} color="#10b981" />
                </div>
              </div>
            </Card>
          </div>
        </div>
      </div>
    </Layout>
  );
}
