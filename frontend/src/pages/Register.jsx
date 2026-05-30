import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { apiError } from '../api/client.js';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [businessName, setBusinessName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await register(businessName, email, password);
      navigate('/');
    } catch (err) {
      setError(apiError(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4">
      <form onSubmit={submit} className="bg-white rounded-2xl border border-gray-200 p-8 w-full max-w-sm">
        <h1 className="text-xl font-bold text-brand mb-1">Create your account</h1>
        <p className="text-sm text-gray-500 mb-6">Start accepting payments in minutes</p>
        {error && <div className="bg-red-50 text-red-700 text-sm rounded-lg px-3 py-2 mb-4">{error}</div>}
        <label className="block text-sm text-gray-600 mb-1">Business name</label>
        <input className="w-full border border-gray-300 rounded-lg px-3 py-2 mb-4"
          value={businessName} onChange={(e) => setBusinessName(e.target.value)} required />
        <label className="block text-sm text-gray-600 mb-1">Email</label>
        <input className="w-full border border-gray-300 rounded-lg px-3 py-2 mb-4" type="email"
          value={email} onChange={(e) => setEmail(e.target.value)} required />
        <label className="block text-sm text-gray-600 mb-1">Password</label>
        <input className="w-full border border-gray-300 rounded-lg px-3 py-2 mb-6" type="password"
          value={password} onChange={(e) => setPassword(e.target.value)} minLength={8} required />
        <button disabled={busy} className="w-full bg-brand text-white rounded-lg py-2.5 font-medium hover:bg-brand-dark disabled:opacity-60">
          {busy ? 'Creating…' : 'Create account'}
        </button>
        <p className="text-sm text-gray-500 mt-4 text-center">
          Have an account? <Link to="/login" className="text-brand hover:underline">Sign in</Link>
        </p>
      </form>
    </div>
  );
}
