interface CivicMetaRowProps {
  label: string;
  value: string;
}

export function CivicMetaRow({ label, value }: CivicMetaRowProps) {
  return (
    <div className="flex justify-content-between align-items-center mb-3">
      <span className="text-xs font-bold text-muted uppercase tracking-widest">{label}</span>
      <span className="text-xs font-mono text-main">{value}</span>
    </div>
  );
}

