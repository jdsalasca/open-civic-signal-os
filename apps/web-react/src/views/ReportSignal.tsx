import { useForm, Controller } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
import { InputNumber } from "primereact/inputnumber";
import { Dropdown } from "primereact/dropdown";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { classNames } from "primereact/utils";
import { Layout } from "../components/Layout";
import { useState } from "react";
import { UserRole } from "../types";

type ReportForm = {
  title: string;
  description: string;
  category: string;
  urgency: number;
  impact: number;
  affectedPeople: number;
};

export function ReportSignal() {
  const navigate = useNavigate();
  const [auth] = useState<{user: string, role: UserRole} | null>(() => {
    const saved = localStorage.getItem("civic_auth_data");
    return saved ? JSON.parse(saved) : null;
  });

  const { control, handleSubmit, formState: { errors } } = useForm<ReportForm>({
    defaultValues: { title: '', description: '', category: 'infrastructure', urgency: 3, impact: 3, affectedPeople: 10 }
  });

  const categories = [
    { label: 'Infrastructure', value: 'infrastructure' },
    { label: 'Safety', value: 'safety' },
    { label: 'Education', value: 'education' },
    { label: 'Environment', value: 'environment' }
  ];

  const onSubmit = async (data: ReportForm) => {
    const token = localStorage.getItem("civic_auth_token");
    try {
      const res = await fetch("/api/signals", {
        method: "POST",
        headers: { 
          "Content-Type": "application/json",
          "Authorization": token ? `Basic ${token}` : ""
        },
        body: JSON.stringify(data)
      });

      if (res.ok) {
        toast.success("Signal reported successfully!");
        navigate("/");
      } else {
        const err = await res.json();
        toast.error(`Error: ${err.message || "Failed to submit"}`);
      }
    } catch (err) {
      toast.error("Network error reporting signal.");
    }
  };

  return (
    <Layout auth={auth} onLogout={() => { localStorage.clear(); window.location.href='/'; }} onLoginClick={() => navigate('/')}>
      <div className="flex justify-content-center">
        <Card title="Report Civic Signal" subTitle="Provide details about the issue in your community" style={{ width: '100%', maxWidth: '800px' }}>
          <form onSubmit={handleSubmit(onSubmit)} className="p-fluid grid">
            <div className="field col-12">
              <label htmlFor="title" className="font-bold">Short Title</label>
              <Controller name="title" control={control} rules={{ required: 'Title is required.' }} 
                render={({ field, fieldState }) => (
                  <InputText id={field.name} {...field} placeholder="e.g. Broken bench in Central Park" className={classNames({ 'p-invalid': fieldState.error })} />
                )} 
              />
              {errors.title && <small className="p-error">{errors.title.message}</small>}
            </div>

            <div className="field col-12">
              <label htmlFor="description" className="font-bold">Detailed Description</label>
              <Controller name="description" control={control} 
                render={({ field }) => (
                  <InputTextarea id={field.name} {...field} rows={4} placeholder="Describe the problem..." autoResize />
                )} 
              />
            </div>

            <div className="field col-12 md:col-6">
              <label htmlFor="category" className="font-bold">Category</label>
              <Controller name="category" control={control} 
                render={({ field }) => (
                  <Dropdown id={field.name} {...field} options={categories} />
                )} 
              />
            </div>

            <div className="field col-12 md:col-2">
              <label htmlFor="urgency" className="font-bold">Urgency (1-5)</label>
              <Controller name="urgency" control={control} 
                render={({ field }) => (
                  <InputNumber id={field.name} value={field.value} onValueChange={(e) => field.onChange(e.value)} min={1} max={5} showButtons />
                )} 
              />
            </div>

            <div className="field col-12 md:col-2">
              <label htmlFor="impact" className="font-bold">Impact (1-5)</label>
              <Controller name="impact" control={control} 
                render={({ field }) => (
                  <InputNumber id={field.name} value={field.value} onValueChange={(e) => field.onChange(e.value)} min={1} max={5} showButtons />
                )} 
              />
            </div>

            <div className="field col-12 md:col-2">
              <label htmlFor="affectedPeople" className="font-bold">Affected</label>
              <Controller name="affectedPeople" control={control} 
                render={({ field }) => (
                  <InputNumber id={field.name} value={field.value} onValueChange={(e) => field.onChange(e.value)} min={1} />
                )} 
              />
            </div>

            <div className="col-12 mt-4">
              <Button type="submit" label="Submit Signal to Prioritization" icon="pi pi-check" severity="success" size="large" />
            </div>
          </form>
        </Card>
      </div>
    </Layout>
  );
}
