import { Component, ErrorInfo, ReactNode } from "react";
import { Button } from "primereact/button";
import { Card } from "primereact/card";

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
}

export class ChunkErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false
  };

  public static getDerivedStateFromError(error: Error): State {
    // FE-1: Detect dynamic import failures (MIME mismatch or network loss)
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
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex align-items-center justify-content-center bg-gray-900 p-4">
          <Card className="exception-card shadow-8 border-cyan-900 border-1 w-full max-w-25rem">
            <div className="text-center">
              <i className="pi pi-refresh text-cyan-500 text-5xl mb-4"></i>
              <h2 className="text-white font-black mb-2">Protocol Resynchronization</h2>
              <p className="text-gray-400 mb-5 line-height-3">
                A system module failed to load due to a stale cache or temporary signal loss. 
                Synchronizing the application state is required.
              </p>
              <Button 
                label="Refresh Intelligence" 
                icon="pi pi-sync" 
                className="p-button-primary w-full py-3 font-bold" 
                onClick={this.handleReload} 
              />
            </div>
          </Card>
        </div>
      );
    }

    return this.props.children; // Fixed: use this.props.children
  }
}
