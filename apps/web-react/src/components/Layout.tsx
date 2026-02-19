import React, { ReactNode } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Button } from "primereact/button";
import { UserRole } from "../types";

type Props = {
  children: ReactNode;
  auth: { user: string; role: UserRole } | null;
  onLogout: () => void;
  onLoginClick: () => void;
};

export function Layout({ children, auth, onLogout, onLoginClick }: Props) {
  const navigate = useNavigate();
  const isStaff = auth?.role === "PUBLIC_SERVANT" || auth?.role === "SUPER_ADMIN";

  return (
    <div className="min-h-screen flex flex-column">
      <nav className="surface-card p-3 shadow-2 flex justify-content-between align-items-center border-bottom-1 border-gray-800">
        <div className="flex align-items-center gap-4">
          <Link to="/" className="text-2xl font-bold text-cyan-400 no-underline tracking-tight">
            SIGNAL OS
          </Link>
          <div className="hidden md:flex gap-3">
            <Button label="Dashboard" text className="text-gray-300" onClick={() => navigate("/")} />
            <Button label="Report Issue" text className="text-gray-300" onClick={() => navigate("/report")} />
          </div>
        </div>

        <div className="flex align-items-center gap-3">
          {auth ? (
            <>
              <div className="flex flex-column align-items-end mr-2">
                <span className="text-sm font-medium text-white">{auth.user}</span>
                <span className="text-xs text-cyan-500 font-bold">{auth.role.replace("ROLE_", "")}</span>
              </div>
              <Button 
                icon="pi pi-sign-out" 
                label="Logout" 
                size="small" 
                outlined 
                severity="danger" 
                onClick={onLogout} 
              />
            </>
          ) : (
            <Button 
              icon="pi pi-user" 
              label="Operator Login" 
              size="small" 
              onClick={onLoginClick} 
            />
          )}
        </div>
      </nav>

      <main className="flex-grow-1 p-4 md:p-6 bg-gray-900 overflow-auto">
        {children}
      </main>

      <footer className="surface-card p-3 text-center border-top-1 border-gray-800 text-gray-500 text-xs">
        &copy; 2026 Open Civic Signal OS. Empowering Communities Through Transparency.
      </footer>
    </div>
  );
}
