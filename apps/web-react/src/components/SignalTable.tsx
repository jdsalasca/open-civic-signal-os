import { useMemo, useState } from "react";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { Tag } from "primereact/tag";
import { InputText } from "primereact/inputtext";
import { FilterMatchMode } from "primereact/api";
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
    const severity = rowData.status === "NEW" ? "info" : 
                    rowData.status === "IN_PROGRESS" ? "warning" : "success";
    return (
      <div className="flex align-items-center">
        <span className={`status-indicator bg-${severity === 'info' ? 'blue' : severity === 'warning' ? 'orange' : 'green'}-500 mr-2`}></span>
        <Tag value={rowData.status} severity={severity} pt={{ root: { style: { background: 'transparent', border: 'none', padding: 0 } } }} />
      </div>
    );
  };

  const scoreTemplate = (rowData: Signal) => {
    return (
      <div className="flex flex-column">
        <span className="font-bold text-cyan-400 text-lg glow-cyan">
          {rowData.priorityScore.toFixed(0)}
        </span>
        <div className="flex gap-1 mt-1">
          <div className="bg-cyan-900 border-round" style={{ height: '3px', width: '20px', opacity: 0.5 }}></div>
          <div className="bg-cyan-700 border-round" style={{ height: '3px', width: '20px' }}></div>
        </div>
      </div>
    );
  };

  const header = (
    <div className="flex justify-content-between align-items-center">
      <h2 className="text-xl font-bold m-0 text-white">Prioritized Backlog</h2>
      <span className="p-input-icon-left">
        <i className="pi pi-search" />
        <InputText value={globalFilterValue} onChange={onGlobalFilterChange} placeholder="Search signals..." className="p-inputtext-sm" />
      </span>
    </div>
  );

  return (
    <div className="animate-fade-in">
      <DataTable 
        value={signals} 
        loading={loading}
        paginator 
        rows={10} 
        filters={filters}
        globalFilterFields={["title", "category", "status"]}
        header={header}
        dataKey="id"
        onRowClick={(e) => navigate(`/signal/${e.data.id}`)}
        rowClassName={() => 'cursor-pointer'}
        emptyMessage="No civic signals found."
        className="p-datatable-lg border-round-xl overflow-hidden shadow-4"
        sortField="priorityScore"
        sortOrder={-1}
        responsiveLayout="stack"
        breakpoint="960px"
      >
        <Column field="title" header="Civic Need" sortable body={(s) => (
          <div className="flex flex-column py-1">
            <span className="font-bold text-gray-100 text-base mb-1">{s.title}</span>
            <span className="text-xs text-gray-500 uppercase tracking-tighter">ID: {s.id.substring(0,8)}</span>
          </div>
        )} />
        <Column field="category" header="Category" sortable body={(s) => (
          <Tag value={s.category} severity="secondary" pt={{ root: { style: { background: 'rgba(255,255,255,0.05)', color: '#94a3b8' } } }} />
        )} />
        <Column field="status" header="Status" sortable body={statusTemplate} />
        <Column field="priorityScore" header="Priority Rank" sortable body={scoreTemplate} />
        <Column body={() => <i className="pi pi-angle-right text-gray-700" />} style={{ width: '3rem' }} />
      </DataTable>
    </div>
  );
}
