import { Skeleton } from 'primereact/skeleton';
import { classNames } from 'primereact/utils';

interface CivicSkeletonProps {
  type: 'card' | 'table-row' | 'metric' | 'text';
  count?: number;
  className?: string;
}

export function CivicSkeleton({ type, count = 1, className }: CivicSkeletonProps) {
  const items = Array.from({ length: count });

  if (type === 'card') {
    return (
      <div className={classNames("grid gap-4", className)}>
        {items.map((_, i) => (
          <div key={i} className="col-12 md:col-6 lg:col-4">
            <div className="glass-panel p-5 border-round-3xl overflow-hidden relative">
              <div className="absolute inset-0 shimmer-effect opacity-20 pointer-events-none"></div>
              <Skeleton width="30%" height="1rem" className="mb-4 bg-white-alpha-10" />
              <Skeleton width="100%" height="2rem" className="mb-2 bg-white-alpha-10" />
              <Skeleton width="80%" height="1.5rem" className="mb-6 bg-white-alpha-10" />
              <div className="flex justify-content-between">
                <Skeleton width="40%" height="2.5rem" borderRadius="12px" className="bg-white-alpha-10" />
                <Skeleton shape="circle" size="2.5rem" className="bg-white-alpha-10" />
              </div>
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (type === 'metric') {
    return (
      <div className="grid gap-4 mb-8">
        {items.map((_, i) => (
          <div key={i} className="col-12 md:col-3">
            <div className="glass-panel p-4 border-round-2xl flex justify-content-between align-items-center overflow-hidden relative">
              <div className="absolute inset-0 shimmer-effect opacity-20 pointer-events-none"></div>
              <div className="flex flex-column gap-2 w-full">
                <Skeleton width="40%" height="0.75rem" className="bg-white-alpha-10" />
                <Skeleton width="60%" height="2.5rem" className="bg-white-alpha-10" />
              </div>
              <Skeleton shape="circle" size="3.5rem" className="bg-white-alpha-10" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className={classNames("flex flex-column gap-3", className)}>
      {items.map((_, i) => (
        <div key={i} className="relative overflow-hidden border-round-xl">
          <div className="absolute inset-0 shimmer-effect opacity-10 pointer-events-none"></div>
          <Skeleton width="100%" height="3.5rem" borderRadius="12px" className="bg-white-alpha-5" />
        </div>
      ))}
    </div>
  );
}
