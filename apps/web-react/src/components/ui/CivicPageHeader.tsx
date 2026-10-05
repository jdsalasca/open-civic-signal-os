import { useTranslation } from "react-i18next";

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
}

export function CivicPageHeader({ title, description, className, eyebrow }: CivicPageHeaderProps) {
  const { t } = useTranslation();
  return (
    <div className={`civic-page-header ${className ?? "mb-8"}`}>
      <div className="u-pill mb-4">
        <i className="pi pi-sparkles text-brand-primary"></i>
        <span className="u-eyebrow" data-testid="page-header-eyebrow">
          {eyebrow ?? t("common.workspace_context")}
        </span>
      </div>
      <h1 className="u-page-title civic-page-title text-4xl md:text-6xl font-black mb-3" data-testid="page-header-title">
        {title}
      </h1>
      {description && (
        <p
          className="u-page-subtitle civic-page-subtitle text-lg font-medium line-height-3"
          data-testid="page-header-description"
        >
          {description}
        </p>
      )}
    </div>
  );
}