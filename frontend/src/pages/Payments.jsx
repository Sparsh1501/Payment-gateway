import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { apiError } from '../api/client.js';
import StatusBadge from '../components/StatusBadge.jsx';

export default function Payments() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ content: [], totalPages: 0, number: 0 });
  const [error, setError] = useState('');
  const [creating, setCreating] = useState(false);

  function load(p = page) {
    api.get(`/api/v1/payment-intents?page=${p}&size=10`)
      .then((r) => setData(r.data.data))
      .catch((e) => setError(apiError(e)));
  }

  useEffect(() => { load(page); }, [page]);

  async function createTestPayment() {
    setCreating(true);
    setError('');
    try {
      const amount = (Math.floor(Math.random() * 9000) + 100) / 100 + 10;
      const { data: created } = await api.post(
        '/api/v1/payment-intents',
        { amount, currency: 'USD', provider: 'STRIPE', metadata: { source: 'dashboard' } },
        { headers: { 'Idempotency-Key': crypto.randomUUID() } }
      );
      await api.post(`/api/v1/payment-intents/${created.data.id}/confirm`);
      load(0);
      setPage(0);
    } catch (e) {
      setError(apiError(e));
    } finally {
      setCreating(false);
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Payments</h1>
        <button onClick={createTestPayment} disabled={creating}
          className="bg-brand text-white text-sm rounded-lg px-4 py-2 font-medium hover:bg-brand-dark disabled:opacity-60">
          {creating ? 'Processing…' : '+ New test payment'}
        </button>
      </div>

      {error && <div className="bg-red-50 text-red-700 text-sm rounded-lg px-3 py-2 mb-4">{error}</div>}

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-500 text-left">
            <tr>
              <th className="px-4 py-3 font-medium">Amount</th>
              <th className="px-4 py-3 font-medium">Provider</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium">Created</th>
            </tr>
          </thead>
          <tbody>
            {data.content?.length === 0 && (
              <tr><td colSpan="4" className="px-4 py-8 text-center text-gray-400">No payments yet</td></tr>
            )}
            {data.content?.map((p) => (
              <tr key={p.id} onClick={() => navigate(`/payments/${p.id}`)}
                className="border-t border-gray-100 hover:bg-gray-50 cursor-pointer">
                <td className="px-4 py-3 font-medium">{Number(p.amount).toFixed(2)} {p.currency}</td>
                <td className="px-4 py-3">{p.provider}</td>
                <td className="px-4 py-3"><StatusBadge status={p.status} /></td>
                <td className="px-4 py-3 text-gray-500">{new Date(p.createdAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex items-center justify-between mt-4 text-sm text-gray-500">
        <span>Page {data.number + 1} of {Math.max(data.totalPages, 1)}</span>
        <div className="space-x-2">
          <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}
            className="px-3 py-1.5 border border-gray-300 rounded-lg disabled:opacity-40">Prev</button>
          <button disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}
            className="px-3 py-1.5 border border-gray-300 rounded-lg disabled:opacity-40">Next</button>
        </div>
      </div>
    </div>
  );
}
