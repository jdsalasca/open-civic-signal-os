import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useSettingsStore } from '../../store/useSettingsStore';

const THEMES = ['light', 'dark', 'contrast'] as const;
const ICONS: Record<(typeof THEMES)[number], string> = {
  light: 'pi pi-sun',
  dark: 'pi pi-moon',
  contrast: 'pi pi-bolt'
};

/** One gear in the top bar for the three interface styles; Settings has the same control. */
export function ThemeWheel() {
  const { t } = useTranslation();
  const { theme, setTheme } = useSettingsStore();
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    const close = (event: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, [open]);

  return (
    <div className="theme-wheel" ref={rootRef}>
      <button
        type="button"
        className="theme-wheel-trigger"
        aria-label={t('settings.theme')}
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
        data-testid="theme-wheel-trigger"
      >
        <i className="pi pi-cog" aria-hidden="true" />
      </button>
      {open && (
        <div className="theme-wheel-panel" role="menu" aria-label={t('settings.theme')}>
          {THEMES.map((mode) => (
            <button
              key={mode}
              type="button"
              role="menuitemradio"
              aria-checked={theme === mode}
              className={theme === mode ? 'is-active' : undefined}
              onClick={() => {
                setTheme(mode);
                setOpen(false);
              }}
              data-testid={`theme-wheel-${mode}`}
            >
              <i className={ICONS[mode]} aria-hidden="true" />
              <span>{t(`settings.${mode}`)}</span>
              {theme === mode && <i className="pi pi-check theme-wheel-check" aria-hidden="true" />}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
