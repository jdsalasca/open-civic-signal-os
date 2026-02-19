import { useForm, Controller } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
import { Dropdown } from "primereact/dropdown";
import { Slider } from "primereact/slider";
import { Button } from "primereact/button";
import { Card } from "primereact/card";
import { classNames } from "primereact/utils";
import { Layout } from "../components/Layout";
import apiClient from "../api/axios";

interface ApiError extends Error {
  friendlyMessage?: string;
}

type ReportForm = {
  title: string;
  description: string;
  category: string;
  urgency: number;
  impact: number;
  affectedPeople: number;
};

const CATEGORIES = [
  { label: 'Public Safety', value: 'safety' },
  { label: 'Infrastructure', value: 'infrastructure' },
  { label: 'Environment', value: 'environment' },
  { label: 'Social Services', value: 'social' },
  { label: 'Mobility', value: 'mobility' },
  { label: 'Education', value: 'education' },
];

export function ReportSignal() {
  const navigate = useNavigate();
  const { control, handleSubmit, formState: { errors, isSubmitting } } = useForm<ReportForm>({
    defaultValues: { title: '', description: '', category: '', urgency: 3, impact: 3, affectedPeople: 10 }
  });

  const onSubmit = async (data: ReportForm) => {
    try {
      await apiClient.post("signals", data);
      toast.success("Civic signal ingested. Prioritizing...");
      navigate("/");
    } catch (err) {
      const apiErr = err as ApiError;
      toast.error(apiErr.friendlyMessage || "Intelligence ingestion failed.");
    }
  };

  return (
    <Layout>
      <div className="flex justify-content-center mt-4 pb-6">
        <Card title="Report Community Signal" style={{ width: '100%', maxWidth: '700px' }} className="shadow-8 border-round-2xl">
          <p className="text-gray-500 mb-5">Your input will be processed by our priority algorithm to optimize civic response.</p>
          
          <form onSubmit={handleSubmit(onSubmit)} className="p-fluid grid">
            <div className="field col-12">
              <label htmlFor="title" className="font-bold block mb-2">Issue Title</label>
              <Controller name="title" control={control} rules={{ required: 'Short title is required' }} 
                render={({ field, fieldState }) => (
                  <InputText id="title" {...field} className={classNames({ 'p-invalid': fieldState.error })} placeholder="e.g. Broken water pipe in Main St." />
                )} 
              />
              {errors.title && <small className="p-error">{errors.title.message}</small>}
            </div>

            <div className="field col-12 md:col-6">
              <label htmlFor="category" className="font-bold block mb-2">Category</label>
              <Controller name="category" control={control} rules={{ required: 'Category is required' }} 
                render={({ field, fieldState }) => (
                  <Dropdown id="category" {...field} options={CATEGORIES} className={classNames({ 'p-invalid': fieldState.error })} placeholder="Select Sector" />
                )} 
              />
            </div>

            <div className="field col-12 md:col-6">
              <label htmlFor="affectedPeople" className="font-bold block mb-2">Scale (Estimated Citizens)</label>
              <Controller name="affectedPeople" control={control} 
                render={({ field }) => (
                  <div className="flex align-items-center gap-3 bg-gray-900 p-2 border-round border-1 border-white-alpha-10">
                    <Slider {...field} min={1} max={1000} className="flex-grow-1" />
                    <span className="font-black text-cyan-400" style={{ minWidth: '40px' }}>{field.value}</span>
                  </div>
                )} 
              />
            </div>

            <div className="field col-12">
              <label htmlFor="description" className="font-bold block mb-2">Intelligence Context (Description)</label>
              <Controller name="description" control={control} rules={{ required: 'Full context is helpful' }} 
                render={({ field, fieldState }) => (
                  <InputTextarea id="description" {...field} rows={4} className={classNames({ 'p-invalid': fieldState.error })} placeholder="Describe the problem, location and observed impact..." />
                )} 
              />
            </div>

            <div className="field col-12 md:col-6">
              <div className="flex justify-content-between mb-2">
                <label className="font-bold">Urgency Factor</label>
                <span className="text-xs text-gray-500 uppercase font-black">Score: 1-5</span>
              </div>
              <Controller name="urgency" control={control} render={({ field }) => (
                <div className="p-3 bg-black-alpha-20 border-round">
                  <Slider {...field} min={1} max={5} step={1} />
                  <div className="flex justify-content-between mt-2 text-xs font-bold text-gray-600">
                    <span>LOW</span>
                    <span className="text-cyan-500">{field.value}</span>
                    <span>CRITICAL</span>
                  </div>
                </div>
              )} />
            </div>

            <div className="field col-12 md:col-6">
              <div className="flex justify-content-between mb-2">
                <label className="font-bold">Social Impact</label>
                <span className="text-xs text-gray-500 uppercase font-black">Score: 1-5</span>
              </div>
              <Controller name="impact" control={control} render={({ field }) => (
                <div className="p-3 bg-black-alpha-20 border-round">
                  <Slider {...field} min={1} max={5} step={1} />
                  <div className="flex justify-content-between mt-2 text-xs font-bold text-gray-600">
                    <span>MINOR</span>
                    <span className="text-purple-500">{field.value}</span>
                    <span>SYSTEMIC</span>
                  </div>
                </div>
              )} />
            </div>

            <div className="col-12 mt-4 flex gap-3">
              <Button type="button" label="Discard" outlined className="p-button-secondary border-gray-700" onClick={() => navigate("/")} />
              <Button type="submit" label="Ingest Signal" icon="pi pi-bolt" loading={isSubmitting} className="p-button-primary" />
            </div>
          </form>
        </Card>
      </div>
    </Layout>
  );
}
