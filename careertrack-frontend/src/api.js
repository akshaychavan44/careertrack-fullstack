import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || (import.meta.env.PROD
    ? 'https://careertrack-backend-z3yy.onrender.com/api' : 'http://localhost:8080/api'),
  timeout: 60000
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && !config.url.startsWith('/auth/')) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function errorMessage(error) {
  if (!error.response) return 'Cannot reach the server. It may be starting up; please try again shortly.';
  if (error.response.status === 401 || error.response.status === 403) {
    return error.config?.url?.startsWith('/auth/')
      ? 'Email or password is incorrect.' : 'Your session expired. Please log in again.';
  }
  const data = error.response.data;
  if (typeof data === 'string' && !data.startsWith('<')) return data;
  return data?.message || 'The request failed. Please try again.';
}

export default api;
