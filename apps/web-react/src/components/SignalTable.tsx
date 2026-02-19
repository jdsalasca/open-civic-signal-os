import { useState, useMemo } from "react";
import { DataTable, DataTableFilterMeta } from "primereact/datatable";
import { Column } from "primereact/column";
import { Tag } from "primereact/tag";
import { InputText } from "primereact/inputtext";
import { FilterMatchMode } from "primereact/api";
import { Skeleton } from "primereact/skeleton";
import { Button } from "primereact/button";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Signal } from "../types";

type Props = {
  signals: Signal[];
  loading: boolean;
};

interface SkeletonRow {
  _skeleton: boolean;
  id?: string;
}

export function SignalTable({ signals, loading }: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [globalFilterValue, setGlobalFilterValue] = useState("");
  
  const [filters, setFilters] = useState<DataTableFilterMeta>({
    global: { value: null, matchMode: FilterMatchMode.CONTAINS },
  });

  const tableData = useMemo<(Signal | SkeletonRow)[]>(() => {
    if (loading) {
      return new Array(6).fill(null).map((_, i) => ({ _skeleton: true, id: `sk-${i}` }));
    }
    return signals;
  }, [signals, loading]);

  const onGlobalFilterChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    const _filters = { ...filters };
    (_filters["global"] as { value: string | null; matchMode: FilterMatchMode }).value = value;
    setFilters(_filters);
    setGlobalFilterValue(value);
  };

  const statusTemplate = (rowData: Signal | SkeletonRow) => {
    if ('_skeleton' in rowData) return <Skeleton width="4rem" height="1.5rem" />;
    const severity = rowData.status === "NEW" ? "info" : 
                    rowData.status === "IN_PROGRESS" ? "warning" : "success";
    return (
      <div className="flex align-items-center">
        <span className={`status-indicator bg-${severity === 'info' ? 'blue' : severity === 'warning' ? 'orange' : 'green'}-500 mr-2 shadow-2`}></span>
        <Tag value={rowData.status} severity={severity} pt={{ root: { style: { background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', fontSize: '10px', fontWeight: 'bold' } } }} />
      </div>
    );
  };

  const scoreTemplate = (rowData: Signal | SkeletonRow) => {
    if ('_skeleton' in rowData) return <Skeleton width="2rem" />;
    return (
      <div className="flex align-items-center gap-3" role="group" aria-label={`${t('common.score')}: ${rowData.priorityScore?.toFixed(0)}`}>
        <span className="font-black text-cyan-400 text-lg glow-cyan" style={{ minWidth: '35px' }}>{rowData.priorityScore?.toFixed(0)}</span>
        <div className="hidden xl:flex flex-column gap-1 flex-grow-1" style={{ maxWidth: '60px' }}>
           <div className="bg-gray-800 border-round overflow-hidden shadow-inner" style={{ height: '6px' }}>
              <div className="bg-cyan-500 h-full shadow-2" style={{ width: `${Math.min(((rowData.priorityScore || 0) / 350) * 100, 100)}%` }}></div>
           </div>
        </div>
      </div>
    );
  };

  const titleTemplate = (rowData: Signal | SkeletonRow) => {
    if ('_skeleton' in rowData) return (
      <div className="flex flex-column gap-2">
        <Skeleton width="10rem" />
        <Skeleton width="6rem" height="0.5rem" />
      </div>
    );
    return (
      <div className="flex flex-column py-1 overflow-hidden" data-testid={`signal-title-${rowData.id?.substring(0,8)}`}>
        <span className="font-bold text-white text-base mb-1 hover:text-cyan-400 transition-colors text-overflow-ellipsis overflow-hidden white-space-nowrap">
          {rowData.title}
        </span>
        <div className="flex align-items-center gap-2">
          <span className="text-xs text-gray-500 font-mono font-bold uppercase tracking-widest">{t('signals.ref')}: {rowData.id?.substring(0,8)}</span>
        </div>
      </div>
    );
  };

  const header = (
    <div className="flex flex-column md:flex-row justify-content-between md:align-items-center gap-3 px-2 py-1">
      <div className="flex align-items-center gap-2">
        <i className="pi pi-bolt text-yellow-500 text-xl shadow-4"></i>
        <h2 className="text-xl font-black m-0 text-white tracking-tight uppercase">{t('dashboard.feed_title')}</h2>
      </div>
      <span className="p-input-icon-left shadow-2 border-round-lg overflow-hidden">
        <i className="pi pi-search text-gray-400" />
        <InputText 
          value={globalFilterValue} 
          onChange={onGlobalFilterChange} 
          placeholder={t('dashboard.search_placeholder')} 
          className="p-inputtext-sm w-full md:w-20rem bg-gray-900 border-none font-medium text-white" 
          data-testid="signal-search-input"
          aria-label={t('dashboard.search_placeholder')}
        />
      </span>
    </div>
  );

  const emptyTemplate = () => (
    <div className="empty-state-container my-6" data-testid="empty-signals-state">
      <div className="bg-gray-900 border-circle inline-flex align-items-center justify-content-center mb-4 shadow-4" style={{ width: '100px', height: '100px' }}>
        <i className="pi pi-search-plus text-4xl text-gray-600"></i>
      </div>
      <h3 className="text-2xl font-black text-white m-0">{t('dashboard.empty_title')}</h3>
      <p className="text-gray-400 max-w-20rem mx-auto mt-2 mb-5 font-medium line-height-3">
        {t('dashboard.empty_desc')}
      </p>
      <Button 
        label={t('dashboard.new_issue')} 
        icon="pi pi-plus" 
        className="p-button-primary border-none px-6 py-3 shadow-6 font-bold" 
        onClick={() => navigate("/report")} 
        data-testid="empty-state-report-button"
        aria-label={t('dashboard.new_issue')}
      />
    </div>
  );

  return (
    <div className="animate-fade-in surface-section border-round-xl border-1 border-white-alpha-10 shadow-8 overflow-hidden">
      <DataTable 
        value={tableData} 
        paginator 
        rows={10} 
        filters={filters}
        globalFilterFields={["title", "category", "status"]}
        header={header}
        dataKey="id" 
        onRowClick={(e) => {
          const row = e.data as Signal | SkeletonRow;
          if (!loading && !('_skeleton' in row)) {
            navigate(`/signal/${row.id}`);
          }
        }}
        rowClassName={(d) => (loading || ('_skeleton' in (d as object))) ? '' : 'cursor-pointer transition-colors'}
        emptyMessage={loading ? null : emptyTemplate}
        className="p-datatable-sm"
        sortField="priorityScore"
        sortOrder={-1}
        removableSort
        tableStyle={{ minWidth: '50rem' }}
        data-testid="signals-datatable"
        aria-label={t('dashboard.feed_title')}
      >
        <Column header={t('common.title')} body={titleTemplate} sortable sortField="title" />
        <Column field="category" header={t('common.category')} sortable body={(s) => (loading || ('_skeleton' in (s as object))) ? <Skeleton width="4rem" /> : (
          <span className="text-xs font-black text-gray-400 uppercase tracking-widest bg-white-alpha-5 px-3 py-1 border-round-lg border-1 border-white-alpha-10">{t(`categories.${(s as Signal).category}`)}</span>
        )} />
        <Column header={t('common.status')} sortable sortField="status" body={statusTemplate} style={{ width: '10rem' }} />
        <Column header={t('common.score')} sortable sortField="priorityScore" body={scoreTemplate} style={{ width: '12rem' }} />
        <Column body={(d) => (!loading && !('_skeleton' in (d as object))) && <i className="pi pi-arrow-up-right text-gray-600 hover:text-cyan-400 transition-colors" />} style={{ width: '3rem' }} aria-label={t('signals.view_details')} />
      </DataTable>
    </div>
  );
}
