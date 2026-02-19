import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { UserCircle, Mail, Lock, Shield } from "lucide-react";

type RegisterForm = {
  username: string;
  email: string;
  password: string;
  role: string;
};

export function Register() {
  const navigate = useNavigate();
  const { register, handleSubmit, formState: { errors } } = useForm<RegisterForm>();

  const onSubmit = async (data: RegisterForm) => {
    try {
      const res = await fetch("/api/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
      });

      if (res.ok) {
        toast.success("Account created! You can now log in.");
        navigate("/");
      } else {
        const err = await res.json();
        toast.error(err.message || "Registration failed.");
      }
    } catch (err) {
      toast.error("Network error during registration.");
    }
  };

  return (
    <div className="form-container" style={{ maxWidth: '450px' }}>
      <header className="main-header" style={{ textAlign: 'center', display: 'block' }}>
        <Shield size={48} style={{ color: 'var(--accent-primary)', marginBottom: '16px' }} />
        <h1>Join Signal OS</h1>
        <p>Create your account to start impacting your community.</p>
      </header>

      <form className="report-form card" onSubmit={handleSubmit(onSubmit)}>
        <div className="input-group">
          <label><UserCircle size={14} inline /> Username</label>
          <input 
            {...register("username", { required: "Username is required", minLength: { value: 4, message: "Min 4 chars" } })}
            placeholder="Choose a public username"
          />
          {errors.username && <span className="small-note" style={{ color: '#ef4444' }}>{errors.username.message}</span>}
        </div>

        <div className="input-group">
          <label><Mail size={14} inline /> Email Address</label>
          <input 
            {...register("email", { required: "Email is required", pattern: { value: /^\S+@\S+$/i, message: "Invalid email" } })}
            placeholder="your@email.com"
          />
          {errors.email && <span className="small-note" style={{ color: '#ef4444' }}>{errors.email.message}</span>}
        </div>

        <div className="input-group">
          <label><Lock size={14} inline /> Password</label>
          <input 
            type="password"
            {...register("password", { required: "Password is required", minLength: { value: 8, message: "Min 8 characters" } })}
            placeholder="Min 8 characters"
          />
          {errors.password && <span className="small-note" style={{ color: '#ef4444' }}>{errors.password.message}</span>}
        </div>

        <div className="input-group">
          <label>I am a...</label>
          <select {...register("role")}>
            <option value="CITIZEN">Citizen (Report & Vote)</option>
            <option value="PUBLIC_SERVANT">Public Servant (Staff)</option>
          </select>
        </div>

        <button type="submit" className="relay-btn" style={{ width: '100%', marginTop: '10px' }}>
          Create Account
        </button>
        
        <p className="hint" style={{ marginTop: '20px' }}>
          Already have an account? <span style={{ color: 'var(--accent-primary)', cursor: 'pointer' }} onClick={() => navigate("/")}>Login here</span>
        </p>
      </form>
    </div>
  );
}
