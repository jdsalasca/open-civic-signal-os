import { useForm, Controller } from "react-hook-form";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { Password } from "primereact/password";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { classNames } from "primereact/utils";
import { useAuthStore } from "../store/useAuthStore";
import { Shield } from "lucide-react";
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
      toast.error(err.response?.data?.message || "Invalid credentials.");
    }
  };

  return (
    <div className="flex justify-content-center align-items-center mt-6">
      <Card 
        title={
          <div className="flex align-items-center gap-2">
            <Shield className="text-cyan-400" />
            <span>Staff & Citizen Login</span>
          </div>
        } 
        subTitle="Enter your credentials to access the OS" 
        style={{ width: '100%', maxWidth: '400px' }}
      >
        <form onSubmit={handleSubmit(onSubmit)} className="p-fluid">
          <div className="field mt-2">
            <label htmlFor="username">Username</label>
            <span className="p-input-icon-left">
              <i className="pi pi-user" />
              <Controller name="username" control={control} rules={{ required: 'Username is required.' }} 
                render={({ field, fieldState }) => (
                  <InputText id={field.name} {...field} className={classNames({ 'p-invalid': fieldState.error })} />
                )} 
              />
            </span>
            {errors.username && <small className="p-error">{errors.username.message}</small>}
          </div>

          <div className="field mt-4">
            <label htmlFor="password">Password</label>
            <Controller name="password" control={control} rules={{ required: 'Password is required.' }} 
              render={({ field, fieldState }) => (
                <Password id={field.name} {...field} toggleMask feedback={false} className={classNames({ 'p-invalid': fieldState.error })} />
              )} 
            />
            {errors.password && <small className="p-error">{errors.password.message}</small>}
          </div>

          <Button type="submit" label="Sign In" icon="pi pi-sign-in" className="mt-4" />
          
          <div className="text-center mt-4">
            <p className="text-gray-500 text-sm">
              Don't have an account? <Link to="/register" className="text-cyan-400 font-bold no-underline">Register here</Link>
            </p>
          </div>

          <div className="mt-4 p-3 bg-gray-800 border-round text-xs text-gray-400">
            <div className="mb-1"><strong>SuperAdmin:</strong> admin / admin12345</div>
            <div className="mb-1"><strong>Staff:</strong> servant / servant2026</div>
            <div><strong>Citizen:</strong> citizen / citizen2026</div>
          </div>
        </form>
      </Card>
    </div>
  );
}
