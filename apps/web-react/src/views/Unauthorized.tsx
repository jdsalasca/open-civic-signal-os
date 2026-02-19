import { Button } from "primereact/button";
import { useNavigate } from "react-router-dom";
import { ShieldAlert } from "lucide-react";

export function Unauthorized() {
  const navigate = useNavigate();
  return (
    <div className="flex flex-column align-items-center justify-content-center mt-8 text-center">
      <ShieldAlert size={80} className="text-red-500 mb-4" />
      <h1 className="text-4xl text-white">Access Denied</h1>
      <p className="text-gray-500 text-lg mb-6">You do not have the required permissions to view this section.</p>
      <Button label="Back to Dashboard" icon="pi pi-home" onClick={() => navigate("/")} />
    </div>
  );
}
