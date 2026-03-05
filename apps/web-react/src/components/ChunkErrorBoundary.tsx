import { Component, ErrorInfo, ReactNode } from "react";
import { withTranslation, WithTranslation } from "react-i18next";
import { CivicButton } from "./ui/CivicButton";
import { CivicCard } from "./ui/CivicCard";

interface Props extends WithTranslation {
  children: ReactNode;
}

interface State {
  hasError: boolean;
}

class ChunkErrorBoundaryBase extends Component<Props, State> {
  public state: State = {
    hasError: false
  };

  public static getDerivedStateFromError(error: Error): State {
    const isChunkLoadError = error.message.includes("Failed to fetch dynamically imported module") || 
                             error.message.includes("Loading chunk");
    
    if (isChunkLoadError) {
      return { hasError: true };
    }
    return { hasError: false };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    if (this.state.hasError) {
      console.error("CRITICAL: Strategic module load failure detected.", error, errorInfo);
    }
  }

  private handleReload = () => {
    window.location.reload();
  };

  public render() {
    const { t } = this.props;

    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex align-items-center justify-content-center bg-app p-4">
          <CivicCard className="w-full max-w-25rem">
            <div className="text-center">
              <i className="pi pi-refresh text-brand-primary text-5xl mb-4 motion-pulse-soft"></i>
              <h2 className="text-main font-black mb-2">{t('exceptions.chunk_title')}</h2>
              <p className="text-secondary mb-5 line-height-3">
                {t('exceptions.chunk_desc')}
              </p>
              <CivicButton
                label={t('exceptions.chunk_button')} 
                icon="pi pi-sync" 
                className="w-full py-3 font-bold"
                onClick={this.handleReload} 
              />
            </div>
          </CivicCard>
        </div>
      );
    }

    return this.props.children;
  }
}

export const ChunkErrorBoundary = withTranslation()(ChunkErrorBoundaryBase);
