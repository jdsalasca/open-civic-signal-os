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
        <span className={`status-indicator bg-${severity === 'info' ? 'blue' : severity === 'warning' ? 'orange' : 'green'}-500 mr-2`}></span>
        <Tag value={rowData.status} severity={severity} pt={{ root: { style: { background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.05)' } } }} />
      </div>
    );
  };

  const scoreTemplate = (rowData: Signal) => {
    if (loading) return <Skeleton width="2rem" />;
    return <span className="font-bold text-cyan-400 text-lg">{rowData.priorityScore.toFixed(0)}</span>;
  };

  const titleTemplate = (s: Signal) => {
    if (loading) return (
      <div className="flex flex-column gap-2">
        <Skeleton width="10rem" />
        <Skeleton width="6rem" height="0.5rem" />
      </div>
    );
    return (
      <div className="flex flex-column py-1">
        <span className="font-bold text-gray-100 text-base mb-1">{s.title}</span>
        <span className="text-xs text-gray-500 uppercase tracking-tighter">ID: {s.id.substring(0,8)}</span>
      </div>
    );
  };

  const header = (
    <div className="flex flex-column md:flex-row justify-content-between md:align-items-center gap-3">
      <h2 className="text-xl font-bold m-0 text-white flex align-items-center gap-3">
        <i className="pi pi-database text-cyan-500"></i>
        Prioritized Signals
      </h2>
      <span className="p-input-icon-left">
        <i className="pi pi-search" />
        <InputText value={globalFilterValue} onChange={onGlobalFilterChange} placeholder="Quick search..." className="p-inputtext-sm w-full md:w-20rem" />
      </span>
    </div>
  );

  return (
    <div className="surface-card border-round-xl border-1 border-white-alpha-10 shadow-8 overflow-hidden animate-fade-in">
      <DataTable 
        value={loading ? Array.from({ length: 5 }) : signals} 
        paginator 
        rows={10} 
        filters={filters}
        globalFilterFields={["title", "category", "status"]}
        header={header}
        dataKey="id"
        onRowClick={(e) => !loading && navigate(`/signal/${e.data.id}`)}
        rowClassName={() => loading ? '' : 'cursor-pointer'}
        emptyMessage="No civic signals found."
        className="p-datatable-lg"
        sortField="priorityScore"
        sortOrder={-1}
        removableSort
      >
        <Column header="Civic Need" body={titleTemplate} sortable sortField="title" />
        <Column field="category" header="Category" sortable body={(s) => loading ? <Skeleton width="4rem" /> : s.category} />
        <Column header="Status" sortable sortField="status" body={statusTemplate} />
        <Column header="Priority" sortable sortField="priorityScore" body={scoreTemplate} style={{ width: '8rem' }} />
        <Column body={() => !loading && <i className="pi pi-angle-right text-gray-700" />} style={{ width: '3.5rem' }} />
      </DataTable>
    </div>
  );
}
