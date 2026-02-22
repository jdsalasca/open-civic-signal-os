import { CivicButton } from './CivicButton';
import { classNames } from 'primereact/utils';

interface CivicEmptyStateProps {
  icon: string;
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
  className?: string;
}

export function CivicEmptyState({ icon, title, description, actionLabel, onAction, className }: CivicEmptyStateProps) {
  return (
    <div className={classNames("flex flex-column align-items-center text-center py-8 px-4 animate-fade-up", className)}>
      <div className="relative mb-6">
        <div className="absolute inset-0 bg-brand-primary blur-3xl opacity-10 border-circle"></div>
        <div className="relative w-6rem h-6rem bg-white-alpha-5 border-round-2xl border-1 border-white-alpha-10 flex align-items-center justify-content-center shadow-premium">
          <i className={classNames("pi text-4xl text-muted", icon)}></i>
        </div>
      </div>
      
      <h3 className="text-2xl font-black text-main m-0 tracking-tighter mb-2">{title}</h3>
      <p className="text-secondary text-base font-medium max-w-20rem mx-auto leading-relaxed mb-6 opacity-70">
        {description}
      </p>
      
      {actionLabel && onAction && (
        <CivicButton 
          label={actionLabel} 
          onClick={onAction} 
          variant="secondary"
          className="px-6 py-3 text-xs"
        />
      )}
    </div>
  );
}
