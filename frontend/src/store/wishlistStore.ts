import { create } from 'zustand';
import { addToWishlist, removeFromWishlist, getWishlist, batchCheckWishlist } from '../services/wishlistService';
import { useAuthStore } from './authStore';

interface WishlistState {
  wishlistedIds: string[];
  isLoaded: boolean;
  isLoading: boolean;
  add: (propertyId: string) => Promise<void>;
  remove: (propertyId: string) => Promise<void>;
  toggle: (propertyId: string) => Promise<void>;
  load: () => Promise<void>;
  batchCheck: (propertyIds: string[]) => Promise<void>;
  clear: () => void;
}

export const useWishlistStore = create<WishlistState>((set, get) => ({
  wishlistedIds: [],
  isLoaded: false,
  isLoading: false,

  add: async (propertyId) => {
    if (!useAuthStore.getState().isAuthenticated) return;
    if (get().wishlistedIds.includes(propertyId)) return;
    
    set((state) => ({ wishlistedIds: [...state.wishlistedIds, propertyId] }));
    try {
      await addToWishlist(propertyId);
    } catch (error) {
      // rollback
      set((state) => ({ wishlistedIds: state.wishlistedIds.filter((id) => id !== propertyId) }));
      throw error;
    }
  },

  remove: async (propertyId) => {
    if (!useAuthStore.getState().isAuthenticated) return;
    if (!get().wishlistedIds.includes(propertyId)) return;

    set((state) => ({ wishlistedIds: state.wishlistedIds.filter((id) => id !== propertyId) }));
    try {
      await removeFromWishlist(propertyId);
    } catch (error) {
      // rollback
      set((state) => ({ wishlistedIds: [...state.wishlistedIds, propertyId] }));
      throw error;
    }
  },

  toggle: async (propertyId) => {
    const isWishlisted = get().wishlistedIds.includes(propertyId);
    if (isWishlisted) {
      await get().remove(propertyId);
    } else {
      await get().add(propertyId);
    }
  },

  load: async () => {
    if (!useAuthStore.getState().isAuthenticated) return;
    if (get().isLoaded || get().isLoading) return;
    set({ isLoading: true });
    try {
      const items = await getWishlist();
      const ids = items.map((item) => item.propertyId);
      set({ wishlistedIds: ids, isLoaded: true });
    } catch (error) {
      console.error('Failed to load wishlist:', error);
      set({ isLoaded: true });
    } finally {
      set({ isLoading: false });
    }
  },

  batchCheck: async (propertyIds) => {
    if (!useAuthStore.getState().isAuthenticated || propertyIds.length === 0) return;
    try {
      const checkedIds = await batchCheckWishlist(propertyIds);
      set((state) => {
        // merge keeping unique items
        const merged = Array.from(new Set([...state.wishlistedIds, ...checkedIds]));
        return { wishlistedIds: merged };
      });
    } catch (error) {
      console.error('Failed to batch check wishlist:', error);
    }
  },

  clear: () => set({ wishlistedIds: [], isLoaded: false, isLoading: false }),
}));

// Clear wishlist when user logs out
useAuthStore.subscribe((state) => {
  if (!state.isAuthenticated) {
    useWishlistStore.getState().clear();
  }
});
