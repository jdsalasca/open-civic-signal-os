import { useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { Signal } from "../types";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { Tag } from "primereact/tag";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { Layout } from "../components/Layout";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";

export function MySignals() {
  const { t } = useTranslation();
  const [signals, setSignals] = useState<Signal[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchMySignals = async () => {
      try {
        const res = await apiClient.get("signals/mine");
        setSignals(res.data);
      } catch (err) {
        toast.error(t('common.error'));
      } finally {
        setLoading(false);
      }
    };
    fetchMySignals();
  }, [t]);

  const statusTemplate = (rowData: Signal) => {
    const severity = rowData.status === "NEW" ? "info" : 
                    rowData.status === "IN_PROGRESS" ? "warning" : 
                    rowData.status === "RESOLVED" ? "success" : "danger";
    return <Tag value={rowData.status} severity={severity} />;
  };

  return (
    <Layout>
      <div className="animate-fade-in page-container">
        <div className="flex align-items-center justify-content-between mb-5">
          <div>
            <h1 className="text-4xl font-black m-0 text-main tracking-tight">{t('my_contributions.title')}</h1>
            <p className="text-muted mt-2">{t('my_contributions.desc')}</p>
          </div>
          <Button label={t('dashboard.new_issue')} icon="pi pi-plus" className="p-button-primary shadow-4" onClick={() => navigate("/report")} />
        </div>

        <div className="grid">
          <div className="col-12 md:col-4">
            <Card className="bg-cyan-900 border-none shadow-4 h-full">
              <div className="flex align-items-center gap-3">
                <i className="pi pi-file text-4xl text-cyan-400"></i>
                <div>
                  <span className="block text-cyan-200 font-bold uppercase text-xs tracking-widest">{t('my_contributions.total_reports')}</span>
                  <span className="text-4xl font-black text-white">{signals.length}</span>
                </div>
              </div>
            </Card>
          </div>
          <div className="col-12 md:col-4">
            <Card className="bg-indigo-900 border-none shadow-4 h-full">
              <div className="flex align-items-center gap-3">
                <i className="pi pi-check-circle text-4xl text-indigo-300"></i>
                <div>
                  <span className="block text-indigo-100 font-bold uppercase text-xs tracking-widest">{t('my_contributions.resolved')}</span>
                  <span className="text-4xl font-black text-white">{signals.filter(s => s.status === 'RESOLVED').length}</span>
                </div>
              </div>
            </Card>
          </div>
          <div className="col-12 md:col-4">
            <Card className="bg-surface border-1 border-white-alpha-10 shadow-4 h-full">
              <div className="flex align-items-center gap-3">
                <i className="pi pi-heart text-4xl text-pink-500"></i>
                <div>
                  <span className="block text-muted font-bold uppercase text-xs tracking-widest">{t('my_contributions.community_votes')}</span>
                  <span className="text-4xl font-black text-main">
                    {signals.reduce((acc, s) => acc + (s.scoreBreakdown?.communityVotes || 0), 0)}
                  </span>
                </div>
              </div>
            </Card>
          </div>
        </div>

        <div className="surface-card border-round-xl border-1 border-white-alpha-10 shadow-8 overflow-hidden mt-4">
          <DataTable value={signals} loading={loading} emptyMessage={t('my_contributions.empty')} className="p-datatable-lg">
            <Column field="title" header={t('common.title')} body={(s) => <span className="font-bold text-main">{s.title}</span>} />
            <Column field="category" header={t('common.category')} body={(s) => t(`categories.${s.category}`)} />
            <Column header={t('common.status')} body={statusTemplate} />
            <Column field="priorityScore" header={t('common.score')} body={(s) => s.priorityScore.toFixed(0)} />
            <Column body={(s) => (
              <Button icon="pi pi-arrow-right" text rounded onClick={() => navigate(`/signal/${s.id}`)} aria-label={t('signals.view_details')} />
            )} />
          </DataTable>
        </div>
      </div>
    </Layout>
  );
}
