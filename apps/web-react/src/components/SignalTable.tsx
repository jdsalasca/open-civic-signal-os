import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { Tag } from "primereact/tag";
import { useNavigate } from "react-router-dom";
import { Signal } from "../types";

type Props = {
  signals: Signal[];
  loading: boolean;
};

export function SignalTable({ signals, loading }: Props) {
  const navigate = useNavigate();

  const statusTemplate = (rowData: Signal) => {
    const severity = rowData.status === "NEW" ? "info" : 
                    rowData.status === "IN_PROGRESS" ? "warning" : "success";
    return <Tag value={rowData.status} severity={severity} />;
  };

  const scoreTemplate = (rowData: Signal) => {
    return (
      <span className="font-bold text-cyan-400">
        {rowData.priorityScore.toFixed(1)}
      </span>
    );
  };

  const actionTemplate = (rowData: Signal) => {
    return (
      <i className="pi pi-chevron-right text-gray-600 cursor-pointer hover:text-cyan-400" 
         onClick={() => navigate(`/signal/${rowData.id}`)} />
    );
  };

  return (
    <div className="surface-card p-4 border-round-xl shadow-2 border-1 border-gray-800">
      <DataTable 
        value={signals} 
        loading={loading}
        paginator 
        rows={10} 
        dataKey="id"
        onRowClick={(e) => navigate(`/signal/${e.data.id}`)}
        rowClassName={() => 'cursor-pointer'}
        emptyMessage="No civic signals found."
        className="p-datatable-sm"
        sortField="priorityScore"
        sortOrder={-1}
      >
        <Column field="title" header="Civic Need" sortable body={(s) => <span className="font-semibold">{s.title}</span>} />
        <Column field="category" header="Category" sortable />
        <Column field="status" header="Status" sortable body={statusTemplate} />
        <Column field="priorityScore" header="Score" sortable body={scoreTemplate} />
        <Column body={actionTemplate} style={{ width: '3rem' }} />
      </DataTable>
    </div>
  );
}
