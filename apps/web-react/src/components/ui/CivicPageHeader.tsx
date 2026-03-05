import { useTranslation } from "react-i18next";

interface CivicPageHeaderProps {
  title: string;
  description: string;
  className?: string;
  eyebrow?: string;
}

export function CivicPageHeader({ title, description, className, eyebrow }: CivicPageHeaderProps) {
  const { t } = useTranslation();
  return (
    <div className={`civic-page-header ${className ?? "mb-8"}`}>
      <div className="u-pill mb-4">
        <i className="pi pi-sparkles text-brand-primary"></i>
        {eyebrow ?? t("common.workspace_context")}
      </div>
      <h1 className="u-page-title civic-page-title text-4xl md:text-6xl font-black mb-3">{title}</h1>
      <p className="u-page-subtitle civic-page-subtitle text-lg font-medium line-height-3">{description}</p>
    </div>
  );
}
