import { HTMLAttributes, ReactNode } from 'react';
import { classNames } from 'primereact/utils';

interface CivicCardProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title' | 'children'> {
  children: ReactNode;
  title?: ReactNode;
  headerActions?: ReactNode;
  className?: string;
  contentClassName?: string;
  titleClassName?: string;
  padding?: 'none' | 'sm' | 'md' | 'lg';
  variant?: 'neutral' | 'brand' | 'success' | 'warning' | 'danger';
  fullHeight?: boolean;
}

export function CivicCard({
  children,
  title,
  headerActions,
  className,
  contentClassName,
  titleClassName,
  padding = 'md',
  variant = 'neutral',
  fullHeight = false,
  ...rest
}: CivicCardProps) {
  const paddingClasses = {
    'none': 'p-0',
    'sm': 'p-3',
    'md': 'p-5',
    'lg': 'p-8',
  };

  const variantClasses = {
    'neutral': '',
    'brand': 'border-brand-primary border-opacity-30',
    'success': 'border-status-resolved border-opacity-30',
    'warning': 'border-status-progress border-opacity-30',
    'danger': 'border-status-rejected border-opacity-30',
  };

  return (
    <div className={classNames(
      'glass-panel u-card-surface motion-card motion-card-hover rounded-3xl overflow-hidden civic-card',
      { 'h-full': fullHeight },
      variantClasses[variant],
      className
    )} {...rest}>
      {(title || headerActions) && (
        <div className="px-5 py-4 border-bottom-1 border-subtle bg-surface-soft civic-card-header">
          <div className="civic-card-header-row">
            {title ? (
              <h3 className={classNames('m-0 text-xs u-section-title civic-card-title', titleClassName)}>
                {title}
              </h3>
            ) : (
              <div />
            )}
            {headerActions && <div className="civic-card-header-actions">{headerActions}</div>}
          </div>
        </div>
      )}
      <div className={classNames(paddingClasses[padding], 'civic-card-content', contentClassName)}>
        {children}
      </div>
    </div>
  );
}
