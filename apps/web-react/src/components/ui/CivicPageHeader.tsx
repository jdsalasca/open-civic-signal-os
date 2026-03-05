interface CivicPageHeaderProps {
  title: string;
  description: string;
  className?: string;
}

export function CivicPageHeader({ title, description, className }: CivicPageHeaderProps) {
  return (
    <div className={className ?? "mb-8"}>
      <div className="u-pill mb-4">
        <i className="pi pi-sparkles text-brand-primary"></i>
        Community workspace
      </div>
      <h1 className="u-page-title text-4xl md:text-6xl font-black mb-3">{title}</h1>
      <p className="u-page-subtitle text-lg font-medium line-height-3">{description}</p>
    </div>
  );
}
