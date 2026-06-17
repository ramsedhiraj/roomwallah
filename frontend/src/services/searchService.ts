import { apiClient } from './api';

// ─── Search Types ────────────────────────────────────────────

export interface PropertyCard {
  propertyId: string;
  listingRef: string;
  slug: string;
  title: string;
  city: string;
  locality: string;
  price: number;
  propertyType: string;
  listingPurpose: string;
  bedrooms: number | null;
  bathrooms: number | null;
  parkingCount: number;
  furnishingStatus: string | null;
  petFriendly: boolean;
  trustScore: number;
  ownerVerified: boolean;
  ownerBadge: string | null;
  mediaCount: number;
  publishedAt: string | null;
  thumbnailUrl: string | null;
  latitude?: number | null;
  longitude?: number | null;
  rankingExplanation: Record<string, any> | null;
}

export interface SearchResponse {
  results: PropertyCard[];
  nextCursor: string | null;
  totalCount: number;
  executionTimeMs: number;
}

export interface SearchParams {
  q?: string;
  city?: string;
  locality?: string;
  propertyType?: string;
  listingPurpose?: string;
  minPrice?: number;
  maxPrice?: number;
  bedrooms?: number;
  bathrooms?: number;
  petFriendly?: boolean;
  ownerVerified?: boolean;
  latitude?: number;
  longitude?: number;
  radiusKm?: number;
  sortBy?: string;
  sortDir?: string;
  cursor?: string;
  size?: number;
  furnishingStatus?: string;
  parkingCount?: number;
  facingDirection?: string;
  availabilityDate?: string;
  minTrustScore?: number;
  bboxNorthEastLat?: number;
  bboxNorthEastLon?: number;
  bboxSouthWestLat?: number;
  bboxSouthWestLon?: number;
  explain?: boolean;
  experimentalBucket?: string;
}

export interface RecommendationItem {
  property: PropertyCard;
  reasons: string[];
}

export interface SavedSearch {
  id: string;
  serializedQuery: string;
  notificationEnabled: boolean;
  lastTriggeredAt: string | null;
  createdAt: string;
  notificationFrequency?: string;
}

export interface TrendingQueryItem {
  queryText: string;
  city: string;
  searchCount: number;
}

// ─── Search API ──────────────────────────────────────────────

export async function searchProperties(params: SearchParams, signal?: AbortSignal): Promise<SearchResponse> {
  const cleanParams: Record<string, string | number | boolean> = {};
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      cleanParams[key] = value;
    }
  });

  const cacheKey = `search_cache_${JSON.stringify(cleanParams)}`;

  if (typeof window !== 'undefined' && !navigator.onLine) {
    const cached = localStorage.getItem(cacheKey);
    if (cached) {
      console.log('Serving search results from offline local storage cache');
      return JSON.parse(cached);
    }
    throw new Error('You are offline and no cached results are available for this search.');
  }

  const response = await apiClient.get('/search', { params: cleanParams, signal });
  const data = response.data.data;

  if (typeof window !== 'undefined' && data) {
    try {
      localStorage.setItem(cacheKey, JSON.stringify(data));
      const keys = Object.keys(localStorage);
      const searchKeys = keys.filter(k => k.startsWith('search_cache_'));
      if (searchKeys.length > 50) {
        localStorage.removeItem(searchKeys[0]);
      }
    } catch (e) {
      console.warn('Failed to write to localStorage search cache', e);
    }
  }

  return data;
}

export async function getAutoComplete(q: string, city?: string): Promise<string[]> {
  const params: Record<string, string> = { q };
  if (city) params.city = city;
  const response = await apiClient.get('/search/autocomplete', { params });
  return response.data.data?.suggestions ?? [];
}

export async function getTrendingQueries(city?: string): Promise<TrendingQueryItem[]> {
  const params: Record<string, string> = {};
  if (city) params.city = city;
  const response = await apiClient.get('/search/trending', { params });
  return response.data.data ?? [];
}

// ─── Recommendations API ─────────────────────────────────────

export async function getRecommendations(limit: number = 10): Promise<RecommendationItem[]> {
  const response = await apiClient.get('/recommendations', { params: { limit } });
  return response.data.data ?? [];
}

export async function refreshRecommendations(): Promise<void> {
  await apiClient.post('/admin/search/recommendations/refresh');
}

// ─── Saved Search API ────────────────────────────────────────

export async function createSavedSearch(serializedQuery: string, notificationEnabled: boolean, notificationFrequency?: string): Promise<SavedSearch> {
  const response = await apiClient.post('/search/saved', { 
    serializedQuery, 
    notificationEnabled,
    notificationFrequency: notificationFrequency ?? 'INSTANT'
  });
  return response.data.data;
}

export async function getMySavedSearches(): Promise<SavedSearch[]> {
  const response = await apiClient.get('/search/saved');
  return response.data.data ?? [];
}

export async function deleteSavedSearch(id: string): Promise<void> {
  await apiClient.delete(`/search/saved/${id}`);
}

// ─── Maintenance API ─────────────────────────────────────────

export async function triggerMaintenance(): Promise<void> {
  await apiClient.post('/admin/search/maintenance');
}

export const searchService = {
  searchProperties,
  getAutoComplete,
  getTrendingQueries,
  getRecommendations,
  refreshRecommendations,
  createSavedSearch,
  getSavedSearches: getMySavedSearches,
  deleteSavedSearch,
  triggerMaintenance
};

export default searchService;
