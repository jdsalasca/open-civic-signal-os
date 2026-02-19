import { useState } from "react";

type Props = {
  onLogin: (user: string, pass: string) => void;
  onClose: () => void;
};

export function LoginModal({ onLogin, onClose }: Props) {
  const [credentials, setCredentials] = useState({ user: "", pass: "" });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onLogin(credentials.user, credentials.pass);
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
        <div className="hint" style={{ lineHeight: '1.4' }}>
          <strong>Staff:</strong> servant / servant2026<br/>
          <strong>Citizen:</strong> citizen / citizen2026
        </div>
      </form>
    </div>
  );
}
