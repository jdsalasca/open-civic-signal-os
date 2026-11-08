import { useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { Signal } from "../types";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { Skeleton } from "primereact/skeleton";
import { Layout } from "../components/Layout";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import apiClient from "../api/axios";
import { CivicCard } from "../components/ui/CivicCard";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicEmptyState } from "../components/ui/CivicEmptyState";
import { CivicActionBar } from "../components/ui/CivicActionBar";
import { CivicPageHeader } from "../components/ui/CivicPageHeader";
import { PriorityScale, StatusChip } from "../components/signalPrimitives";
import { useSignalStatusLabel } from "../utils/signalStatus";

/**
 * My contributions, in the same visual language as the central signal surfaces: the big number
 * first, then the same priority scale and lifecycle chip used in the dashboard feed, so a report
 * looks the same here as it does in the backlog.
 */
export function MySignals() {
  const { t } = useTranslation();
  const statusLabel = useSignalStatusLabel();
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

  const totalVotes = signals.reduce((acc, s) => acc + (s.communityVotes || 0), 0);
  const resolvedCount = signals.filter((s) => s.status === "RESOLVED").length;

  const summary = [
    { id: "total", label: t("my_contributions.total_reports"), value: signals.length, support: t("my_contributions.total_support") },
    { id: "resolved", label: t("my_contributions.resolved"), value: resolvedCount, support: t("my_contributions.resolved_support") },
    { id: "votes", label: t("my_contributions.community_votes"), value: totalVotes, support: t("my_contributions.votes_support") },
  ];

  const statusTemplate = (rowData: Signal) =>
    loading ? (
      <Skeleton width="5rem" height="1.5rem" />
    ) : (
      <StatusChip status={rowData.status} label={statusLabel(rowData.status)} />
    );

  const scoreTemplate = (rowData: Signal) =>
    loading ? <Skeleton width="5rem" /> : <PriorityScale score={rowData.priorityScore} compact />;

  const titleTemplate = (rowData: Signal) =>
    loading ? (
      <div className="flex flex-column gap-2">
        <Skeleton width="12rem" />
        <Skeleton width="6rem" height="0.5rem" />
      </div>
    ) : (
      <div className="flex flex-column min-w-0">
        <span className="font-bold text-main group-hover:text-brand-primary transition-colors">{rowData.title}</span>
        <span className="text-xs text-muted font-semibold mt-1">
          {t("signals.protocol_id")}: {rowData.id.substring(0, 8)}
        </span>
      </div>
    );

  const categoryTemplate = (rowData: Signal) =>
    loading ? (
      <Skeleton width="5rem" />
    ) : (
      <span className="signal-status-chip" data-status={rowData.category}>
        <span className="signal-status-chip__dot" aria-hidden="true" style={{ background: "var(--chart-4)" }} />
        <span>{t(`categories.${rowData.category}`)}</span>
      </span>
    );

  return (
    <Layout>
      <div className="animate-fade-up motion-page">
        <div className="flex flex-column md:flex-row align-items-start md:align-items-center justify-content-between mb-8 gap-4">
          <CivicPageHeader title={t('my_contributions.title')} description={t('my_contributions.desc')} className="mb-0" />
          <CivicActionBar className="ml-auto">
            <CivicButton label={t('dashboard.new_issue')} icon="pi pi-plus" onClick={() => navigate("/report")} glow />
            <CivicButton label={t('nav.dialogues')} icon="pi pi-comments" variant="secondary" onClick={() => navigate("/communities/threads")} />
          </CivicActionBar>
        </div>

        <div className="signal-metric-grid mb-6" role="region" aria-label={t("metrics.region_label")}>
          {summary.map((metric) => (
            <article key={metric.id} className="signal-metric-card" data-testid={`mine-metric-${metric.id}`}>
              <div className="signal-metric-card__top">
                <span className="signal-metric-card__label">{metric.label}</span>
              </div>
              <div className="signal-metric-card__value">{loading ? "—" : metric.value}</div>
              <div className="signal-metric-card__support">{metric.support}</div>
            </article>
          ))}
        </div>

        <CivicCard padding="none" className="signal-table-shell" data-testid="mine-signals-table">
          <DataTable
            value={signals}
            loading={loading}
            emptyMessage={
              <CivicEmptyState
                icon="pi-file-edit"
                title={t('my_contributions.empty_title')}
                description={t('my_contributions.empty_desc')}
                actionLabel={t('my_contributions.empty_action')}
                onAction={() => navigate("/report")}
              />
            }
            onRowClick={(e) => navigate(`/signal/${e.data.id}`)}
            rowClassName={() => 'cursor-pointer group'}
            className="p-datatable-sm"
          >
            <Column
              header={t('common.title')}
              body={titleTemplate}
              data-label={t('common.title')}
            />
            <Column
              field="category"
              header={t('common.category')}
              body={categoryTemplate}
              data-label={t('common.category')}
            />
            <Column header={t('common.status')} body={statusTemplate} data-label={t('common.status')} />
            <Column header={t('common.score')} body={scoreTemplate} data-label={t('common.score')} />
            <Column
              className="signal-table-cell--action"
              data-label=""
              body={() => <i className="pi pi-chevron-right text-muted group-hover:text-brand-primary group-hover:translate-x-1 transition-all" aria-hidden="true" />}
            />
          </DataTable>
        </CivicCard>
      </div>
    </Layout>
  );
}
