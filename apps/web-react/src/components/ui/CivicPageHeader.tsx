interface CivicPageHeaderProps {
  title: string;
  description: string;
  className?: string;
}

export function CivicPageHeader({ title, description, className }: CivicPageHeaderProps) {
  return (
    <div className={className ?? "mb-8"}>
      <h1 className="u-page-title text-4xl md:text-5xl font-black mb-2">{title}</h1>
      <p className="u-page-subtitle text-lg font-medium">{description}</p>
    </div>
  );
}
