import { HTMLAttributes, ReactNode } from 'react';
import { classNames } from 'primereact/utils';

interface CivicActionLinksProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
}

/**
 * Secondary actions that belong together. One row of compact chips instead of a
 * second toolbar: a page should have one primary action, not six equal buttons.
 */
export function CivicActionLinks({ children, className, ...rest }: CivicActionLinksProps) {
  return (
    <div className={classNames('civic-action-links', className)} {...rest}>
      {children}
    </div>
  );
}
