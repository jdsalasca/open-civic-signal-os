import { ReactNode } from "react";
import { classNames } from "primereact/utils";

interface CivicToolbarProps {
  children: ReactNode;
  className?: string;
}

export function CivicToolbar({ children, className }: CivicToolbarProps) {
  return (
    <div
      className={classNames(
        "civic-toolbar flex flex-wrap align-items-center gap-3 border-round-2xl border-1 border-white-alpha-10 p-2",
        className
      )}
    >
      {children}
    </div>
  );
}
