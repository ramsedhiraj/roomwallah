import React, { useState, useEffect } from 'react';
import { Heart, ArrowLeft } from 'lucide-react';
import { Link } from 'react-router-dom';
import { getWishlist } from '../services/wishlistService';
import { PropertyCard } from '../services/searchService';
import PropertyResultsGrid from '../components/PropertyResultsGrid';
import { useWishlistStore } from '../store/wishlistStore';

export default function WishlistPage() {
  const [properties, setProperties] = useState<PropertyCard[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const wishlistedIds = useWishlistStore((state) => state.wishlistedIds);

  useEffect(() => {
    const fetchWishlist = async () => {
      try {
        setLoading(true);
        const data = await getWishlist();
        setProperties(data);
        setError(null);
      } catch (err: any) {
        console.error('Failed to load wishlist:', err);
        setError('Failed to load your wishlist. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    fetchWishlist();
  }, [wishlistedIds]);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-24 pb-16 min-h-screen">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div className="space-y-1">
          <Link to="/search" className="flex items-center gap-1.5 text-xs text-muted-foreground hover:text-white transition-colors mb-2">
            <ArrowLeft className="w-3.5 h-3.5" />
            Back to search
          </Link>
          <div className="flex items-center gap-2">
            <div className="p-1.5 bg-red-500/10 border border-red-500/20 rounded-lg">
              <Heart className="w-5 h-5 text-red-500 fill-red-500/20" />
            </div>
            <h1 className="text-2xl font-bold text-slate-200">My Wishlist</h1>
          </div>
          <p className="text-xs text-muted-foreground">Properties you have saved for later</p>
        </div>
      </div>

      {/* Error state */}
      {error && (
        <div className="glass border-red-900/30 bg-rose-950/10 p-6 rounded-2xl text-center max-w-md mx-auto my-12">
          <p className="text-red-400 font-semibold mb-2">Error</p>
          <p className="text-slate-300 text-sm">{error}</p>
        </div>
      )}

      {/* Grid of Results */}
      {!error && (
        <PropertyResultsGrid results={properties} loading={loading} />
      )}

      {/* Empty State */}
      {!loading && !error && properties.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 text-center glass rounded-3xl max-w-xl mx-auto">
          <div className="p-4 rounded-full bg-slate-800/40 border border-slate-700/50 mb-6">
            <Heart className="w-10 h-10 text-muted-foreground" />
          </div>
          <h3 className="text-xl font-bold text-slate-200 mb-2">Your wishlist is empty</h3>
          <p className="text-sm text-muted-foreground max-w-md mb-6">
            Explore listings on RoomWallah and save them here by clicking the heart icon.
          </p>
          <Link
            to="/search"
            className="px-5 py-2.5 rounded-lg bg-gradient-to-r from-primary to-secondary text-white hover:opacity-95 transition-all text-sm font-semibold"
          >
            Find Properties
          </Link>
        </div>
      )}
    </div>
  );
}
