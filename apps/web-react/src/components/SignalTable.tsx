import { useState } from "react";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { Tag } from "primereact/tag";
import { InputText } from "primereact/inputtext";
import { FilterMatchMode } from "primereact/api";
import { Skeleton } from "primereact/skeleton";
import { useNavigate } from "react-router-dom";
import { Signal } from "../types";

type Props = {
  signals: Signal[];
  loading: boolean;
};

export function SignalTable({ signals, loading }: Props) {
  const navigate = useNavigate();
  const [globalFilterValue, setGlobalFilterValue] = useState("");
  const [filters, setFilters] = useState({
    global: { value: null, matchMode: FilterMatchMode.CONTAINS },
  });

  const onGlobalFilterChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    let _filters = { ...filters };
    _filters["global"].value = value;
    setFilters(_filters);
    setGlobalFilterValue(value);
  };

  const statusTemplate = (rowData: Signal) => {
    if (loading) return <Skeleton width="4rem" height="1.5rem" border-round="12px" />;
    const severity = rowData.status === "NEW" ? "info" : 
                    rowData.status === "IN_PROGRESS" ? "warning" : "success";
    return (
      <div className="flex align-items-center">
        <span className={`status-indicator bg-${severity === 'info' ? 'blue' : severity === 'warning' ? 'orange' : 'green'}-500 mr-2 shadow-2`}></span>
        <Tag value={rowData.status} severity={severity} pt={{ root: { style: { background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.05)', fontSize: '10px' } } }} />
      </div>
    );
  };

  const scoreTemplate = (rowData: Signal) => {
    if (loading) return <Skeleton width="2rem" />;
    return (
      <div className="flex align-items-center gap-3">
        <span className="font-bold text-cyan-400 text-lg glow-cyan" style={{ minWidth: '35px' }}>{rowData.priorityScore.toFixed(0)}</span>
        <div className="hidden xl:flex flex-column gap-1 flex-grow-1" style={{ maxWidth: '60px' }}>
           <div className="bg-gray-800 border-round overflow-hidden" style={{ height: '4px' }}>
              <div className="bg-cyan-500 h-full" style={{ width: `${Math.min((rowData.priorityScore / 350) * 100, 100)}%` }}></div>
           </div>
        </div>
      </div>
    );
  };

  const titleTemplate = (s: Signal) => {
    if (loading) return (
      <div className="flex flex-column gap-2">
        <Skeleton width="10rem" />
        <Skeleton width="6rem" height="0.5rem" />
      </div>
    );
    return (
      <div className="flex flex-column py-1 overflow-hidden">
        <span className="font-bold text-gray-100 text-base mb-1 hover:text-cyan-400 transition-colors text-overflow-ellipsis overflow-hidden white-space-nowrap">
          {s.title}
        </span>
        <div className="flex align-items-center gap-2">
          <span className="text-xs text-gray-600 uppercase tracking-tighter">REF: {s.id.substring(0,8)}</span>
        </div>
      </div>
    );
  };

  const header = (
    <div className="flex flex-column md:flex-row justify-content-between md:align-items-center gap-3 px-2 py-1">
      <div className="flex align-items-center gap-2">
        <i className="pi pi-bolt text-yellow-500"></i>
        <h2 className="text-xl font-bold m-0 text-white">Prioritized Feed</h2>
      </div>
      <span className="p-input-icon-left">
        <i className="pi pi-search text-gray-500" />
        <InputText value={globalFilterValue} onChange={onGlobalFilterChange} placeholder="Search signals..." className="p-inputtext-sm w-full md:w-20rem bg-gray-950 border-gray-800" />
      </span>
    </div>
  );

  return (
    <div className="animate-fade-in surface-section border-round-xl border-1 border-white-alpha-10 shadow-8 overflow-hidden">
      <DataTable 
        value={loading ? Array.from({ length: 6 }) : signals} 
        paginator 
        rows={10} 
        filters={filters}
        globalFilterFields={["title", "category", "status"]}
        header={header}
        dataKey="id"
        onRowClick={(e) => !loading && navigate(`/signal/${e.data.id}`)}
        rowClassName={() => loading ? '' : 'cursor-pointer'}
        emptyMessage="No civic signals found."
        className="p-datatable-sm"
        sortField="priorityScore"
        sortOrder={-1}
        removableSort
        tableStyle={{ minWidth: '50rem' }}
      >
        <Column header="Civic Need" body={titleTemplate} sortable sortField="title" />
        <Column field="category" header="Category" sortable body={(s) => loading ? <Skeleton width="4rem" /> : (
          <span className="text-xs font-bold text-gray-500 uppercase tracking-widest bg-black-alpha-20 px-2 py-1 border-round">{s.category}</span>
        )} />
        <Column header="Status" sortable sortField="status" body={statusTemplate} style={{ width: '10rem' }} />
        <Column header="Priority" sortable sortField="priorityScore" body={scoreTemplate} style={{ width: '10rem' }} />
        <Column body={() => !loading && <i className="pi pi-arrow-up-right text-gray-700 hover:text-cyan-400 transition-colors" />} style={{ width: '3rem' }} />
      </DataTable>
    </div>
  );
}
