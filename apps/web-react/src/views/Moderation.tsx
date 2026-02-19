import { useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { Button } from "primereact/button";
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
      // UX-001: Standardized path
      const res = await apiClient.get("signals/flagged");
      setFlagged(res.data);
    } catch (err: any) {
      toast.error(err.friendlyMessage || "Failed to load queue.");
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
      // UX-001: Standardized path
      await apiClient.post(`signals/${selectedSignal.id}/moderate`, {
        action: moderationAction,
        reason
      });
      toast.success(`Signal ${moderationAction === 'APPROVE' ? 'approved' : 'rejected'}`);
      setSelectedSignal(null);
      setReason("");
      loadFlagged();
    } catch (err: any) {
      toast.error(err.friendlyMessage || "Moderation failed.");
    }
  };

  const actionTemplate = (rowData: Signal) => {
    return (
      <div className="flex gap-2">
        <Button 
          icon="pi pi-check" 
          severity="success" 
          rounded 
          className="bg-green-600 border-none hover:bg-green-500 shadow-2"
          onClick={() => { setSelectedSignal(rowData); setModerationAction("APPROVE"); }} 
        />
        <Button 
          icon="pi pi-times" 
          severity="danger" 
          rounded 
          className="bg-red-600 border-none hover:bg-red-500 shadow-2"
          onClick={() => { setSelectedSignal(rowData); setModerationAction("REJECT"); }} 
        />
      </div>
    );
  };

  return (
    <Layout>
      <div className="animate-fade-in">
        <h1 className="text-4xl font-black mb-2">Moderation <span className="text-orange-500">Queue</span></h1>
        <p className="text-gray-400 mb-5 text-lg font-medium">Review community signals flagged by algorithmic integrity rules.</p>

        <div className="surface-card border-round-xl border-1 border-white-alpha-10 shadow-8 overflow-hidden">
          <DataTable value={flagged} loading={loading} emptyMessage={
            <div className="p-6 text-center text-gray-500 font-bold uppercase tracking-widest">
              No signals pending review
            </div>
          } className="p-datatable-lg">
            <Column field="title" header="Flagged Signal" body={(s) => (
              <div className="flex flex-column py-1">
                <span className="font-bold text-gray-100">{s.title}</span>
                <span className="text-xs text-gray-500 font-mono uppercase">REF: {s.id.substring(0,8)}</span>
              </div>
            )} />
            <Column field="category" header="Category" body={(s) => <span className="text-gray-400 font-bold text-xs uppercase">{s.category}</span>} />
            <Column header="Priority" body={(s) => <span className="font-bold text-orange-400 text-lg">{s.priorityScore.toFixed(0)}</span>} />
            <Column header="Moderation Action" body={actionTemplate} style={{ width: '10rem' }} />
          </DataTable>
        </div>

        <Dialog header={`${moderationAction} Signal`} visible={!!selectedSignal} style={{ width: '100%', maxWidth: '450px' }} onHide={() => setSelectedSignal(null)} footer={
          <div className="flex gap-2 justify-content-end p-3">
            <Button label="Cancel" text className="text-gray-400 font-bold" onClick={() => setSelectedSignal(null)} />
            <Button 
              label="Execute Action" 
              className={`p-button-primary font-bold ${moderationAction === 'APPROVE' ? 'bg-green-600' : 'bg-red-600'}`}
              onClick={handleModerate} 
            />
          </div>
        }>
          <div className="flex flex-column gap-3 p-2">
            <p className="m-0 text-gray-300 font-medium">Provide an internal justification for this moderation decision.</p>
            <InputTextarea value={reason} onChange={(e) => setReason(e.target.value)} rows={5} placeholder="Moderation reasoning..." autoResize className="bg-gray-900 border-gray-800" />
          </div>
        </Dialog>
      </div>
    </Layout>
  );
}
