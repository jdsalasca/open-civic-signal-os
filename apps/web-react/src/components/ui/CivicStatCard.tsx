import { HTMLAttributes, ReactNode } from "react";
import { classNames } from "primereact/utils";

type CivicStatCardProps = Omit<HTMLAttributes<HTMLDivElement>, "title"> & {
  label: ReactNode;
  value: ReactNode;
  supportingText?: ReactNode;
  icon?: ReactNode;
  tone?: "default" | "brand";
  compact?: boolean;
};

export function CivicStatCard({
  label,
  value,
  supportingText,
  icon,
  tone = "default",
  compact = false,
  className,
  ...rest
}: CivicStatCardProps) {
  return (
    <div
      className={classNames(
        "civic-stat-card",
        {
          "civic-stat-card-brand": tone === "brand",
          "civic-stat-card-compact": compact
        },
        className
      )}
      {...rest}
    >
      <div className="civic-stat-card-top">
        <span className="civic-stat-label">{label}</span>
        {icon ? <span className="civic-stat-icon">{icon}</span> : null}
      </div>
      <div className="civic-stat-value">{value}</div>
      {supportingText ? <div className="civic-stat-supporting">{supportingText}</div> : null}
    </div>
  );
}
