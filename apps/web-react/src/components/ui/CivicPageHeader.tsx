interface CivicPageHeaderProps {
  title: string;
  description: string;
  className?: string;
}

export function CivicPageHeader({ title, description, className }: CivicPageHeaderProps) {
  return (
    <div className={className ?? "mb-8"}>
      <h1 className="text-5xl font-black mb-2 text-main tracking-tighter">{title}</h1>
      <p className="text-secondary text-lg font-medium">{description}</p>
    </div>
  );
}
