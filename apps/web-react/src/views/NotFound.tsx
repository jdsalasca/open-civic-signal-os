import { Button } from "primereact/button";
import { useNavigate } from "react-router-dom";
import { Layout } from "../components/Layout";

export function NotFound() {
  const navigate = useNavigate();
  
  return (
    <Layout>
      <div className="flex flex-column align-items-center justify-content-center mt-8 text-center animate-fade-in">
        <div className="bg-gray-800 border-circle flex align-items-center justify-content-center mb-4 shadow-4" style={{ width: '120px', height: '120px' }}>
          <i className="pi pi-map text-6xl text-cyan-500"></i>
        </div>
        <h1 className="text-6xl font-black text-white m-0 tracking-tighter">404</h1>
        <h2 className="text-2xl font-bold text-gray-300 mt-2 mb-4">Signal Lost in Space</h2>
        <p className="text-gray-500 text-lg mb-6 max-w-25rem line-height-3">
          The civic coordinate you are looking for does not exist or has been moved to a different sector.
        </p>
        <Button 
          label="Back to Insights" 
          icon="pi pi-home" 
          className="bg-cyan-600 border-none px-5 py-3 font-bold"
          onClick={() => navigate("/")} 
        />
      </div>
    </Layout>
  );
}
