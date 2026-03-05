import { useEffect, useState, useCallback } from "react";
import { toast } from "react-hot-toast";
import { Signal } from "../types";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { useTranslation } from "react-i18next";
import { Layout } from "../components/Layout";
import apiClient from "../api/axios";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";

interface ApiError extends Error {
  friendlyMessage?: string;
}

export function Moderation() {
  const { t } = useTranslation();
  const [flagged, setFlagged] = useState<Signal[]>([]);
  const [loading, setLoading] = useState(true);

  const loadFlagged = useCallback(async () => {
    try {
      setLoading(true);
      const res = await apiClient.get("signals/flagged");
      setFlagged(res.data.content || []);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('moderation.load_error'));
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    loadFlagged();
  }, [loadFlagged]);      

  const handleAction = async (id: string, action: 'APPROVE' | 'REJECT') => {
    try {
      await apiClient.post(`signals/${id}/moderate`, {
        action,
        reason: action === 'APPROVE' ? 'Identity and signal context verified.' : 'Protocol violation: flagged for review.'
      });
      toast.success(`Protocol ${action.toLowerCase()} complete`);
      loadFlagged();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || t('moderation.action_error'));
    }
  };

  return (
    <Layout>
      <div className="animate-fade-up">
        <div className="flex flex-column md:flex-row justify-content-between align-items-start md:align-items-center mb-10 gap-4">
          <div>
            <h1 className="text-5xl font-black mb-2 text-main tracking-tighter uppercase">Triage Terminal</h1>
            <p className="text-secondary text-lg font-medium opacity-70">Securing community data integrity via high-level review.</p>
          </div>
          
          <div className="flex gap-4">
            <div className="flex flex-column align-items-end">
              <span className="text-xs font-black text-muted uppercase tracking-widest">Queue Status</span>
              <span className="text-2xl font-black text-brand-primary tabular-nums">{flagged.length} Pending</span>
            </div>
            <CivicButton icon="pi pi-sync" variant="ghost" onClick={loadFlagged} className="p-3" />
          </div>
        </div>

        <CivicCard padding="none">
          <DataTable 
            value={flagged} 
            loading={loading} 
            emptyMessage={
              <CivicEmptyState 
                icon="pi-shield"
                title="Integrity Verified"
                description="The moderation queue is currently clear. All signals have been triaged."
              />
            }
            className="p-datatable-responsive"
          >
            <Column 
              header="Operational Context" 
              className="pl-6 py-5"
              body={(s: Signal) => (
                <div className="flex flex-column gap-1">
                  <span className="font-bold text-main text-base">{s.title}</span>
                  <span className="text-min font-mono text-muted uppercase tracking-tighter">SIG-REF: {s.id.substring(0,8)}</span>
                </div>
              )} 
            />
            <Column 
              header="Sector" 
              body={(s: Signal) => <CivicBadge label={s.category} type="category" />} 
            />
            <Column 
              header="Calculated Index" 
              body={(s: Signal) => <span className="font-black text-brand-primary text-xl tabular-nums">{s.priorityScore?.toFixed(0)}</span>} 
            />
            <Column 
              header="Decision Protocol" 
              className="pr-6"
              body={(s: Signal) => (
                <div className="flex gap-3 justify-content-end">
                  <CivicButton 
                    icon="pi pi-check-circle" 
                    label="Authorize" 
                    className="text-xs py-3"
                    onClick={() => handleAction(s.id, 'APPROVE')} 
                  />
                  <CivicButton 
                    icon="pi pi-ban" 
                    variant="danger" 
                    label="Sanitize" 
                    className="text-xs py-3"
                    onClick={() => handleAction(s.id, 'REJECT')} 
                  />
                </div>
              )} 
            />
          </DataTable>
        </CivicCard>
      </div>
    </Layout>
  );
}
