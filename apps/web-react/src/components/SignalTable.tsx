import { useState, useMemo } from "react";
import { DataTable, DataTableFilterMeta } from "primereact/datatable";
import { Column } from "primereact/column";
import { InputText } from "primereact/inputtext";
import { FilterMatchMode } from "primereact/api";
import { Skeleton } from "primereact/skeleton";
import { useNavigate } from "react-router-dom";
import { Signal } from "../types";
import { CivicBadge } from "./ui/CivicBadge";

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
  const [globalFilterValue, setGlobalFilterValue] = useState("");
  
  const [filters, setFilters] = useState<DataTableFilterMeta>({
    global: { value: null, matchMode: FilterMatchMode.CONTAINS },
  });

  const tableData = useMemo<(Signal | SkeletonRow)[]>(() => {
    if (loading) {
      return new Array(rows || 6).fill(null).map((_, i) => ({ _skeleton: true, id: `sk-${i}` }));
    }
    return signals;
  }, [signals, loading, rows]);

  const onGlobalFilterChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    const _filters = { ...filters };
    (_filters["global"] as { value: string | null; matchMode: FilterMatchMode }).value = value;
    setFilters(_filters);
    setGlobalFilterValue(value);
  };

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
    return (
      <div className="flex align-items-center gap-3">
        <span className="font-black text-main text-lg tabular-nums">{rowData.priorityScore?.toFixed(0)}</span>
        <div className="hidden xl:flex flex-column gap-1 flex-grow-1" style={{ maxWidth: '60px' }}>
           <div className="bg-white-alpha-10 border-round-sm overflow-hidden" style={{ height: '4px' }}>
              <div className="bg-brand-primary h-full" style={{ width: `${Math.min(((rowData.priorityScore || 0) / 350) * 100, 100)}%` }}></div>
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
      <div className="flex flex-column py-2 overflow-hidden">
        <span className="font-bold text-main text-sm mb-1 group-hover:text-brand-primary transition-colors truncate">
          {rowData.title}
        </span>
        <div className="flex align-items-center gap-2 opacity-50">
          <span className="text-min font-mono font-bold uppercase tracking-tighter">SIG-ID: {rowData.id?.substring(0,8)}</span>
        </div>
      </div>
    );
  };

  const header = (
    <div className="flex flex-column md:flex-row justify-content-between md:align-items-center gap-4 px-6 py-4 border-bottom-1 border-white-alpha-5 bg-black-alpha-20">
      <div className="flex align-items-center gap-3">
        <div className="w-2rem h-2rem bg-brand-primary border-round-sm flex align-items-center justify-content-center shadow-lg">
          <i className="pi pi-database text-black font-bold text-xs"></i>
        </div>
        <h2 className="text-sm font-black m-0 text-main tracking-widest uppercase">Live Intelligence Feed</h2>
      </div>
      <span className="p-input-icon-left w-full md:w-auto">
        <i className="pi pi-search text-muted text-xs" />
        <InputText 
          value={globalFilterValue} 
          onChange={onGlobalFilterChange} 
          placeholder="Filter protocols..." 
          className="w-full md:w-20rem p-inputtext-sm bg-black-alpha-40 border-white-alpha-10 text-xs font-bold uppercase tracking-wider" 
        />
      </span>
    </div>
  );

  return (
    <div className="animate-fade-up overflow-hidden border-round-3xl border-1 border-white-alpha-10 shadow-premium">
      <DataTable 
        value={tableData} 
        lazy
        paginator 
        first={first}
        rows={rows || 10} 
        totalRecords={totalRecords}
        onPage={onPage}
        filters={filters}
        header={header}
        dataKey="id" 
        onRowClick={(e) => {
          const row = e.data as Signal | SkeletonRow;
          if (!loading && !('_skeleton' in row)) {
            navigate(`/signal/${row.id}`);
          }
        }}
        rowClassName={(d) => (loading || ('_skeleton' in (d as object))) ? '' : 'cursor-pointer group hover:bg-white-alpha-5 transition-colors'}
        emptyMessage={loading ? null : <div className="p-8 text-center text-muted font-bold uppercase tracking-widest text-xs">No intelligence data matches current filters</div>}
        className="p-datatable-sm"
        sortField="priorityScore"
        sortOrder={-1}
        removableSort
        tableStyle={{ minWidth: '50rem' }}
      >
        <Column header="Intelligence Context" body={titleTemplate} sortable sortField="title" className="pl-6" />
        <Column field="category" header="Sector" sortable body={(s) => (loading || ('_skeleton' in (s as object))) ? <Skeleton width="4rem" /> : (
          <CivicBadge type="category" label={(s as Signal).category} />
        )} />
        <Column header="Lifecycle" sortable sortField="status" body={statusTemplate} style={{ width: '10rem' }} />
        <Column header="Index" sortable sortField="priorityScore" body={scoreTemplate} style={{ width: '10rem' }} />
        <Column body={(d) => (!loading && !('_skeleton' in (d as object))) && <i className="pi pi-arrow-right text-muted group-hover:text-brand-primary group-hover:translate-x-1 transition-all text-xs" />} style={{ width: '4rem' }} className="pr-6" />
      </DataTable>
    </div>
  );
}
