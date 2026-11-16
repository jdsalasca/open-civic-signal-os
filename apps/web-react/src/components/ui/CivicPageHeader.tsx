import { ReactNode } from "react";
import { classNames } from "primereact/utils";

interface CivicPageHeaderProps {
  title: string;
  /**
   * Optional on purpose. A subtitle is only worth rendering when it says something the screen does
   * not already say in a labelled card; forcing one to be invented is how a page ends up announcing
   * the title of the card directly beneath it.
   */
  description?: string;
  className?: string;
  eyebrow?: string;
  /**
   * Primary action cluster for the page (buttons, a freshness pill, a filter). When present the
   * header becomes a two-column layout on wide screens and stacks on narrow ones, so every
   * community view shares one title + subtitle + action pattern instead of re-inventing the wrapper.
   */
  actions?: ReactNode;
  actionsClassName?: string;
}

export function CivicPageHeader({
  title,
  description,
  className,
  eyebrow,
  actions,
  actionsClassName,
}: CivicPageHeaderProps) {
  return (
    <header
      className={classNames("civic-page-header", className ?? "mb-6", {
        "civic-page-header-with-actions": Boolean(actions),
      })}
      data-testid="civic-page-header"
    >
      <div className="civic-page-header-main">
        {eyebrow && (
          <div className="u-pill mb-3">
            <i className="pi pi-sparkles text-brand-primary"></i>
            <span className="u-eyebrow" data-testid="page-header-eyebrow">
              {eyebrow}
            </span>
          </div>
        )}
        <h1 className="u-page-title civic-page-title text-3xl md:text-4xl font-black mb-2" data-testid="page-header-title">
          {title}
        </h1>
        {description && (
          <p
            className="u-page-subtitle civic-page-subtitle text-base font-medium line-height-3"
            data-testid="page-header-description"
          >
            {description}
          </p>
        )}
      </div>
      {actions && (
        <div className={classNames("civic-page-header-actions", actionsClassName)} data-testid="page-header-actions">
          {actions}
        </div>
      )}
    </header>
  );
}
