import React, { useState } from 'react';
import { Heart } from 'lucide-react';
import { useAuthStore } from '../store/authStore';
import { useWishlistStore } from '../store/wishlistStore';
import { useNavigate } from 'react-router-dom';

interface WishlistButtonProps {
  propertyId: string;
  className?: string;
}

export const WishlistButton: React.FC<WishlistButtonProps> = ({ propertyId, className = "" }) => {
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const wishlistedIds = useWishlistStore((state) => state.wishlistedIds);
  const add = useWishlistStore((state) => state.add);
  const remove = useWishlistStore((state) => state.remove);
  
  const isWishlisted = wishlistedIds.includes(propertyId);
  const [loading, setLoading] = useState(false);

  const handleToggle = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();

    if (!isAuthenticated) {
      navigate('/login');
      return;
    }

    setLoading(true);
    try {
      if (isWishlisted) {
        await remove(propertyId);
      } else {
        await add(propertyId);
      }
    } catch (err) {
      console.error('Failed to toggle wishlist:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      onClick={handleToggle}
      disabled={loading}
      className={`p-2 rounded-full glass hover:bg-slate-700/50 transition-all duration-300 flex items-center justify-center ${className}`}
      aria-label={isWishlisted ? "Remove from wishlist" : "Add to wishlist"}
    >
      <Heart
        className={`w-5 h-5 transition-all duration-300 ${
          isWishlisted
            ? "text-red-500 fill-red-500 scale-110"
            : "text-slate-400 hover:text-red-500 hover:scale-110"
        }`}
      />
    </button>
  );
};
