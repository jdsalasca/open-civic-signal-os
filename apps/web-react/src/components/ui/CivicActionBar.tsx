import { HTMLAttributes, ReactNode } from "react";
import { classNames } from "primereact/utils";

interface CivicActionBarProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  className?: string;
}

export function CivicActionBar({ children, className, ...rest }: CivicActionBarProps) {
  return (
    <div
      className={classNames(
        "civic-toolbar motion-card flex flex-wrap align-items-center gap-2 border-round-3xl border-1 border-white-alpha-10 p-2 civic-action-bar",
        className
      )}
      {...rest}
    >
      {children}
    </div>
  );
}

