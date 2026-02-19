import { useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { Button } from "primereact/button";
import { Tag } from "primereact/tag";
import { Dialog } from "primereact/dialog";
import { InputTextarea } from "primereact/inputtextarea";
import { Layout } from "../components/Layout";
import { Signal } from "../types";
import apiClient from "../api/axios";

export function Moderation() {
  const [flagged, setFlagged] = useState<Signal[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedSignal, setSelectedSignal] = useState<Signal | null>(null);
  const [moderationAction, setModerationAction] = useState<"APPROVE" | "REJECT" | null>(null);
  const [reason, setReason] = useState("");

  const loadFlagged = async () => {
    try {
      setLoading(true);
      const res = await apiClient.get("/api/signals/flagged");
      setFlagged(res.data);
    } catch (err) {
      toast.error("Failed to load flagged signals.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadFlagged();
  }, []);

  const handleModerate = async () => {
    if (!selectedSignal || !moderationAction) return;
    try {
      await apiClient.post(`/api/signals/${selectedSignal.id}/moderate`, {
        action: moderationAction,
        reason
      });
      toast.success(`Signal ${moderationAction === 'APPROVE' ? 'approved' : 'rejected'}`);
      setSelectedSignal(null);
      setReason("");
      loadFlagged();
    } catch (err) {
      toast.error("Moderation failed.");
    }
  };

  const actionTemplate = (rowData: Signal) => {
    return (
      <div className="flex gap-2">
        <Button icon="pi pi-check" severity="success" rounded text onClick={() => { setSelectedSignal(rowData); setModerationAction("APPROVE"); }} />
        <Button icon="pi pi-times" severity="danger" rounded text onClick={() => { setSelectedSignal(rowData); setModerationAction("REJECT"); }} />
      </div>
    );
  };

  return (
    <Layout>
      <div className="page-container animate-fade-in">
        <h1 className="text-4xl font-black mb-2">Moderation <span className="text-orange-500">Queue</span></h1>
        <p className="text-gray-500 mb-5 text-lg">Review community signals flagged by automated intelligence or integrity rules.</p>

        <div className="surface-card border-round-xl border-1 border-white-alpha-10 shadow-8 overflow-hidden">
          <DataTable value={flagged} loading={loading} emptyMessage="No signals pending review." className="p-datatable-lg">
            <Column field="title" header="Flagged Signal" body={(s) => (
              <div className="flex flex-column py-1">
                <span className="font-bold text-gray-100">{s.title}</span>
                <span className="text-xs text-gray-600 uppercase">REF: {s.id.substring(0,8)}</span>
              </div>
            )} />
            <Column field="category" header="Category" />
            <Column header="Priority" body={(s) => <span className="font-bold text-orange-400">{s.priorityScore.toFixed(0)}</span>} />
            <Column header="Moderation Action" body={actionTemplate} style={{ width: '10rem' }} />
          </DataTable>
        </div>

        <Dialog header={`${moderationAction} Signal`} visible={!!selectedSignal} style={{ width: '450px' }} onHide={() => setSelectedSignal(null)} footer={
          <div>
            <Button label="Cancel" text onClick={() => setSelectedSignal(null)} />
            <Button label="Confirm" severity={moderationAction === 'APPROVE' ? 'success' : 'danger'} onClick={handleModerate} autoFocus />
          </div>
        }>
          <div className="flex flex-column gap-3">
            <p className="m-0 text-gray-400">Please provide a reason for this {moderationAction?.toLowerCase()} action.</p>
            <InputTextarea value={reason} onChange={(e) => setReason(e.target.value)} rows={5} placeholder="Internal moderation note..." autoResize />
          </div>
        </Dialog>
      </div>
    </Layout>
  );
}
