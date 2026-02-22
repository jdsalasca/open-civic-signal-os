import { Button, ButtonProps } from 'primereact/button';
import { classNames } from 'primereact/utils';

interface CivicButtonProps extends ButtonProps {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  glow?: boolean;
}

export function CivicButton({ variant = 'primary', glow, className, ...props }: CivicButtonProps) {
  const variantClasses = {
    'primary': 'bg-brand-primary hover:bg-brand-primary-hover border-none text-white shadow-lg',
    'secondary': 'bg-white-alpha-10 hover:bg-white-alpha-20 border-none text-main',
    'danger': 'bg-status-rejected hover:opacity-90 border-none text-white',
    'ghost': 'bg-transparent hover:bg-white-alpha-5 border-none text-secondary hover:text-main',
  };

  return (
    <Button
      {...props}
      className={classNames(
        'transition-all active:scale-95 px-4 py-2 font-bold uppercase tracking-wider text-xs',
        variantClasses[variant],
        { 'shadow-cyan-500/20': glow && variant === 'primary' },
        className
      )}
    />
  );
}
