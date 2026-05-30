import { NavLink } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';

const links = [
  { to: '/', label: 'Overview', end: true },
  { to: '/payments', label: 'Payments' },
  { to: '/refunds', label: 'Refunds' },
  { to: '/webhooks', label: 'Webhooks' },
  { to: '/api-keys', label: 'API Keys' },
];

export default function Layout({ children }) {
  const { merchant, logout } = useAuth();

  return (
    <div className="min-h-full flex">
      <aside className="w-60 bg-white border-r border-gray-200 flex flex-col">
        <div className="px-6 py-5 text-lg font-bold text-brand">PayGateway</div>
        <nav className="flex-1 px-3 space-y-1">
          {links.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              end={l.end}
              className={({ isActive }) =>
                `block px-3 py-2 rounded-lg text-sm font-medium ${
                  isActive ? 'bg-brand text-white' : 'text-gray-600 hover:bg-gray-100'
                }`
              }
            >
              {l.label}
            </NavLink>
          ))}
        </nav>
        <div className="p-4 border-t border-gray-200">
          <div className="text-sm font-medium text-gray-800 truncate">{merchant?.businessName}</div>
          <div className="text-xs text-gray-500 truncate mb-2">{merchant?.email}</div>
          <button onClick={logout} className="text-xs text-brand hover:underline">
            Sign out
          </button>
        </div>
      </aside>
      <main className="flex-1 overflow-auto">
        <div className="max-w-6xl mx-auto px-8 py-8">{children}</div>
      </main>
    </div>
  );
}
