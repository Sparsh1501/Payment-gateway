import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { apiError } from '../api/client.js';
import StatusBadge from '../components/StatusBadge.jsx';

const CURRENCIES = ['USD', 'EUR', 'GBP', 'INR'];
const PROVIDERS = ['STRIPE', 'RAZORPAY'];

export default function Payments() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ content: [], totalPages: 0, number: 0 });
  const [error, setError] = useState('');
  const [creating, setCreating] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [amount, setAmount] = useState('');
  const [currency, setCurrency] = useState('USD');
  const [provider, setProvider] = useState('STRIPE');

  function load(p = page) {
    api.get(`/api/v1/payment-intents?page=${p}&size=10`)
      .then((r) => setData(r.data.data))
      .catch((e) => setError(apiError(e)));
  }

  useEffect(() => { load(page); }, [page]);

  async function createTestPayment(e) {
    e.preventDefault();
    setCreating(true);
    setError('');
    try {
      const { data: created } = await api.post(
        '/api/v1/payment-intents',
        {
          amount: Number(amount),
          currency,
          provider,
          metadata: { source: 'dashboard' },
        },
        { headers: { 'Idempotency-Key': crypto.randomUUID() } }
      );
      await api.post(`/api/v1/payment-intents/${created.data.id}/confirm`);
      setAmount('');
      setCurrency('USD');
      setProvider('STRIPE');
      setShowForm(false);
      load(0);
      setPage(0);
    } catch (err) {
      setError(apiError(err));
    } finally {
      setCreating(false);
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Payments</h1>
        <button
          type="button"
          onClick={() => { setShowForm((v) => !v); setError(''); }}
          className="bg-brand text-white text-sm rounded-lg px-4 py-2 font-medium hover:bg-brand-dark"
        >
          {showForm ? 'Cancel' : '+ New test payment'}
        </button>
      </div>

      {error && <div className="bg-red-50 text-red-700 text-sm rounded-lg px-3 py-2 mb-4">{error}</div>}

      {showForm && (
        <form onSubmit={createTestPayment} className="bg-white rounded-xl border border-gray-200 p-5 mb-6">
          <div className="text-sm font-semibold text-gray-700 mb-3">Payment details</div>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-4">
            <div>
              <label className="block text-sm text-gray-600 mb-1">Amount</label>
              <input
                type="number"
                min="0.50"
                step="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="10.00"
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                required
              />
            </div>
            <div>
              <label className="block text-sm text-gray-600 mb-1">Currency</label>
              <select
                value={currency}
                onChange={(e) => setCurrency(e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm bg-white"
              >
                {CURRENCIES.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm text-gray-600 mb-1">Provider</label>
              <select
                value={provider}
                onChange={(e) => setProvider(e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm bg-white"
              >
                {PROVIDERS.map((p) => (
                  <option key={p} value={p}>{p}</option>
                ))}
              </select>
            </div>
          </div>
          <button
            type="submit"
            disabled={creating}
            className="bg-brand text-white text-sm rounded-lg px-4 py-2 font-medium hover:bg-brand-dark disabled:opacity-60"
          >
            {creating ? 'Processing…' : 'Create & confirm'}
          </button>
        </form>
      )}

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
