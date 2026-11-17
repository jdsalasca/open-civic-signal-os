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
  /** One-line page heading for feed-style screens. */
  compact?: boolean;
}

export function CivicPageHeader({
  title,
  description,
  className,
  eyebrow,
  actions,
  actionsClassName,
  compact = false,
}: CivicPageHeaderProps) {
  return (
    <header
      className={classNames("civic-page-header", className ?? "mb-5", {
        "civic-page-header-with-actions": Boolean(actions),
        "civic-page-header-compact": compact,
      })}
      data-testid="civic-page-header"
    >
      <div className="civic-page-header-main">
        {eyebrow && (
          <span className="u-eyebrow civic-page-eyebrow" data-testid="page-header-eyebrow">
            {eyebrow}
          </span>
        )}
        <h1 className="u-page-title civic-page-title" data-testid="page-header-title">
          {title}
        </h1>
        {description && (
          <p className="u-page-subtitle civic-page-subtitle" data-testid="page-header-description">
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
