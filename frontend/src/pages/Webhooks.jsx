import { useEffect, useState } from 'react';
import api, { apiError } from '../api/client.js';
import StatusBadge from '../components/StatusBadge.jsx';

const EVENT_OPTIONS = [
  'payment.success', 'payment.failed', 'payment.created',
  'refund.created', 'refund.success', 'refund.failed',
];

export default function Webhooks() {
  const [endpoints, setEndpoints] = useState([]);
  const [deliveries, setDeliveries] = useState([]);
  const [url, setUrl] = useState('');
  const [events, setEvents] = useState(['payment.success']);
  const [error, setError] = useState('');

  function load() {
    api.get('/api/v1/webhooks/endpoints').then((r) => setEndpoints(r.data.data || [])).catch(() => {});
    api.get('/api/v1/webhooks/deliveries?size=20').then((r) => setDeliveries(r.data.data?.content || [])).catch(() => {});
  }
  useEffect(load, []);

  function toggleEvent(ev) {
    setEvents((cur) => (cur.includes(ev) ? cur.filter((e) => e !== ev) : [...cur, ev]));
  }

  async function addEndpoint(e) {
    e.preventDefault();
    setError('');
    try {
      await api.post('/api/v1/webhooks/endpoints', { url, events });
      setUrl('');
      load();
    } catch (err) {
      setError(apiError(err));
    }
  }

  async function remove(id) {
    await api.delete(`/api/v1/webhooks/endpoints/${id}`);
    load();
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Webhooks</h1>
      {error && <div className="bg-red-50 text-red-700 text-sm rounded-lg px-3 py-2 mb-4">{error}</div>}

      <form onSubmit={addEndpoint} className="bg-white rounded-xl border border-gray-200 p-5 mb-6">
        <div className="text-sm font-semibold text-gray-700 mb-3">Add endpoint</div>
        <input value={url} onChange={(e) => setUrl(e.target.value)} placeholder="https://example.com/webhook"
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm mb-3" required />
        <div className="flex flex-wrap gap-3 mb-4">
          {EVENT_OPTIONS.map((ev) => (
            <label key={ev} className="flex items-center gap-1.5 text-sm text-gray-600">
              <input type="checkbox" checked={events.includes(ev)} onChange={() => toggleEvent(ev)} />
              {ev}
            </label>
          ))}
        </div>
        <button className="bg-brand text-white text-sm rounded-lg px-4 py-2 font-medium hover:bg-brand-dark">
          Add endpoint
        </button>
      </form>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden mb-6">
        <div className="px-5 py-3 text-sm font-semibold text-gray-700 border-b border-gray-100">Endpoints</div>
        {endpoints.length === 0 && <div className="px-5 py-6 text-center text-gray-400 text-sm">No endpoints</div>}
        {endpoints.map((e) => (
          <div key={e.id} className="px-5 py-3 border-t border-gray-100 flex items-center justify-between">
            <div>
              <div className="text-sm text-gray-900">{e.url}</div>
              <div className="text-xs text-gray-400">{e.events.join(', ')}</div>
              <div className="text-xs text-gray-400 font-mono">{e.secret}</div>
            </div>
            <button onClick={() => remove(e.id)} className="text-xs text-red-600 hover:underline">Delete</button>
          </div>
        ))}
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="px-5 py-3 text-sm font-semibold text-gray-700 border-b border-gray-100">Delivery logs</div>
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-500 text-left">
            <tr>
              <th className="px-4 py-2 font-medium">Event</th>
              <th className="px-4 py-2 font-medium">Status</th>
              <th className="px-4 py-2 font-medium">Attempts</th>
              <th className="px-4 py-2 font-medium">Response</th>
              <th className="px-4 py-2 font-medium">Created</th>
            </tr>
          </thead>
          <tbody>
            {deliveries.length === 0 && (
              <tr><td colSpan="5" className="px-4 py-6 text-center text-gray-400">No deliveries</td></tr>
            )}
            {deliveries.map((d) => (
              <tr key={d.id} className="border-t border-gray-100">
                <td className="px-4 py-2">{d.eventType}</td>
                <td className="px-4 py-2"><StatusBadge status={d.status} /></td>
                <td className="px-4 py-2">{d.attempts}</td>
                <td className="px-4 py-2 text-gray-500">{d.lastResponseCode ?? '—'}</td>
                <td className="px-4 py-2 text-gray-500">{new Date(d.createdAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
