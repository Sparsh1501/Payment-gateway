import { useEffect, useState } from 'react';
import api, { apiError } from '../api/client.js';

function CopyField({ label, value }) {
  const [copied, setCopied] = useState(false);
  async function copy() {
    await navigator.clipboard.writeText(value || '');
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  }
  return (
    <div className="mb-4">
      <div className="text-sm text-gray-500 mb-1">{label}</div>
      <div className="flex gap-2">
        <input readOnly value={value || ''}
          className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm font-mono bg-gray-50" />
        <button onClick={copy}
          className="border border-gray-300 rounded-lg px-3 py-2 text-sm hover:bg-gray-50 w-20">
          {copied ? 'Copied' : 'Copy'}
        </button>
      </div>
    </div>
  );
}

export default function ApiKeys() {
  const [keys, setKeys] = useState(null);
  const [error, setError] = useState('');

  function load() {
    api.get('/api/v1/api-keys').then((r) => setKeys(r.data.data)).catch((e) => setError(apiError(e)));
  }
  useEffect(load, []);

  async function regenerate() {
    if (!confirm('Regenerate API credentials? Existing keys will stop working.')) return;
    try {
      const { data } = await api.post('/api/v1/api-keys/regenerate');
      setKeys(data.data);
    } catch (e) {
      setError(apiError(e));
    }
  }

  return (
    <div className="max-w-xl">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">API Keys</h1>
      {error && <div className="bg-red-50 text-red-700 text-sm rounded-lg px-3 py-2 mb-4">{error}</div>}
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <CopyField label="API Key" value={keys?.apiKey} />
        <CopyField label="API Secret" value={keys?.apiSecret} />
        <p className="text-xs text-gray-400 mb-4">
          Use these with the <code>X-API-Key</code> and <code>X-API-Secret</code> headers for server-to-server calls.
        </p>
        <button onClick={regenerate}
          className="bg-red-600 text-white text-sm rounded-lg px-4 py-2 font-medium hover:bg-red-700">
          Regenerate credentials
        </button>
      </div>
    </div>
  );
}
