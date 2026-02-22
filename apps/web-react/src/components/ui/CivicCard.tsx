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
    'neutral': 'border-white-alpha-10',
    'brand': 'border-brand-primary/30',
    'success': 'border-status-resolved/30',
    'warning': 'border-status-progress/30',
    'danger': 'border-status-rejected/30',
  };

  return (
    <div className={classNames(
      'glass-panel rounded-2xl overflow-hidden transition-all duration-300 hover:border-white-alpha-20',
      variantClasses[variant],
      className
    )} {...rest}>
      {title && (
        <div className="px-5 py-4 border-bottom-1 border-white-alpha-10 bg-white-alpha-5">
          <h3 className="m-0 text-xs font-black uppercase tracking-widest text-brand-primary">
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
