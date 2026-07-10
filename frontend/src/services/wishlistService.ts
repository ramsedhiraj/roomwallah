import { apiClient } from './api';
import { PropertyCard } from './searchService';

export async function addToWishlist(propertyId: string): Promise<void> {
  await apiClient.post(`/wishlist/${propertyId}`);
}

export async function removeFromWishlist(propertyId: string): Promise<void> {
  await apiClient.delete(`/wishlist/${propertyId}`);
}

export async function getWishlist(): Promise<PropertyCard[]> {
  const response = await apiClient.get('/wishlist');
  return response.data.data;
}

export async function batchCheckWishlist(propertyIds: string[]): Promise<string[]> {
  if (propertyIds.length === 0) return [];
  const response = await apiClient.get('/wishlist/check', {
    params: { propertyIds: propertyIds.join(',') },
  });
  return response.data.data;
}
