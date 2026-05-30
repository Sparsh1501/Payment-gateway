import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext.jsx';
import Layout from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Overview from './pages/Overview.jsx';
import Payments from './pages/Payments.jsx';
import PaymentDetail from './pages/PaymentDetail.jsx';
import Refunds from './pages/Refunds.jsx';
import Webhooks from './pages/Webhooks.jsx';
import ApiKeys from './pages/ApiKeys.jsx';

function Protected({ children }) {
  const { merchant, loading } = useAuth();
  if (loading) return <div className="p-10 text-gray-500">Loading…</div>;
  if (!merchant) return <Navigate to="/login" replace />;
  return <Layout>{children}</Layout>;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/" element={<Protected><Overview /></Protected>} />
      <Route path="/payments" element={<Protected><Payments /></Protected>} />
      <Route path="/payments/:id" element={<Protected><PaymentDetail /></Protected>} />
      <Route path="/refunds" element={<Protected><Refunds /></Protected>} />
      <Route path="/webhooks" element={<Protected><Webhooks /></Protected>} />
      <Route path="/api-keys" element={<Protected><ApiKeys /></Protected>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
