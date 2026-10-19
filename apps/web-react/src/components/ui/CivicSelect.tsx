import { Dropdown, DropdownProps } from "primereact/dropdown";
import { classNames } from "primereact/utils";

type CivicSelectProps = DropdownProps;

export function CivicSelect({ className, appendTo, panelClassName, ...props }: CivicSelectProps) {
  return (
    <Dropdown
      {...props}
      appendTo={appendTo ?? (typeof document !== "undefined" ? document.body : undefined)}
      panelClassName={classNames("civic-select-panel", panelClassName)}
      className={classNames("w-full", className)}
    />
  );
}
