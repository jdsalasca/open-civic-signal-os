import { useForm, Controller } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { Password } from "primereact/password";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { classNames } from "primereact/utils";
import { Layout } from "../components/Layout";
import apiClient from "../api/axios";

type RegisterForm = {
  username: string;
  email: string;
  password: string;
};

export function Register() {
  const navigate = useNavigate();
  const { control, handleSubmit, formState: { errors } } = useForm<RegisterForm>({
    defaultValues: { username: '', email: '', password: '' }
  });

  const onSubmit = async (data: RegisterForm) => {
    try {
      const res = await apiClient.post("/api/auth/register", data);

      if (res.status === 200 || res.status === 201) {
        toast.success("Account created! You can now log in.");
        navigate("/login");
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || "Registration failed. Check your details.";
      toast.error(msg);
    }
  };

  return (
    <Layout>
      <div className="flex justify-content-center align-items-center mt-6">
        <Card title="Join Signal OS" subTitle="Create your citizen account to start impacting your community" style={{ width: '100%', maxWidth: '450px' }}>
          <form onSubmit={handleSubmit(onSubmit)} className="p-fluid">
            <div className="field mt-2">
              <span className="p-float-label p-input-icon-left">
                <i className="pi pi-user" />
                <Controller name="username" control={control} rules={{ required: 'Username is required.', minLength: { value: 4, message: 'Min 4 characters' } }} 
                  render={({ field, fieldState }) => (
                    <InputText id="username" {...field} className={classNames({ 'p-invalid': fieldState.error })} />
                  )} 
                />
                <label htmlFor="username">Username</label>
              </span>
              {errors.username && <small className="p-error">{errors.username.message}</small>}
            </div>

            <div className="field mt-4">
              <span className="p-float-label p-input-icon-left">
                <i className="pi pi-envelope" />
                <Controller name="email" control={control} rules={{ required: 'Email is required.', pattern: { value: /^\S+@\S+$/i, message: 'Invalid email' } }} 
                  render={({ field, fieldState }) => (
                    <InputText id="email" {...field} className={classNames({ 'p-invalid': fieldState.error })} />
                  )} 
                />
                <label htmlFor="email">Email Address</label>
              </span>
              {errors.email && <small className="p-error">{errors.email.message}</small>}
            </div>

            <div className="field mt-4">
              <span className="p-float-label">
                <Controller name="password" control={control} rules={{ required: 'Password is required.', minLength: { value: 8, message: 'Min 8 characters' } }} 
                  render={({ field, fieldState }) => (
                    <Password id="password" {...field} toggleMask inputId="password-input" className={classNames('w-full', { 'p-invalid': fieldState.error })} />
                  )} 
                />
                <label htmlFor="password">Password</label>
              </span>
              {errors.password && <small className="p-error">{errors.password.message}</small>}
            </div>

            <div className="mt-4 p-3 bg-blue-900 border-round text-xs text-blue-100 flex align-items-center gap-2">
              <i className="pi pi-shield"></i>
              <span>Default Role: Citizen</span>
            </div>

            <Button type="submit" label="Create Account" className="mt-4 py-3 font-bold" />
            
            <p className="text-center mt-4 text-gray-500 text-sm">
              Already have an account? <span className="text-cyan-400 cursor-pointer font-bold" onClick={() => navigate("/login")}>Login here</span>
            </p>
          </form>
        </Card>
      </div>
    </Layout>
  );
}
