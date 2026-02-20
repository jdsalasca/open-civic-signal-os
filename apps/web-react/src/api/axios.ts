import axios from 'axios';
import { useAuthStore } from '../store/useAuthStore';
import { useCommunityStore } from '../store/useCommunityStore';

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

  const activeCommunityId = useCommunityStore.getState().activeCommunityId;
  if (activeCommunityId) {
    config.headers['X-Community-Id'] = activeCommunityId;
  }
  
  // UX-001: Safety - Prevent double prefix if absolute path is passed
  if (config.url?.startsWith('/api')) {
    config.url = config.url.replace('/api', '');
  }
  
  return config;
});

// Response Interceptor: Handle 401/403 and Refresh Token
let isRefreshing = false;
let failedQueue: any[] = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

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

    // P0: Refresh-loop breaker logic
    // 1. Guard against recursive refresh on /auth/refresh or /auth/login
    if (originalRequest.url?.includes('auth/refresh') || originalRequest.url?.includes('auth/login')) {
      if (error.response?.status === 401) {
        useAuthStore.getState().logout();
      }
      return Promise.reject(error);
    }

    // 2. Trigger refresh if 401 and not retried yet
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return apiClient(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const res = await apiClient.post('auth/refresh', {});
        const { accessToken } = res.data;
        
        useAuthStore.getState().updateAccessToken(accessToken);
        processQueue(null, accessToken);
        isRefreshing = false;
        
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        isRefreshing = false;
        useAuthStore.getState().logout();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
