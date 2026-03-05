import { HTMLAttributes, ReactNode } from 'react';
import { classNames } from 'primereact/utils';

interface CivicCardProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title' | 'children'> {
  children: ReactNode;
  title?: ReactNode;
  className?: string;
  padding?: 'none' | 'sm' | 'md' | 'lg';
  variant?: 'neutral' | 'brand' | 'success' | 'warning' | 'danger';
}

export function CivicCard({ children, title, className, padding = 'md', variant = 'neutral', ...rest }: CivicCardProps) {
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
      variantClasses[variant],
      className
    )} {...rest}>
      {title && (
        <div className="px-5 py-4 border-bottom-1 border-subtle bg-white-alpha-5 civic-card-header">
          <h3 className="m-0 text-xs u-section-title">
            {title}
          </h3>
        </div>
      )}
      <div className={paddingClasses[padding]}>
        {children}
      </div>
    </div>
  );
}
