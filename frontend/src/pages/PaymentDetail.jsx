import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { apiError } from '../api/client.js';
import StatusBadge from '../components/StatusBadge.jsx';

export default function PaymentDetail() {
  const { id } = useParams();
  const [pi, setPi] = useState(null);
  const [error, setError] = useState('');
  const [refundAmount, setRefundAmount] = useState('');
  const [msg, setMsg] = useState('');

  function load() {
    api.get(`/api/v1/payment-intents/${id}`).then((r) => setPi(r.data.data)).catch((e) => setError(apiError(e)));
  }
  useEffect(load, [id]);

  async function refund() {
    setMsg('');
    setError('');
    try {
      await api.post('/api/v1/refunds', {
        paymentIntentId: id,
        amount: Number(refundAmount),
        reason: 'requested_by_customer',
      });
      setMsg('Refund created');
      setRefundAmount('');
      load();
    } catch (e) {
      setError(apiError(e));
    }
  }

  if (error) return <div className="bg-red-50 text-red-700 text-sm rounded-lg px-3 py-2">{error}</div>;
  if (!pi) return <div className="text-gray-500">Loading…</div>;

  const rows = [
    ['ID', pi.id],
    ['Amount', `${Number(pi.amount).toFixed(2)} ${pi.currency}`],
    ['Provider', pi.provider],
    ['Provider payment id', pi.providerPaymentId || '—'],
    ['Idempotency key', pi.idempotencyKey || '—'],
    ['Failure reason', pi.failureReason || '—'],
    ['Created', new Date(pi.createdAt).toLocaleString()],
    ['Updated', new Date(pi.updatedAt).toLocaleString()],
  ];

  return (
    <div>
      <Link to="/payments" className="text-sm text-brand hover:underline">← Back to payments</Link>
      <div className="flex items-center gap-3 mt-3 mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Payment</h1>
        <StatusBadge status={pi.status} />
      </div>

      {msg && <div className="bg-green-50 text-green-700 text-sm rounded-lg px-3 py-2 mb-4">{msg}</div>}

      <div className="bg-white rounded-xl border border-gray-200 divide-y divide-gray-100">
        {rows.map(([k, v]) => (
          <div key={k} className="flex px-5 py-3 text-sm">
            <div className="w-48 text-gray-500">{k}</div>
            <div className="text-gray-900 break-all">{v}</div>
          </div>
        ))}
      </div>

      {pi.status === 'SUCCESS' && (
        <div className="bg-white rounded-xl border border-gray-200 p-5 mt-6 max-w-md">
          <div className="text-sm font-semibold text-gray-700 mb-3">Issue a refund</div>
          <div className="flex gap-2">
            <input type="number" step="0.01" value={refundAmount} placeholder="Amount"
              onChange={(e) => setRefundAmount(e.target.value)}
              className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm" />
            <button onClick={refund}
              className="bg-brand text-white text-sm rounded-lg px-4 py-2 font-medium hover:bg-brand-dark">
              Refund
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
