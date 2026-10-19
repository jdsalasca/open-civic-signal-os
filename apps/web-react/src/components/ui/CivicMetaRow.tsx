interface CivicMetaRowProps {
  label: string;
  value: string;
}

export function CivicMetaRow({ label, value }: CivicMetaRowProps) {
  return (
    <div className="flex justify-content-between align-items-start gap-3 mb-3 civic-meta-stack">
      <span className="u-meta-label civic-meta-label">{label}</span>
      <span className="text-sm text-main u-meta-value">{value}</span>
    </div>
  );
}

