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
      const res = await apiClient.post("auth/register", data);

      if (res.status === 200 || res.status === 201) {
        toast.success("Account created! You can now log in.");
        navigate("/login");
      }
    } catch (err: any) {
      toast.error(err.friendlyMessage || "Registration failed. Please check your data.");
    }
  };

  return (
    <Layout authMode>
      <div className="flex justify-content-center align-items-center mt-6">
        <Card 
          title={<div className="text-center w-full">Join Signal OS</div>} 
          subTitle={<div className="text-center w-full">Create your citizen account</div>} 
          style={{ width: '100%', maxWidth: '450px' }}
          data-testid="register-card"
        >
          <form onSubmit={handleSubmit(onSubmit)} className="p-fluid" aria-label="Registration Form">
            <div className="field mt-2">
              <span className="p-float-label p-input-icon-left">
                <i className="pi pi-user text-cyan-500" />
                <Controller name="username" control={control} rules={{ required: 'Username is required.', minLength: { value: 4, message: 'Min 4 characters' } }} 
                  render={({ field, fieldState }) => (
                    <InputText 
                      id="username" 
                      {...field} 
                      className={classNames('py-3 pl-5', { 'p-invalid': fieldState.error })} 
                      data-testid="register-username-input"
                    />
                  )} 
                />
                <label htmlFor="username">Username</label>
              </span>
              {errors.username && <small className="p-error block mt-1" data-testid="register-username-error">{errors.username.message}</small>}
            </div>

            <div className="field mt-5">
              <span className="p-float-label p-input-icon-left">
                <i className="pi pi-envelope text-cyan-500" />
                <Controller name="email" control={control} rules={{ required: 'Email is required.', pattern: { value: /^\S+@\S+$/i, message: 'Invalid email' } }} 
                  render={({ field, fieldState }) => (
                    <InputText 
                      id="email" 
                      {...field} 
                      className={classNames('py-3 pl-5', { 'p-invalid': fieldState.error })} 
                      data-testid="register-email-input"
                    />
                  )} 
                />
                <label htmlFor="email">Email Address</label>
              </span>
              {errors.email && <small className="p-error block mt-1" data-testid="register-email-error">{errors.email.message}</small>}
            </div>

            <div className="field mt-5">
              <span className="p-float-label">
                <Controller name="password" control={control} rules={{ required: 'Password is required.', minLength: { value: 8, message: 'Min 8 characters' } }} 
                  render={({ field, fieldState }) => (
                    <Password 
                      id="password" 
                      {...field} 
                      toggleMask 
                      inputId="password-input" 
                      inputClassName="py-3" 
                      className={classNames('w-full', { 'p-invalid': fieldState.error })} 
                      data-testid="register-password-input"
                    />
                  )} 
                />
                <label htmlFor="password">Password</label>
              </span>
              {errors.password && <small className="p-error block mt-1" data-testid="register-password-error">{errors.password.message}</small>}
            </div>

            <div className="mt-5 p-3 bg-blue-900-alpha-20 border-round text-xs text-blue-300 flex align-items-center gap-2 border-1 border-blue-800">
              <i className="pi pi-shield"></i>
              <span className="font-bold uppercase tracking-wider">Default Security Clearance: Citizen</span>
            </div>

            <Button 
              type="submit" 
              label="Create Account" 
              className="p-button-primary mt-5 py-3 font-bold" 
              data-testid="register-submit-button"
              aria-label="Submit Registration"
            />
            
            <p className="text-center mt-5 text-gray-500 text-sm font-medium">
              Already a member? <span className="text-cyan-400 cursor-pointer font-bold hover:underline" onClick={() => navigate("/login")} data-testid="go-to-login">Sign in here</span>
            </p>
          </form>
        </Card>
      </div>
    </Layout>
  );
}
