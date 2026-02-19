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
      const res = await apiClient.get(`/api/signals/${id}`);
      if (res.status === 200) {
        setSignal(res.data);
      }
    } catch (err) {
      toast.error("Signal not found.");
      navigate("/");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSignal();
  }, [id, navigate]);

  const updateStatus = async (newStatus: string) => {
    try {
      const res = await apiClient.patch(`/api/signals/${id}/status`, { status: newStatus });
      if (res.status === 200) {
        toast.success(`Status updated to ${newStatus}`);
        fetchSignal();
      }
    } catch (err) {
      toast.error("Failed to update status.");
    }
  };

  const handleVote = async () => {
    setVoting(true);
    try {
      const res = await apiClient.post(`/api/signals/${id}/vote`);
      if (res.status === 200) {
        toast.success("Support registered!");
        fetchSignal();
      }
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Error occurred.");
    } finally {
      setVoting(false);
    }
  };

  if (loading) return <Layout><div className="p-6"><ProgressBar mode="indeterminate" style={{ height: '6px' }} /></div></Layout>;
  if (!signal) return null;

  const isStaff = role === "PUBLIC_SERVANT" || role === "SUPER_ADMIN";
  const getStatusSeverity = (s: string) => s === 'NEW' ? 'info' : s === 'IN_PROGRESS' ? 'warning' : 'success';

  return (
    <Layout>
      <div className="animate-fade-in pb-6">
        <div className="flex flex-column md:flex-row align-items-start md:align-items-center justify-content-between mb-5 gap-3">
          <div className="flex align-items-center gap-3">
            <Button icon="pi pi-arrow-left" rounded text className="text-gray-400" onClick={() => navigate('/')} />
            <div>
              <div className="flex align-items-center gap-2 mb-1">
                <Tag value={signal.status} severity={getStatusSeverity(signal.status)} />
                <span className="text-xs text-gray-600 font-mono uppercase tracking-widest">ID: {signal.id}</span>
              </div>
              <h1 className="text-4xl font-black m-0 tracking-tight">{signal.title}</h1>
            </div>
          </div>
          <Button 
            label={voting ? "Voting..." : "Support this Issue"} 
            icon="pi pi-heart-fill" 
            className="bg-red-500 border-none px-5 py-3 shadow-4 font-bold text-lg" 
            loading={voting}
            onClick={handleVote} 
          />
        </div>

        <div className="grid">
          <div className="col-12 lg:col-8">
            <Card className="mb-4 shadow-6">
              <h3 className="text-gray-500 mb-4 uppercase text-xs font-black tracking-widest">Description & Context</h3>
              <p className="text-xl line-height-4 m-0 text-gray-100" style={{ whiteSpace: 'pre-wrap' }}>
                {signal.description || "Detailed analysis pending for this community signal."}
              </p>
              
              <Divider className="my-5 opacity-10" />
              
              <div className="grid grid-nogutter">
                <div className="col-12 md:col-6 flex align-items-center gap-3 mb-3">
                  <div className="bg-cyan-900 border-round p-2"><i className="pi pi-users text-cyan-400"></i></div>
                  <div>
                    <div className="text-xs text-gray-600 uppercase font-bold">Affected People</div>
                    <div className="text-lg font-bold">{signal.scoreBreakdown.affectedPeople * 10} Estimated</div>
                  </div>
                </div>
                <div className="col-12 md:col-6 flex align-items-center gap-3 mb-3">
                  <div className="bg-purple-900 border-round p-2"><i className="pi pi-tag text-purple-400"></i></div>
                  <div>
                    <div className="text-xs text-gray-600 uppercase font-bold">Category</div>
                    <div className="text-lg font-bold uppercase tracking-tight">{signal.category}</div>
                  </div>
                </div>
              </div>
            </Card>

            {isStaff && (
              <Card title={<span className="text-sm uppercase tracking-widest text-cyan-500">Lifecycle Operations</span>} className="border-1 border-dashed border-cyan-800 bg-black-alpha-20 shadow-2">
                <div className="flex flex-wrap gap-3 mt-2">
                  <Button label="Set to NEW" outlined severity="info" size="small" onClick={() => updateStatus('NEW')} disabled={signal.status === 'NEW'} />
                  <Button label="Mark IN PROGRESS" severity="warning" size="small" icon="pi pi-bolt" onClick={() => updateStatus('IN_PROGRESS')} disabled={signal.status === 'IN_PROGRESS'} />
                  <Button label="Mark RESOLVED" severity="success" size="small" icon="pi pi-check" onClick={() => updateStatus('RESOLVED')} disabled={signal.status === 'RESOLVED'} />
                </div>
              </Card>
            )}
          </div>

          <div className="col-12 lg:col-4">
            <Card className="text-center shadow-8 mb-4 border-cyan-900" header={
              <div className="pt-4 px-4"><span className="text-xs font-black text-gray-600 uppercase tracking-widest">Algorithmic Rank</span></div>
            }>
              <div className="text-7xl font-black text-cyan-400 mb-2 glow-cyan">
                {signal.priorityScore.toFixed(0)}
              </div>
              <p className="text-gray-500 text-sm mb-4">Priority Intelligence Index</p>
              
              <Divider className="opacity-10" />
              
              <div className="text-left px-2">
                <div className="mb-4">
                  <div className="flex justify-content-between mb-2">
                    <span className="text-xs font-bold text-gray-500 uppercase">Urgency</span>
                    <span className="text-sm font-bold text-gray-200">{signal.scoreBreakdown.urgency} / 150</span>
                  </div>
                  <ProgressBar value={(signal.scoreBreakdown.urgency / 150) * 100} showValue={false} style={{ height: '6px' }} />
                </div>
                
                <div className="mb-4">
                  <div className="flex justify-content-between mb-2">
                    <span className="text-xs font-bold text-gray-500 uppercase">Social Impact</span>
                    <span className="text-sm font-bold text-gray-200">{signal.scoreBreakdown.impact} / 125</span>
                  </div>
                  <ProgressBar value={(signal.scoreBreakdown.impact / 125) * 100} showValue={false} severity="warning" style={{ height: '6px' }} />
                </div>

                <div className="mb-2">
                  <div className="flex justify-content-between mb-2">
                    <span className="text-xs font-bold text-gray-500 uppercase">Community Trust</span>
                    <span className="text-sm font-bold text-gray-200">{signal.scoreBreakdown.communityVotes} / 15</span>
                  </div>
                  <ProgressBar value={(signal.scoreBreakdown.communityVotes / 15) * 100} showValue={false} severity="success" style={{ height: '6px' }} />
                </div>
              </div>
            </Card>
          </div>
        </div>
      </div>
    </Layout>
  );
}
