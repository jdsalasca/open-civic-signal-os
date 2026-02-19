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
import apiClient from "../api/axios";

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
    try {
      // UX-001: Standardized path
      const res = await apiClient.post("signals", data);
      if (res.status === 200 || res.status === 201) {
        toast.success("Signal reported successfully!");
        navigate("/");
      }
    } catch (err: any) {
      toast.error(err.friendlyMessage || "Failed to submit signal.");
    }
  };

  return (
    <Layout>
      <div className="flex justify-content-center animate-fade-in">
        <Card title="Report Civic Signal" subTitle="Provide details about the issue in your community" style={{ width: '100%', maxWidth: '800px' }}>
          <form onSubmit={handleSubmit(onSubmit)} className="p-fluid grid">
            <div className="field col-12 mb-4">
              <label htmlFor="title" className="block text-gray-400 font-bold mb-2 text-xs uppercase">Short Title</label>
              <Controller name="title" control={control} rules={{ required: 'Title is required.' }} 
                render={({ field, fieldState }) => (
                  <InputText id="title" {...field} placeholder="e.g. Broken bench in Central Park" className={classNames('py-3', { 'p-invalid': fieldState.error })} />
                )} 
              />
              {errors.title && <small className="p-error block mt-1">{errors.title.message}</small>}
            </div>

            <div className="field col-12 mb-4">
              <label htmlFor="description" className="block text-gray-400 font-bold mb-2 text-xs uppercase">Detailed Description</label>
              <Controller name="description" control={control} 
                render={({ field }) => (
                  <InputTextarea id="description" {...field} rows={4} placeholder="Describe the problem..." autoResize className="bg-gray-900 border-gray-800" />
                )} 
              />
            </div>

            <div className="field col-12 md:col-6 mb-4">
              <label htmlFor="category" className="block text-gray-400 font-bold mb-2 text-xs uppercase">Category</label>
              <Controller name="category" control={control} 
                render={({ field }) => (
                  <Dropdown id="category" {...field} options={categories} className="bg-gray-900 border-gray-800 py-1" />
                )} 
              />
            </div>

            <div className="field col-12 md:col-2 mb-4">
              <label htmlFor="urgency" className="block text-gray-400 font-bold mb-2 text-xs uppercase">Urgency</label>
              <Controller name="urgency" control={control} 
                render={({ field }) => (
                  <InputNumber id="urgency" value={field.value} onValueChange={(e) => field.onChange(e.value)} min={1} max={5} showButtons inputClassName="bg-gray-900 border-gray-800" />
                )} 
              />
            </div>

            <div className="field col-12 md:col-2 mb-4">
              <label htmlFor="impact" className="block text-gray-400 font-bold mb-2 text-xs uppercase">Impact</label>
              <Controller name="impact" control={control} 
                render={({ field }) => (
                  <InputNumber id="impact" value={field.value} onValueChange={(e) => field.onChange(e.value)} min={1} max={5} showButtons inputClassName="bg-gray-900 border-gray-800" />
                )} 
              />
            </div>

            <div className="field col-12 md:col-2 mb-4">
              <label htmlFor="affectedPeople" className="block text-gray-400 font-bold mb-2 text-xs uppercase">Affected</label>
              <Controller name="affectedPeople" control={control} 
                render={({ field }) => (
                  <InputNumber id="affectedPeople" value={field.value} onValueChange={(e) => field.onChange(e.value)} min={1} inputClassName="bg-gray-900 border-gray-800" />
                )} 
              />
            </div>

            <div className="col-12 mt-4 flex gap-3">
              <Button type="button" label="Discard" outlined className="p-button-secondary border-gray-700 text-gray-400 w-auto px-5 font-bold" onClick={() => navigate('/')} />
              <Button type="submit" label="Submit to Registry" icon="pi pi-check" className="p-button-primary w-auto px-6 shadow-4 font-bold" />
            </div>
          </form>
        </Card>
      </div>
    </Layout>
  );
}
