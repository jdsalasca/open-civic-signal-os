import { useForm, Controller } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { Password } from "primereact/password";
import { Dropdown } from "primereact/dropdown";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { classNames } from "primereact/utils";

type RegisterForm = {
  username: string;
  email: string;
  password: string;
  role: string;
};

export function Register() {
  const navigate = useNavigate();
  const { control, handleSubmit, formState: { errors } } = useForm<RegisterForm>({
    defaultValues: { username: '', email: '', password: '', role: 'CITIZEN' }
  });

  const roles = [
    { label: 'Citizen (Report & Vote)', value: 'CITIZEN' },
    { label: 'Public Servant (Staff)', value: 'PUBLIC_SERVANT' }
  ];

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
    <div className="flex justify-content-center align-items-center mt-6">
      <Card title="Join Signal OS" subTitle="Create your account to start impacting your community" style={{ width: '100%', maxWidth: '450px' }}>
        <form onSubmit={handleSubmit(onSubmit)} className="p-fluid">
          <div className="field mt-2">
            <span className="p-float-label p-input-icon-left">
              <i className="pi pi-user" />
              <Controller name="username" control={control} rules={{ required: 'Username is required.', minLength: { value: 4, message: 'Min 4 characters' } }} 
                render={({ field, fieldState }) => (
                  <InputText id={field.name} {...field} className={classNames({ 'p-invalid': fieldState.error })} />
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
                  <InputText id={field.name} {...field} className={classNames({ 'p-invalid': fieldState.error })} />
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
                  <Password id={field.name} {...field} toggleMask className={classNames({ 'p-invalid': fieldState.error })} />
                )} 
              />
              <label htmlFor="password">Password</label>
            </span>
            {errors.password && <small className="p-error">{errors.password.message}</small>}
          </div>

          <div className="field mt-4">
            <label htmlFor="role" className="text-xs font-semibold text-gray-500 uppercase ml-1">I am a...</label>
            <Controller name="role" control={control} 
              render={({ field }) => (
                <Dropdown id={field.name} {...field} options={roles} placeholder="Select a role" />
              )} 
            />
          </div>

          <Button type="submit" label="Create Account" className="mt-4" />
          
          <p className="text-center mt-4 text-gray-500 text-sm">
            Already have an account? <span className="text-cyan-400 cursor-pointer font-bold" onClick={() => navigate("/")}>Login here</span>
          </p>
        </form>
      </Card>
    </div>
  );
}
