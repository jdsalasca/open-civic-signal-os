import { useNavigate } from "react-router-dom";

export function NotFound() {
  const navigate = useNavigate();
  return (
    <div style={{ textAlign: 'center', padding: '100px 20px' }}>
      <h1>404</h1>
      <p>The page you are looking for does not exist in Signal OS.</p>
      <button className="relay-btn" style={{ marginTop: '20px' }} onClick={() => navigate("/")}>
        Back to Safety (Dashboard)
      </button>
    </div>
  );
}
