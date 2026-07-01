import axios from 'axios';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL + '/api',
  headers: { 'Content-Type': 'application/json' },
});

// Attach token
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('cx_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Handle 401
client.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('cx_token');
      localStorage.removeItem('cx_user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default client;
