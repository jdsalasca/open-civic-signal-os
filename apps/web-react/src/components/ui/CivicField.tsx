import { ReactNode } from 'react';
import { classNames } from 'primereact/utils';

interface CivicFieldProps {
  label: string;
  error?: string;
  children: ReactNode;
  className?: string;
  helpText?: string;
}

export function CivicField({ label, error, children, className, helpText }: CivicFieldProps) {
  return (
    <div className={classNames('flex flex-column gap-2 mb-4', className)}>
      <label className="text-xs font-bold uppercase tracking-widest text-secondary ml-1">
        {label}
      </label>
      <div className="relative">
        {children}
      </div>
      {error && (
        <small className="p-error text-xs font-bold mt-1 flex align-items-center gap-1">
          <i className="pi pi-exclamation-circle text-xs"></i>
          {error}
        </small>
      )}
      {helpText && !error && (
        <small className="text-muted text-xs mt-1 ml-1">{helpText}</small>
      )}
    </div>
  );
}
