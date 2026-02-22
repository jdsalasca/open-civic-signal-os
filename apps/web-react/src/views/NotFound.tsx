import { useNavigate } from "react-router-dom";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { Layout } from "../components/Layout";

export function NotFound() {
  const navigate = useNavigate();

  return (
    <Layout authMode>
      <div className="min-h-screen flex justify-content-center align-items-center p-4">
        <CivicCard className="w-full max-w-30rem text-center animate-fade-up" padding="lg">
          <div className="mb-8">
            <div className="inline-flex align-items-center justify-content-center p-4 bg-status-rejected-alpha-10 border-round-3xl mb-6 border-1 border-status-rejected-alpha-20">
              <i className="pi pi-map text-5xl text-status-rejected"></i>
            </div>
            <h1 className="text-6xl font-black text-main m-0 tracking-tighter">404</h1>
            <h2 className="text-2xl font-bold text-main mt-2 tracking-tight">Sector Not Found</h2>
            <p className="text-secondary mt-4 mb-0 font-medium leading-relaxed">
              The coordinate you are trying to access does not exist in the current civic grid.
            </p>
          </div>

          <div className="flex flex-column gap-3">
            <CivicButton 
              label="Return to Command Center" 
              icon="pi pi-home" 
              className="py-4 text-base" 
              onClick={() => navigate("/")}
              glow
            />
            <CivicButton 
              label="Report System Anomaly" 
              variant="ghost"
              icon="pi pi-info-circle" 
              className="text-xs" 
            />
          </div>
        </CivicCard>
      </div>
    </Layout>
  );
}
