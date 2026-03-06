import { ReactNode } from "react";
import { classNames } from "primereact/utils";

type Tone = "brand" | "success" | "warning" | "danger" | "neutral";

interface CivicStatProps {
  label: string;
  value: ReactNode;
  icon?: string;
  tone?: Tone;
  className?: string;
}

export function CivicStat({ label, value, icon, tone = "neutral", className }: CivicStatProps) {
  const toneClass = {
    brand: "bg-brand-primary-alpha-10 text-brand-primary",
    success: "bg-status-resolved-alpha-10 text-status-resolved",
    warning: "bg-status-progress-alpha-10 text-status-progress",
    danger: "bg-status-rejected-alpha-10 text-status-rejected",
    neutral: "bg-surface-soft text-main border-1 border-surface-soft",
  }[tone];

  return (
    <div className={classNames("flex align-items-center gap-4", className)}>
      {icon && (
        <div className={classNames("p-3 border-round-xl", toneClass)}>
          <i className={`pi ${icon} text-3xl`} />
        </div>
      )}
      <div>
        <span className="block text-muted font-bold uppercase text-xs tracking-widest mb-1">{label}</span>
        <span className="text-4xl font-black text-main">{value}</span>
      </div>
    </div>
  );
}

