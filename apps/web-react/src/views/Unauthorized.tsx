import { Button } from "primereact/button";
import { useNavigate } from "react-router-dom";
import { ShieldAlert } from "lucide-react";
import { Layout } from "../components/Layout";

export function Unauthorized() {
  const navigate = useNavigate();
  return (
    <Layout>
      <div className="flex flex-column align-items-center justify-content-center mt-8 text-center animate-fade-in">
        <div className="bg-orange-900 border-circle flex align-items-center justify-content-center mb-4 shadow-4" style={{ width: '120px', height: '120px', background: 'rgba(251, 146, 60, 0.1)', border: '2px solid rgba(251, 146, 60, 0.2)' }}>
          <ShieldAlert size={60} className="text-orange-500" />
        </div>
        <h1 className="text-4xl font-black text-white m-0 tracking-tight">Access Restricted</h1>
        <p className="text-gray-400 text-lg mt-3 mb-6 max-w-25rem line-height-3">
          Your current security clearance does not allow access to this civic coordinate.
        </p>
        <div className="flex gap-3">
          <Button 
            label="Return Home" 
            icon="pi pi-home" 
            outlined
            className="border-gray-700 text-gray-300 px-5"
            onClick={() => navigate("/")} 
          />
          <Button 
            label="Switch Account" 
            icon="pi pi-user" 
            className="bg-cyan-600 border-none px-5 font-bold"
            onClick={() => navigate("/login")} 
          />
        </div>
      </div>
    </Layout>
  );
}
