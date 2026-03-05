import { useNavigate } from "react-router-dom";
import { CivicButton } from "../components/ui/CivicButton";
import { CivicCard } from "../components/ui/CivicCard";
import { Layout } from "../components/Layout";

export function Unauthorized() {
  const navigate = useNavigate();

  return (
    <Layout authMode>
      <div className="min-h-screen flex justify-content-center align-items-center p-4">
        <CivicCard className="w-full max-w-30rem text-center animate-fade-up" padding="lg">
          <div className="mb-8">
            <div className="inline-flex align-items-center justify-content-center p-4 bg-status-progress-alpha-10 border-round-3xl mb-6 border-1 border-status-progress-alpha-20">
              <i className="pi pi-lock text-5xl text-status-progress"></i>
            </div>
            <h1 className="text-4xl font-black text-main m-0 tracking-tighter">Clearance Denied</h1>
            <p className="text-secondary mt-4 mb-0 font-medium leading-relaxed">
              Your current identity level does not have the required authorization to access this operational sector.
            </p>
          </div>

          <div className="flex flex-column gap-3">
            <CivicButton 
              label="Request Elevation" 
              icon="pi pi-shield" 
              className="py-4 text-base" 
              onClick={() => navigate("/settings")}
              glow
            />
            <CivicButton 
              label="Back to Safety" 
              variant="secondary"
              icon="pi pi-arrow-left" 
              className="py-4 text-base"
              onClick={() => navigate("/")}
            />
          </div>
        </CivicCard>
      </div>
    </Layout>
  );
}
