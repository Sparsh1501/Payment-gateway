import { useEffect, useState } from 'react';
import {
  CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import api from '../api/client.js';
import StatCard from '../components/StatCard.jsx';

export default function Overview() {
  const [summary, setSummary] = useState(null);
  const [series, setSeries] = useState([]);

  useEffect(() => {
    api.get('/api/v1/analytics/summary').then((r) => setSummary(r.data.data)).catch(() => {});
    api.get('/api/v1/analytics/timeseries?days=7').then((r) => setSeries(r.data.data || [])).catch(() => {});
  }, []);

  const chartData = series.map((p) => ({ date: p.date?.slice(5), revenue: Number(p.revenue) }));

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Overview</h1>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <StatCard label="Total revenue" value={summary ? Number(summary.totalVolume).toLocaleString() : '—'} sub="last 30 days" />
        <StatCard label="Transactions" value={summary ? summary.totalTransactions : '—'} />
        <StatCard label="Success rate" value={summary ? `${summary.successRate}%` : '—'} />
        <StatCard label="Failed" value={summary ? summary.failedCount : '—'} />
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <div className="text-sm font-semibold text-gray-700 mb-4">Revenue · last 7 days</div>
        <div style={{ width: '100%', height: 280 }}>
          <ResponsiveContainer>
            <LineChart data={chartData} margin={{ top: 8, right: 16, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#eee" />
              <XAxis dataKey="date" stroke="#9ca3af" fontSize={12} />
              <YAxis stroke="#9ca3af" fontSize={12} />
              <Tooltip />
              <Line type="monotone" dataKey="revenue" stroke="#5b5bd6" strokeWidth={2} dot={{ r: 3 }} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}
