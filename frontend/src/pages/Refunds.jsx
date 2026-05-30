import { useEffect, useState } from 'react';
import api, { apiError } from '../api/client.js';
import StatusBadge from '../components/StatusBadge.jsx';

export default function Refunds() {
  const [data, setData] = useState({ content: [] });
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/api/v1/refunds?size=50').then((r) => setData(r.data.data)).catch((e) => setError(apiError(e)));
  }, []);

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Refunds</h1>
      {error && <div className="bg-red-50 text-red-700 text-sm rounded-lg px-3 py-2 mb-4">{error}</div>}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-500 text-left">
            <tr>
              <th className="px-4 py-3 font-medium">Amount</th>
              <th className="px-4 py-3 font-medium">Payment intent</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium">Created</th>
            </tr>
          </thead>
          <tbody>
            {data.content?.length === 0 && (
              <tr><td colSpan="4" className="px-4 py-8 text-center text-gray-400">No refunds yet</td></tr>
            )}
            {data.content?.map((r) => (
              <tr key={r.id} className="border-t border-gray-100">
                <td className="px-4 py-3 font-medium">{Number(r.amount).toFixed(2)}</td>
                <td className="px-4 py-3 text-gray-500 font-mono text-xs">{r.paymentIntentId}</td>
                <td className="px-4 py-3"><StatusBadge status={r.status} /></td>
                <td className="px-4 py-3 text-gray-500">{new Date(r.createdAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
