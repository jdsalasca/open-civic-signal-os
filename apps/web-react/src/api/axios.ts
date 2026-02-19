import axios from 'axios';
import { useAuthStore } from '../store/useAuthStore';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor: Attach Token
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  // P0-2: Removed sensitive payload logs
  return config;
});

// Response Interceptor: Handle 401/403 and Refresh Token
apiClient.interceptors.response.use(
  (response) => {
    // P0-2: Removed sensitive payload logs
    return response;
  },
  async (error) => {
    // P0-2: Safe redacted error log
    console.error(`[API Error] ${error.response?.status || 'Network Error'} on ${error.config?.url}`);
    
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      const refreshToken = useAuthStore.getState().refreshToken;

      if (refreshToken) {
        try {
          const baseUrl = apiClient.defaults.baseURL || '';
          const res = await axios.post(`${baseUrl}/api/auth/refresh`, { refreshToken });
          const { accessToken } = res.data;
          useAuthStore.getState().updateAccessToken(accessToken);
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return apiClient(originalRequest);
        } catch (refreshError) {
          useAuthStore.getState().logout();
          return Promise.reject(refreshError);
        }
      } else {
        useAuthStore.getState().logout();
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
