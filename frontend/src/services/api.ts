import axios from 'axios';
import { useAuthStore } from '../store/authStore';

const getNormalizedApiUrl = (): string => {
  const envUrl = import.meta.env.VITE_API_URL || 'https://roomwallah-84hk.onrender.com/api/v1';
  const trimmedUrl = envUrl.replace(/\/+$/, '');
  return trimmedUrl.endsWith('/api/v1') ? trimmedUrl : `${trimmedUrl}/api/v1`;
};

export const API_URL = getNormalizedApiUrl();

export const apiClient = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Flag to prevent multiple concurrent token refresh calls
let isRefreshing = false;
let failedQueue: Array<{ resolve: (token: string) => void; reject: (err: any) => void }> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else if (token) {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Check whether current window path is a public route
export const isPublicRoute = (): boolean => {
  try {
    const rawPath = window.location.pathname.toLowerCase();
    const path = rawPath.replace(/\/+$/, '') || '/';
    const exactPublicPaths = [
      '/',
      '/login',
      '/register',
      '/verify-email',
      '/forgot-password',
      '/reset-password',
      '/search',
      '/status',
      '/about',
      '/faq'
    ];
    if (exactPublicPaths.includes(path)) return true;
    if (path.startsWith('/properties') || path.startsWith('/owners') || path.startsWith('/verify-email')) {
      return true;
    }
    return false;
  } catch {
    return true; // fail-safe to public
  }
};

// Request Interceptor: Inject in-memory access token
apiClient.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().accessToken;
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response Interceptor: Catch 401 and rotate refresh tokens
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    // Do not attempt refresh for authentication endpoints or if request was already retried
    const isAuthEndpoint = originalRequest?.url && (
      originalRequest.url.includes('/auth/login') ||
      originalRequest.url.includes('/auth/register') ||
      originalRequest.url.includes('/auth/verify-email') ||
      originalRequest.url.includes('/auth/resend-verification') ||
      originalRequest.url.includes('/auth/refresh') ||
      originalRequest.url.includes('/auth/forgot-password') ||
      originalRequest.url.includes('/auth/reset-password')
    );

    if (error.response?.status !== 401 || originalRequest?._retry || isAuthEndpoint) {
      return Promise.reject(error);
    }

    const token = useAuthStore.getState().accessToken;
    const isMockToken = token === 'tenant_access_token' || token === 'owner_access_token' || token?.startsWith('mock_');
    if (isMockToken) {
      return Promise.reject(error);
    }

    const rawRefreshToken = localStorage.getItem('refreshToken');
    if (!rawRefreshToken) {
      useAuthStore.getState().logout();
      if (!isPublicRoute()) {
        window.location.href = '/login';
      }
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      })
        .then((newToken) => {
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          return apiClient(originalRequest);
        })
        .catch((err) => {
          return Promise.reject(err);
        });
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      // Trigger token rotation refresh
      const response = await axios.post(`${API_URL}/auth/refresh`, {
        refreshToken: rawRefreshToken,
      });

      const { accessToken, refreshToken } = response.data.data;

      // Save new tokens
      useAuthStore.getState().setToken(accessToken);
      localStorage.setItem('refreshToken', refreshToken);

      processQueue(null, accessToken);
      isRefreshing = false;

      // Retry the original request
      originalRequest.headers.Authorization = `Bearer ${accessToken}`;
      return apiClient(originalRequest);
    } catch (refreshError) {
      processQueue(refreshError, null);
      isRefreshing = false;
      useAuthStore.getState().logout();
      if (!isPublicRoute()) {
        window.location.href = '/login';
      }
      return Promise.reject(refreshError);
    }
  }
);
