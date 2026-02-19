import { useMemo } from "react";
import { Signal } from "../types";
import { Card } from "primereact/card";
import { Divider } from "primereact/divider";

type Props = {
  signals: Signal[];
};

export function DigestSidebar({ signals }: Props) {
  const topUnresolved = useMemo(() => {
    return signals
      .filter((s) => s.status === "NEW")
      .sort((a, b) => b.priorityScore - a.priorityScore)
      .slice(0, 5); // Limit to top 5 for cleaner look
  }, [signals]);

  return (
    <Card className="shadow-4 border-1 border-gray-800 animate-fade-in" header={
      <div className="p-4 pb-0">
        <h2 className="text-lg font-bold text-gray-100 flex align-items-center gap-2">
          <i className="pi pi-star-fill text-yellow-500 text-sm"></i>
          CRITICAL NEEDS
        </h2>
        <p className="text-xs text-gray-500 m-0 mt-1 uppercase tracking-widest">Unresolved community top</p>
      </div>
    }>
      <div className="flex flex-column gap-2 mt-2">
        {topUnresolved.map((signal, idx) => (
          <div key={signal.id}>
            <div className="flex align-items-start gap-3 p-2 border-round-lg hover:bg-white-alpha-5 cursor-pointer">
              <span className="text-xl font-black text-gray-800 opacity-50">0{idx + 1}</span>
              <div className="flex flex-column overflow-hidden">
                <span className="text-sm font-semibold text-gray-200 line-height-2 white-space-nowrap text-overflow-ellipsis overflow-hidden">
                  {signal.title}
                </span>
                <span className="text-xs text-cyan-600 font-medium">{signal.category}</span>
              </div>
            </div>
            {idx < topUnresolved.length - 1 && <Divider className="m-0 opacity-10" />}
          </div>
        ))}
        {topUnresolved.length === 0 && <p className="text-center text-gray-600 text-sm py-4">All signals are being addressed.</p>}
      </div>
    </Card>
  );
}
