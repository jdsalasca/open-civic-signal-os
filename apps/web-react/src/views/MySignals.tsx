import { useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { Signal } from "../types";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { Layout } from "../components/Layout";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicBadge } from "../components/ui/CivicBadge";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";

export function MySignals() {
  const { t } = useTranslation();
  const [signals, setSignals] = useState<Signal[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchMySignals = async () => {
      try {
        const res = await apiClient.get("signals/mine");
        setSignals(res.data.content || res.data); // Handle both paginated and list
      } catch (err) {
        toast.error(t('common.error'));
      } finally {
        setLoading(false);
      }
    };
    fetchMySignals();
  }, [t]);

  const statusTemplate = (rowData: Signal) => {
    let severity: 'new' | 'progress' | 'resolved' | 'rejected' = 'new';
    if (rowData.status === "IN_PROGRESS") severity = 'progress';
    if (rowData.status === "RESOLVED") severity = 'resolved';
    if (rowData.status === "REJECTED") severity = 'rejected';
    return <CivicBadge label={rowData.status} severity={severity} />;
  };

  const totalVotes = signals.reduce((acc, s) => acc + (s.communityVotes || 0), 0);

  return (
    <Layout>
      <div className="animate-fade-up">
        <div className="flex flex-column md:flex-row align-items-start md:align-items-center justify-content-between mb-8 gap-4">
          <div>
            <h1 className="text-5xl font-black m-0 text-main tracking-tighter">{t('my_contributions.title')}</h1>
            <p className="text-secondary text-lg font-medium mt-2">{t('my_contributions.desc')}</p>
          </div>
          <CivicButton label={t('dashboard.new_issue')} icon="pi pi-plus" onClick={() => navigate("/report")} glow />
        </div>

        <div className="grid mb-8">
          <div className="col-12 md:col-4">
            <CivicCard variant="brand" className="h-full">
              <div className="flex align-items-center gap-4">
                <div className="bg-brand-primary-alpha-10 p-3 border-round-xl">
                  <i className="pi pi-file text-3xl text-brand-primary"></i>
                </div>
                <div>
                  <span className="block text-muted font-bold uppercase text-xs tracking-widest mb-1">{t('my_contributions.total_reports')}</span>
                  <span className="text-4xl font-black text-main">{signals.length}</span>
                </div>
              </div>
            </CivicCard>
          </div>
          <div className="col-12 md:col-4">
            <CivicCard variant="success" className="h-full">
              <div className="flex align-items-center gap-4">
                <div className="bg-status-resolved-alpha-10 p-3 border-round-xl">
                  <i className="pi pi-check-circle text-3xl text-status-resolved"></i>
                </div>
                <div>
                  <span className="block text-muted font-bold uppercase text-xs tracking-widest mb-1">{t('my_contributions.resolved')}</span>
                  <span className="text-4xl font-black text-main">{signals.filter(s => s.status === 'RESOLVED').length}</span>
                </div>
              </div>
            </CivicCard>
          </div>
          <div className="col-12 md:col-4">
            <CivicCard className="h-full">
              <div className="flex align-items-center gap-4">
                <div className="bg-white-alpha-5 p-3 border-round-xl">
                  <i className="pi pi-heart text-3xl text-status-rejected"></i>
                </div>
                <div>
                  <span className="block text-muted font-bold uppercase text-xs tracking-widest mb-1">{t('my_contributions.community_votes')}</span>
                  <span className="text-4xl font-black text-main">{totalVotes}</span>
                </div>
              </div>
            </CivicCard>
          </div>
        </div>

        <CivicCard padding="none">
          <DataTable 
            value={signals} 
            loading={loading} 
            emptyMessage={
              <CivicEmptyState 
                icon="pi-file-edit"
                title="No Contributions Found"
                description="Your operational history is currently empty. Start by reporting a new civic signal."
                actionLabel="Report First Issue"
                onAction={() => navigate("/report")}
              />
            }
            onRowClick={(e) => navigate(`/signal/${e.data.id}`)}
            rowClassName={() => 'cursor-pointer group'}
            className="p-datatable-responsive"
          >
            <Column 
              header={t('common.title')} 
              className="pl-5 py-4"
              body={(s: Signal) => (
                <div className="flex flex-column">
                  <span className="font-bold text-main group-hover:text-brand-primary transition-colors">{s.title}</span>
                  <span className="text-xs text-muted font-mono mt-1">{s.id.substring(0,8)}</span>
                </div>
              )} 
            />
            <Column 
              field="category" 
              header={t('common.category')} 
              body={(s: Signal) => <CivicBadge label={t(`categories.${s.category}`)} type="category" />} 
            />
            <Column header={t('common.status')} body={statusTemplate} />
            <Column field="priorityScore" header={t('common.score')} body={(s: Signal) => <span className="font-black text-main">{s.priorityScore?.toFixed(0)}</span>} />
            <Column 
              className="pr-5 text-right"
              body={() => <i className="pi pi-chevron-right text-muted group-hover:text-brand-primary group-hover:translate-x-1 transition-all" />} 
            />
          </DataTable>
        </CivicCard>
      </div>
    </Layout>
  );
}
