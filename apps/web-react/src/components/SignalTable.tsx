import { useState, useMemo } from "react";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { InputText } from "primereact/inputtext";
import { Skeleton } from "primereact/skeleton";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { ExplainabilityFactor, Signal } from "../types";
import { CivicBadge } from "./ui/CivicBadge";
import { CivicButton } from "./ui/CivicButton";

type Props = {
  signals: Signal[];
  loading: boolean;
  totalRecords?: number;
  rows?: number;
  first?: number;
  onPage?: (event: any) => void;
};

interface SkeletonRow {
  _skeleton: boolean;
  id?: string;
}

export function SignalTable({ signals, loading, totalRecords, rows, first, onPage }: Props) {
  const navigate = useNavigate();
  const { t } = useTranslation();
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
    
    let severity: 'new' | 'progress' | 'resolved' | 'rejected' = 'new';
    if (rowData.status === "IN_PROGRESS") severity = 'progress';
    if (rowData.status === "RESOLVED") severity = 'resolved';
    if (rowData.status === "REJECTED") severity = 'rejected';

    return <CivicBadge label={rowData.status} severity={severity} />;
  };

  const scoreTemplate = (rowData: Signal | SkeletonRow) => {
    if ('_skeleton' in rowData) return <Skeleton width="2rem" />;
    return <span className="font-black text-main text-lg tabular-nums">{rowData.priorityScore?.toFixed(0)}</span>;
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
      <div className="flex flex-column py-2 overflow-hidden">
        <span className="font-bold text-main text-sm mb-1 group-hover:text-brand-primary transition-colors truncate">
          {rowData.title}
        </span>
        <div className="flex align-items-center gap-2">
          <span className="text-min text-muted font-mono font-bold uppercase tracking-tighter">SIG-ID: {rowData.id?.substring(0,8)}</span>
        </div>
        {previewText && (
          <span
            className="text-xs text-secondary mt-2 line-height-3"
            data-testid={`signal-explainability-${rowData.id}`}
          >
            {previewText}
          </span>
        )}
      </div>
    );
  };

  const header = (
    <div className="flex flex-column md:flex-row justify-content-between md:align-items-center gap-4 px-6 py-4 bg-surface">
      <div className="flex align-items-center gap-3">
        <div className="w-2rem h-2rem bg-brand-primary border-round-sm flex align-items-center justify-content-center shadow-lg">
          <i className="pi pi-database text-on-brand font-bold text-xs"></i>
        </div>
        <div className="flex flex-column gap-1">
          <h2 className="text-sm font-black m-0 text-main tracking-widest uppercase">{t('dashboard.feed_title')}</h2>
          <span className="text-xs text-muted">{t('dashboard.focus_subtitle')}</span>
        </div>
      </div>
      <div className="app-search-shell w-full md:w-auto md:ml-auto" style={{ maxWidth: "20rem" }}>
        <i className="pi pi-search app-search-icon" />
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
    <div className="animate-fade-up overflow-hidden border-round-3xl border-1 border-subtle shadow-premium bg-card" data-testid="signals-datatable">
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
        rowClassName={(d) => (loading || ('_skeleton' in (d as object))) ? '' : 'cursor-pointer group hover:bg-white-alpha-5 transition-colors'}
        emptyMessage={loading ? null : <div className="p-8 text-center text-muted font-bold uppercase tracking-widest text-xs">{t('signals.no_results')}</div>}
        className="p-datatable-sm"
        sortField="priorityScore"
        sortOrder={-1}
        removableSort
        tableStyle={{ minWidth: '50rem' }}
      >
        <Column header={t('signals.context_header')} body={titleTemplate} sortable sortField="title" className="pl-6" />
        <Column field="category" header={t('signals.sector_header')} sortable body={(s) => (loading || ('_skeleton' in (s as object))) ? <Skeleton width="4rem" /> : (
          <CivicBadge type="category" label={(s as Signal).category} />
        )} />
        <Column header={t('signals.lifecycle_header')} sortable sortField="status" body={statusTemplate} style={{ width: '10rem' }} />
        <Column header={t('signals.index_header')} sortable sortField="priorityScore" body={scoreTemplate} style={{ width: '8rem' }} />
        <Column
          style={{ width: '10rem' }}
          className="pr-6"
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
