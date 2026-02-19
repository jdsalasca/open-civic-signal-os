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
import apiClient from "../api/axios";

export function SignalDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { role, isLoggedIn } = useAuthStore();
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
    if (!isLoggedIn) {
      toast.error("Please login to support issues.");
      navigate("/login");
      return;
    }
    setVoting(true);
    try {
      const res = await apiClient.post(`/api/signals/${id}/vote`);
      if (res.status === 200) {
        toast.success("Support registered! Priority increased.");
        fetchSignal();
      }
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Already voted or error occurred.");
    } finally {
      setVoting(false);
    }
  };

  if (loading) return <ProgressBar mode="indeterminate" style={{ height: '6px' }} />;
  if (!signal) return null;

  const isStaff = role === "PUBLIC_SERVANT" || role === "SUPER_ADMIN";

  const getStatusSeverity = (s: string) => s === 'NEW' ? 'info' : s === 'IN_PROGRESS' ? 'warning' : 'success';

  return (
    <div className="form-container" style={{ maxWidth: '900px' }}>
      <div className="flex align-items-center justify-content-between mb-4">
        <div className="flex align-items-center gap-3">
          <Button icon="pi pi-arrow-left" rounded text onClick={() => navigate('/')} />
          <h1 className="text-3xl m-0">{signal.title}</h1>
          <Tag value={signal.status} severity={getStatusSeverity(signal.status)} />
        </div>
        <Button 
          label="Support Issue" 
          icon="pi pi-heart-fill" 
          className="bg-red-500 border-none px-4" 
          loading={voting}
          onClick={handleVote} 
        />
      </div>

      <div className="grid">
        <div className="col-12 lg:col-8">
          <Card className="mb-4 shadow-3">
            <h3 className="text-gray-500 mb-3 uppercase text-xs tracking-widest">Problem Description</h3>
            <p className="text-lg line-height-3 m-0" style={{ whiteSpace: 'pre-wrap' }}>
              {signal.description || "No detailed description provided."}
            </p>
            
            <Divider />
            
            <h3 className="text-gray-500 mb-3 uppercase text-xs tracking-widest">Impact Details</h3>
            <div className="flex flex-wrap gap-4 mt-2">
              <div className="flex align-items-center gap-2">
                <i className="pi pi-users text-cyan-400"></i>
                <span><strong>{signal.scoreBreakdown.affectedPeople}</strong> people affected</span>
              </div>
              <div className="flex align-items-center gap-2">
                <i className="pi pi-tag text-purple-400"></i>
                <span>Category: <strong>{signal.category}</strong></span>
              </div>
              <div className="flex align-items-center gap-2">
                <i className="pi pi-heart text-red-400"></i>
                <span>Community Support: <strong>{signal.scoreBreakdown.communityVotes} votes</strong></span>
              </div>
            </div>
          </Card>

          {isStaff && (
            <Card title="Management Operations" className="border-1 border-dashed border-cyan-800 bg-gray-900 shadow-2">
              <p className="text-sm text-gray-500 mb-4">Transition this issue through the resolution lifecycle.</p>
              <div className="flex gap-2">
                <Button label="New" outlined size="small" onClick={() => updateStatus('NEW')} disabled={signal.status === 'NEW'} />
                <Button label="In Progress" severity="warning" size="small" icon="pi pi-bolt" onClick={() => updateStatus('IN_PROGRESS')} disabled={signal.status === 'IN_PROGRESS'} />
                <Button label="Resolved" severity="success" size="small" icon="pi pi-check" onClick={() => updateStatus('RESOLVED')} disabled={signal.status === 'RESOLVED'} />
              </div>
            </Card>
          )}
        </div>

        <div className="col-12 lg:col-4">
          <Card title="Priority Score" className="text-center shadow-3 mb-4">
            <div className="text-6xl font-black text-cyan-400 mb-2">
              {signal.priorityScore.toFixed(0)}
            </div>
            <p className="text-gray-500 text-xs uppercase tracking-wider">Algorithmic Rank</p>
            <Divider />
            <div className="text-left">
              <div className="flex justify-content-between mb-2">
                <span className="text-sm">Urgency</span>
                <span className="font-bold">{signal.scoreBreakdown.urgency}</span>
              </div>
              <ProgressBar value={(signal.scoreBreakdown.urgency / 150) * 100} showValue={false} style={{ height: '4px' }} />
              
              <div className="flex justify-content-between mb-2 mt-3">
                <span className="text-sm">Social Impact</span>
                <span className="font-bold">{signal.scoreBreakdown.impact}</span>
              </div>
              <ProgressBar value={(signal.scoreBreakdown.impact / 125) * 100} showValue={false} severity="warning" style={{ height: '4px' }} />

              <div className="flex justify-content-between mb-2 mt-3">
                <span className="text-sm">Community Votes</span>
                <span className="font-bold">{signal.scoreBreakdown.communityVotes}</span>
              </div>
              <ProgressBar value={(signal.scoreBreakdown.communityVotes / 15) * 100} showValue={false} severity="success" style={{ height: '4px' }} />
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
