import { Chart } from 'primereact/chart';
import { Signal } from '../types';
import { Card } from 'primereact/card';

type Props = {
  signals: Signal[];
};

export function CategoryChart({ signals }: Props) {
  const data = signals.reduce((acc: any, s) => {
    acc[s.category] = (acc[s.category] || 0) + 1;
    return acc;
  }, {});

  const chartData = {
    labels: Object.keys(data),
    datasets: [
      {
        data: Object.values(data),
        backgroundColor: ['#06b6d4', '#8b5cf6', '#f59e0b', '#10b981', '#3b82f6'],
        hoverBackgroundColor: ['#22d3ee', '#a78bfa', '#fbbf24', '#34d399', '#60a5fa'],
        borderWidth: 0
      }
    ]
  };

  const options = {
    plugins: {
      legend: {
        position: 'bottom',
        labels: { color: '#94a3b8', usePointStyle: true, font: { size: 11 } }
      }
    },
    maintainAspectRatio: false,
    cutout: '70%'
  };

  return (
    <Card className="shadow-4 border-1 border-gray-800 animate-fade-in" header={
      <div className="p-4 pb-0">
        <h2 className="text-lg font-bold text-gray-100">SIGNAL DISTRIBUTION</h2>
        <p className="text-xs text-gray-500 m-0 uppercase tracking-widest">By category volume</p>
      </div>
    }>
      <div className="flex justify-content-center" style={{ height: '200px' }}>
        <Chart type="doughnut" data={chartData} options={options} style={{ width: '100%' }} />
      </div>
    </Card>
  );
}
