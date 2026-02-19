import { useNavigate } from "react-router-dom";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { Layout } from "../components/Layout";

export function NotFound() {
  const navigate = useNavigate();

  return (
    <Layout authMode>
      <div className="flex align-items-center justify-content-center mt-8">
        <Card className="exception-card shadow-8 border-1 border-white-alpha-10 animate-fade-in" style={{ maxWidth: '450px' }}>
          <div className="text-center">
            <div className="inline-flex align-items-center justify-content-center bg-gray-800 border-circle mb-4 shadow-4" style={{ width: '100px', height: '100px' }}>
              <i className="pi pi-map text-5xl text-cyan-500"></i>
            </div>
            <h1 className="text-6xl font-black text-white m-0 tracking-tighter">404</h1>
            <h2 className="text-2xl font-bold text-gray-200 mt-2 mb-4">Coordinate Not Found</h2>
            <p className="text-gray-400 mb-6 line-height-3">
              The civic resource you are looking for is outside the mapped territory of Signal OS.
            </p>
            
            <div className="flex flex-column gap-3">
              <Button 
                label="Return to Command Center" 
                icon="pi pi-home" 
                className="p-button-primary py-3 font-bold text-lg" 
                onClick={() => navigate("/")} 
                data-testid="notfound-home-button"
              />
              {/* FE-3: Hardened contrast for secondary action */}
              <Button 
                label="Verify Connection" 
                icon="pi pi-wifi" 
                text 
                className="text-gray-100 hover:bg-white-alpha-10 font-bold py-3 border-1 border-white-alpha-20" 
                onClick={() => window.location.reload()} 
              />
            </div>
          </div>
        </Card>
      </div>
    </Layout>
  );
}
