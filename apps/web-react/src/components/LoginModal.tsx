import { useState } from "react";
import { useNavigate } from "react-router-dom";

type Props = {
  onLogin: (user: string, pass: string) => void;
  onClose: () => void;
};

export function LoginModal({ onLogin, onClose }: Props) {
  const navigate = useNavigate();
  const [credentials, setCredentials] = useState({ user: "", pass: "" });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onLogin(credentials.user, credentials.pass);
  };

  const handleRegisterRedirect = () => {
    onClose();
    navigate("/register");
  };

  return (
    <div className="modal-overlay">
      <form className="login-modal" onSubmit={handleSubmit}>
        <h2>Account Login</h2>
        <input
          type="text"
          placeholder="Username"
          required
          value={credentials.user}
          onChange={(e) => setCredentials({ ...credentials, user: e.target.value })}
        />
        <input
          type="password"
          placeholder="Password"
          required
          value={credentials.pass}
          onChange={(e) => setCredentials({ ...credentials, pass: e.target.value })}
        />
        <div className="modal-actions">
          <button type="submit">Login</button>
          <button type="button" onClick={onClose}>
            Cancel
          </button>
        </div>
        
        <p className="hint" style={{ marginTop: '10px' }}>
          New to Signal OS? <span style={{ color: 'var(--accent-primary)', cursor: 'pointer' }} onClick={handleRegisterRedirect}>Register here</span>
        </p>

        <div className="hint" style={{ lineHeight: '1.4', borderTop: '1px solid rgba(255,255,255,0.1)', paddingTop: '10px' }}>
          <strong>Admin:</strong> admin / admin12345<br/>
          <strong>Staff:</strong> servant / servant2026<br/>
          <strong>Citizen:</strong> citizen / citizen2026
        </div>
      </form>
    </div>
  );
}
