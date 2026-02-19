import { useForm, Controller } from "react-hook-form";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { Password } from "primereact/password";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { classNames } from "primereact/utils";
import { useAuthStore } from "../store/useAuthStore";
import { ShieldCheck } from "lucide-react";
import { Layout } from "../components/Layout";
import apiClient from "../api/axios";

type LoginForm = {
  username: string;
  password: string;
};

export function Login() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((state) => state.setAuth);
  const { control, handleSubmit, formState: { errors } } = useForm<LoginForm>({
    defaultValues: { username: '', password: '' }
  });

  const onSubmit = async (data: LoginForm) => {
    try {
      const res = await apiClient.post("/api/auth/login", data);
      setAuth(res.data);
      toast.success(`Welcome back, ${res.data.username}!`);
      navigate("/");
    } catch (err: any) {
      const msg = err.response?.data?.message || "Invalid username or password.";
      toast.error(msg);
    }
  };

  return (
    <Layout>
      <div className="flex justify-content-center align-items-center mt-6">
        <Card 
          className="shadow-8 border-round-2xl border-1 border-white-alpha-10 animate-fade-in"
          style={{ width: '100%', maxWidth: '420px', background: 'var(--bg-surface)' }}
        >
          <div className="text-center mb-5">
            <ShieldCheck size={60} className="text-cyan-500 mb-3" />
            <h1 className="text-3xl font-black text-white m-0 tracking-tight">Access Portal</h1>
            <p className="text-gray-500 mt-2">Open Civic Signal OS Management</p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="p-fluid">
            <div className="field mb-4">
              <label htmlFor="username" className="block text-gray-400 font-bold mb-2 text-xs uppercase">Username</label>
              <span className="p-input-icon-left">
                <i className="pi pi-user text-cyan-500" />
                <Controller 
                  name="username" 
                  control={control} 
                  rules={{ required: 'Username is required.' }} 
                  render={({ field, fieldState }) => (
                    <InputText 
                      id="username" 
                      {...field} 
                      autoFocus
                      placeholder="Enter your username"
                      className={classNames('p-inputtext-lg py-3 pl-5', { 'p-invalid': fieldState.error })} 
                    />
                  )} 
                />
              </span>
              {errors.username && <small className="p-error block mt-1">{errors.username.message}</small>}
            </div>

            <div className="field mb-5">
              <label htmlFor="password" name="password-label" className="block text-gray-400 font-bold mb-2 text-xs uppercase">Password</label>
              <Controller 
                name="password" 
                control={control} 
                rules={{ required: 'Password is required.' }} 
                render={({ field, fieldState }) => (
                  <Password 
                    id="password" 
                    {...field} 
                    toggleMask 
                    feedback={false} 
                    placeholder="Enter your password"
                    inputClassName="p-inputtext-lg py-3"
                    className={classNames('w-full', { 'p-invalid': fieldState.error })} 
                  />
                )} 
              />
              {errors.password && <small className="p-error block mt-1">{errors.password.message}</small>}
            </div>

            <Button 
              type="submit" 
              label="Sign In to OS" 
              icon="pi pi-sign-in" 
              className="py-3 font-bold text-lg shadow-4 border-none bg-cyan-600 hover:bg-cyan-500" 
            />
            
            <div className="text-center mt-5">
              <span className="text-gray-600 mr-2">Don't have an account?</span>
              <Link to="/register" className="text-cyan-400 font-bold no-underline hover:underline">Create one</Link>
            </div>

            {/* P0-6: Removed Demo Credentials box from UI */}
          </form>
        </Card>
      </div>
    </Layout>
  );
}
