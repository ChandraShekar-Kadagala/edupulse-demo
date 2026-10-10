import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? 'http://localhost:8080/api' : '/api'),
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = 'Bearer ' + token;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    let errorMsg = null;
    if (!navigator.onLine) {
      errorMsg = "You are currently offline. Please check your internet connection.";
    } else if (error.response && error.response.status >= 500) {
      errorMsg = "Server is currently unavailable. Please try again later.";
    } else if (error.code === 'ERR_NETWORK') {
      errorMsg = "Unable to connect to the server. Please check your connection or try again later.";
    }
    
    if (errorMsg) {
      window.dispatchEvent(new CustomEvent('network-error', { detail: errorMsg }));
    }
    return Promise.reject(error);
  }
);

export default api;
