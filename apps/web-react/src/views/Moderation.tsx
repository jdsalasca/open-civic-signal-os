import { useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { Signal } from "../types";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { Tag } from "primereact/tag";
import { useTranslation } from "react-i18next";
import { Layout } from "../components/Layout";
import apiClient from "../api/axios";

interface ApiError extends Error {
  friendlyMessage?: string;
}

export function Moderation() {
  const { t } = useTranslation();
  const [flagged, setFlagged] = useState<Signal[]>([]);
  const [loading, setLoading] = useState(true);

  const loadFlagged = async () => {
    try {
      setLoading(true);
      const res = await apiClient.get("signals/flagged");
      // P1-B: Handle paginated response structure
      setFlagged(res.data.content || []);
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || "Failed to load moderation queue.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadFlagged();
  }, []);

  const handleAction = async (id: string, action: 'APPROVE' | 'REJECT') => {
    try {
      await apiClient.post(`signals/${id}/moderate`, {
        action,
        reason: action === 'APPROVE' ? 'Manually reviewed and approved.' : 'Rejected due to community guidelines violation.'
      });
      toast.success(t('moderation.success', { action: action.toLowerCase() }));
      loadFlagged();
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || "Moderation action failed.");
    }
  };

  return (
    <Layout>
      <div className="animate-fade-in">
        <h1 className="text-4xl font-black mb-2 text-white">{t('moderation.title')}</h1>
        <p className="text-gray-500 mb-5">{t('moderation.desc')}</p>

        <Card className="shadow-8 border-1 border-white-alpha-10 bg-gray-900 overflow-hidden">
          <DataTable value={flagged} loading={loading} emptyMessage={t('moderation.empty')} className="p-datatable-sm">
            <Column field="title" header={t('moderation.suspect_title')} body={(s) => <span className="font-bold">{s.title}</span>} />
            <Column field="category" header={t('common.category')} body={(s) => <Tag value={s.category} severity="secondary" />} />
            <Column field="priorityScore" header={t('common.score')} body={(s) => <span className="font-mono text-cyan-400 font-bold">{s.priorityScore?.toFixed(0)}</span>} />
            <Column header={t('common.actions')} body={(s) => (
              <div className="flex gap-2">
                <Button icon="pi pi-check" severity="success" size="small" label={t('moderation.approve')} onClick={() => handleAction(s.id, 'APPROVE')} />
                <Button icon="pi pi-times" severity="danger" size="small" label={t('moderation.reject')} onClick={() => handleAction(s.id, 'REJECT')} />
              </div>
            )} />
          </DataTable>
        </Card>
      </div>
    </Layout>
  );
}
