import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL || '';

export const tokenStore = {
  get access() {
    return localStorage.getItem('pg_access');
  },
  get refresh() {
    return localStorage.getItem('pg_refresh');
  },
  set({ accessToken, refreshToken }) {
    if (accessToken) localStorage.setItem('pg_access', accessToken);
    if (refreshToken) localStorage.setItem('pg_refresh', refreshToken);
  },
  clear() {
    localStorage.removeItem('pg_access');
    localStorage.removeItem('pg_refresh');
  },
};

const api = axios.create({ baseURL });

api.interceptors.request.use((config) => {
  const token = tokenStore.access;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshing = null;

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;

    if (status === 401 && !original._retry && tokenStore.refresh) {
      original._retry = true;
      try {
        refreshing =
          refreshing ||
          axios.post(`${baseURL}/api/v1/auth/refresh`, {
            refreshToken: tokenStore.refresh,
          });
        const { data } = await refreshing;
        refreshing = null;
        const tokens = data.data;
        tokenStore.set({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken });
        original.headers.Authorization = `Bearer ${tokens.accessToken}`;
        return api(original);
      } catch (e) {
        refreshing = null;
        tokenStore.clear();
        window.location.href = '/login';
        return Promise.reject(e);
      }
    }
    return Promise.reject(error);
  }
);

export function apiError(error) {
  return error.response?.data?.error?.message || error.message || 'Something went wrong';
}

export default api;
