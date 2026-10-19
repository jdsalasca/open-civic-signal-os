import { useState, useMemo } from "react";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { InputText } from "primereact/inputtext";
import { Skeleton } from "primereact/skeleton";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { ExplainabilityFactor, Signal } from "../types";
import { CivicButton } from "./ui/CivicButton";
import { PriorityScale, SignalEmpty, StatusChip } from "./signalPrimitives";
import { useSignalStatusLabel } from "../utils/signalStatus";

type Props = {
  signals: Signal[];
  loading: boolean;
  totalRecords?: number;
  rows?: number;
  first?: number;
  onPage?: (event: any) => void;
  /** Backend threshold used to align the visual priority scale with the CRITICAL filter. */
  criticalThreshold?: number;
};

interface SkeletonRow {
  _skeleton: boolean;
  id?: string;
}

export function SignalTable({ signals, loading, totalRecords, rows, first, onPage, criticalThreshold = 0 }: Props) {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const statusLabel = useSignalStatusLabel();
  const [globalFilterValue, setGlobalFilterValue] = useState("");

  const filteredSignals = useMemo(() => {
    if (!globalFilterValue.trim()) {
      return signals;
    }
    const query = globalFilterValue.trim().toLowerCase();
    return signals.filter((signal) => {
      const idPart = signal.id?.slice(0, 8).toLowerCase() ?? "";
      return (
        signal.title.toLowerCase().includes(query) ||
        signal.category.toLowerCase().includes(query) ||
        signal.status.toLowerCase().includes(query) ||
        idPart.includes(query)
      );
    });
  }, [signals, globalFilterValue]);

  const tableData = useMemo<(Signal | SkeletonRow)[]>(() => {
    if (loading) {
      return new Array(rows || 6).fill(null).map((_, i) => ({ _skeleton: true, id: `sk-${i}` }));
    }
    return filteredSignals;
  }, [filteredSignals, loading, rows]);

  const onGlobalFilterChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setGlobalFilterValue(e.target.value);
  };

  const tableTotalRecords = globalFilterValue.trim() ? filteredSignals.length : totalRecords;

  const statusTemplate = (rowData: Signal | SkeletonRow) => {
    if ('_skeleton' in rowData) return <Skeleton width="4rem" height="1.5rem" />;
    return <StatusChip status={rowData.status} label={statusLabel(rowData.status)} />;
  };

  const scoreTemplate = (rowData: Signal | SkeletonRow) => {
    if ('_skeleton' in rowData) return <Skeleton width="4rem" />;
    return <PriorityScale score={rowData.priorityScore} threshold={criticalThreshold} compact />;
  };

  const titleTemplate = (rowData: Signal | SkeletonRow) => {
    if ('_skeleton' in rowData) return (
      <div className="flex flex-column gap-2">
        <Skeleton width="10rem" />
        <Skeleton width="6rem" height="0.5rem" />
      </div>
    );

    const backendTopFactors = rowData.explainabilitySummary?.topFactors || [];
    const topFactors = backendTopFactors.length
      ? backendTopFactors
      : ([
          { key: "urgency", contribution: rowData.scoreBreakdown?.urgency || 0 },
          { key: "impact", contribution: rowData.scoreBreakdown?.impact || 0 },
          { key: "affectedPeople", contribution: rowData.scoreBreakdown?.affectedPeople || 0 },
          { key: "communityVotes", contribution: rowData.scoreBreakdown?.communityVotes || 0 },
        ] as ExplainabilityFactor[])
          .sort((a, b) => b.contribution - a.contribution)
          .slice(0, 2);
    const primary = topFactors[0];
    const secondary = topFactors[1];
    const factorLabel = (factor?: ExplainabilityFactor) =>
      factor ? t(`signals.factor_keys.${factor.key}`) : null;

    let previewText = rowData.explainabilitySummary?.summary || "";
    if (primary && secondary) {
      previewText = t("signals.why_ranked_preview", {
        primary: factorLabel(primary),
        secondary: factorLabel(secondary),
      });
    } else if (primary) {
      previewText = t("signals.why_ranked_preview_single", {
        primary: factorLabel(primary),
      });
    }

    return (
      <div className="flex flex-column py-2 overflow-hidden min-w-0">
        <span className="u-list-item-title text-sm mb-1 group-hover:text-brand-primary transition-colors">
          {rowData.title}
        </span>
        <div className="u-card-meta-row">
          <span className="text-xs text-muted u-meta-value">SIG-ID: {rowData.id?.substring(0,8)}</span>
        </div>
        {previewText && (
          <span
            className="u-list-item-copy text-xs mt-2 line-height-3"
            data-testid={`signal-explainability-${rowData.id}`}
          >
            {previewText}
          </span>
        )}
      </div>
    );
  };

  const emptyMessage = loading ? null : (
    <SignalEmpty
      icon="pi pi-inbox"
      title={t("signals.no_results_title")}
      description={t("signals.no_results_desc")}
      testId="signals-empty-state"
    />
  );

  // `visibleTotal` is the backend total unless a search is active, in which case it is the number
  // of matches on this page.
  const visibleTotal = tableTotalRecords ?? 0;

  const header = (
    <div className="signal-table-header">
      <div className="signal-table-header__copy">
        <div className="signal-table-header__icon">
          <i className="pi pi-database font-bold text-xs" aria-hidden="true" />
        </div>
        <div className="signal-table-header__title">
          <span className="u-eyebrow m-0">{t('dashboard.feed_title')}</span>
          <span className="signal-table-header__count">
            {t('signals.showing_count', { count: filteredSignals.length, total: visibleTotal })}
          </span>
        </div>
      </div>
      <div className="app-search-shell w-full md:w-auto md:ml-auto" style={{ maxWidth: "20rem" }}>
        <i className="pi pi-search app-search-icon" aria-hidden="true" />
        <InputText
          value={globalFilterValue}
          onChange={onGlobalFilterChange}
          placeholder={t('dashboard.search_placeholder')}
          className="app-search-input"
        />
      </div>
    </div>
  );

  return (
    <div className="animate-fade-up overflow-hidden border-round-3xl border-1 border-subtle shadow-premium bg-card signal-table-shell" data-testid="signals-datatable">
      <DataTable
        value={tableData}
        lazy
        paginator
        first={first}
        rows={rows || 10}
        totalRecords={tableTotalRecords}
        onPage={onPage}
        header={header}
        dataKey="id"
        onRowClick={(e) => {
          const row = e.data as Signal | SkeletonRow;
          if (!loading && !('_skeleton' in row)) {
            navigate(`/signal/${row.id}`);
          }
        }}
        rowClassName={(d) => (loading || ('_skeleton' in (d as object))) ? '' : 'cursor-pointer group hover:bg-surface-soft transition-colors'}
        emptyMessage={emptyMessage}
        className="p-datatable-sm"
        sortField="priorityScore"
        sortOrder={-1}
        removableSort
        tableStyle={{ minWidth: '50rem' }}
      >
        <Column header={t('signals.context_header')} body={titleTemplate} sortable sortField="title" data-label={t('signals.context_header')} />
        <Column field="category" header={t('signals.sector_header')} sortable data-label={t('signals.sector_header')} body={(s) => (loading || ('_skeleton' in (s as object))) ? <Skeleton width="4rem" /> : (
          <span className="signal-status-chip" data-status={t(`categories.${(s as Signal).category}`)}>
            <span className="signal-status-chip__dot" aria-hidden="true" style={{ background: "var(--chart-4)" }} />
            <span>{t(`categories.${(s as Signal).category}`)}</span>
          </span>
        )} />
        <Column header={t('signals.lifecycle_header')} sortable sortField="status" body={statusTemplate} data-label={t('signals.lifecycle_header')} style={{ width: '10rem' }} />
        <Column header={t('signals.index_header')} sortable sortField="priorityScore" body={scoreTemplate} data-label={t('signals.index_header')} style={{ width: '9rem' }} />
        <Column
          style={{ width: '10rem' }}
          className="signal-table-cell--action"
          data-label=""
          body={(d) => {
            if (loading || ('_skeleton' in (d as object))) {
              return <Skeleton width="6rem" height="2rem" />;
            }
            const signal = d as Signal;
            return (
              <CivicButton
                type="button"
                variant="ghost"
                size="small"
                label={t('signals.view_details')}
                icon="pi pi-arrow-right"
                onClick={(event) => {
                  event.stopPropagation();
                  navigate(`/signal/${signal.id}`);
                }}
              />
            );
          }}
        />
      </DataTable>
    </div>
  );
}
