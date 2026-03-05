import { Button, ButtonProps } from 'primereact/button';
import { classNames } from 'primereact/utils';

interface CivicButtonProps extends ButtonProps {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  glow?: boolean;
}

export function CivicButton({ variant = 'primary', glow, className, ...props }: CivicButtonProps) {
  const variantClasses = {
    'primary': 'p-button-primary civic-button civic-button-primary',
    'secondary': 'p-button-secondary civic-button civic-button-secondary',
    'danger': 'p-button-danger civic-button civic-button-danger',
    'ghost': 'p-button-text civic-button civic-button-ghost bg-transparent border-1 border-white-alpha-10 text-secondary hover:text-main hover:bg-white-alpha-5',
  };

  return (
    <Button
      {...props}
      type={props.type ?? 'button'}
      loadingIcon="pi pi-spinner pi-spin"
      className={classNames(
        'transition-all active:scale-95 px-4 py-2 font-bold text-sm civic-button-base',
        { 'shadow-lg': glow },
        variantClasses[variant],
        className
      )}
    />
  );
}
