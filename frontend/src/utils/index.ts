import { API_URL } from '../services/api';

/**
 * Resolves a media URL from backend into a full accessible URL.
 * Automatically prepends the centralized backend host for relative paths.
 */
export function getFullMediaUrl(url: string | null | undefined): string {
  if (!url) {
    return 'https://images.unsplash.com/photo-1560518883-ce09059eeffa?auto=format&fit=crop&w=800&q=80';
  }
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  // Trim trailing /api/v1 if the URL already has /api/v1
  const base = API_URL.endsWith('/api/v1') ? API_URL.slice(0, -7) : API_URL;
  const cleanUrl = url.startsWith('/') ? url : `/${url}`;
  return `${base}${cleanUrl}`;
}

export const FALLBACK_PROPERTY_IMAGE = 
  'https://images.unsplash.com/photo-1560518883-ce09059eeffa?auto=format&fit=crop&w=800&q=80';

/**
 * Formats price in Indian Lakhs and Crores or regular thousands.
 */
export function formatIndianPrice(price: number | string | null | undefined): string {
  if (price === null || price === undefined) return '₹0';
  const num = typeof price === 'string' ? parseFloat(price) : price;
  if (isNaN(num)) return '₹0';
  if (num >= 10000000) {
    return `₹${(num / 10000000).toFixed(2)} Cr`;
  }
  if (num >= 100000) {
    return `₹${(num / 100000).toFixed(2)} L`;
  }
  return `₹${num.toLocaleString('en-IN')}`;
}

