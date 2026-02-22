import { Dropdown, DropdownProps } from "primereact/dropdown";
import { classNames } from "primereact/utils";

type CivicSelectProps = DropdownProps;

export function CivicSelect({ className, appendTo, ...props }: CivicSelectProps) {
  return (
    <Dropdown
      {...props}
      appendTo={appendTo ?? (typeof document !== "undefined" ? document.body : undefined)}
      className={classNames("w-full bg-black-alpha-20", className)}
    />
  );
}
