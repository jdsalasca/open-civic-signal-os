import axios from 'axios';
import { useAuthStore } from '../store/useAuthStore';

// UX-001: Centralized baseURL
const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

// Request Interceptor: Attach Token
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  
  // UX-001: Safety - Prevent double prefix if absolute path is passed
  if (config.url?.startsWith('/api')) {
    config.url = config.url.replace('/api', '');
  }
  
  return config;
});

// Response Interceptor: Handle 401/403 and Refresh Token
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    // UX-005: Map specific error messages by root cause
    if (!error.response) {
      error.friendlyMessage = "Civic API Unreachable: Check connection.";
    } else {
      // Prioritize backend message if available
      const backendMessage = error.response.data?.message;
      
      if (backendMessage) {
        error.friendlyMessage = backendMessage;
      } else {
        switch (error.response.status) {
          case 401: error.friendlyMessage = "Authentication Required."; break;
          case 403: error.friendlyMessage = "Clearance Denied."; break;
          case 404: error.friendlyMessage = "Resource not found."; break;
          case 409: error.friendlyMessage = "State Conflict detected."; break;
          case 500: error.friendlyMessage = "Internal System Synchronization Failure."; break;
        }
      }
    }

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const res = await apiClient.post('auth/refresh', {});
        const { accessToken } = res.data;
        
        useAuthStore.getState().updateAccessToken(accessToken);
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        
        return apiClient(originalRequest);
      } catch (refreshError) {
        useAuthStore.getState().logout();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
