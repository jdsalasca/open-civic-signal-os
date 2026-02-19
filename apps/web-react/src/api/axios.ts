import axios from 'axios';
import { useAuthStore } from '../store/useAuthStore';

// UX-001: Hardened baseURL. We avoid trailing slash to keep composition predictable.
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
  
  // UX-001: Safety check - ensure no double /api in outgoing URL
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

    // UX-005: Root-cause analysis for error messaging
    if (!error.response) {
      error.friendlyMessage = "Civic API Unreachable: Please verify your connection or check service status.";
    } else {
      switch (error.response.status) {
        case 401: error.friendlyMessage = "Authentication Required: Your session is invalid or expired."; break;
        case 403: error.friendlyMessage = "Forbidden: Your role lacks clearance for this coordinate."; break;
        case 404: error.friendlyMessage = "Not Found: The requested civic resource does not exist."; break;
        case 409: error.friendlyMessage = "Conflict: Action rejected to maintain data integrity."; break;
        default: error.friendlyMessage = "Civic Error: An unexpected system rejection occurred.";
      }
    }

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // UX-001: Explicit relative path for refresh
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
