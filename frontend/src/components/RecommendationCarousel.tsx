import { useState, useEffect } from 'react';
import { ChevronLeft, ChevronRight, ShieldCheck, Sparkles, MapPin } from 'lucide-react';
import { searchService } from '../services/searchService';
import type { RecommendationItem } from '../services/searchService';

interface Props {
  userId?: string;
}

function formatPrice(price: number): string {
  if (price >= 10000000) return `₹${(price / 10000000).toFixed(2)} Cr`;
  if (price >= 100000) return `₹${(price / 100000).toFixed(2)} L`;
  return `₹${price.toLocaleString('en-IN')}`;
}

export default function RecommendationCarousel({ userId }: Props) {
  const [recommendations, setRecommendations] = useState<RecommendationItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    async function fetchRecommendations() {
      try {
        setLoading(true);
        const data = await searchService.getRecommendations(10);
        setRecommendations(data);
      } catch (err) {
        console.error('Failed to load recommendations', err);
      } finally {
        setLoading(false);
      }
    }
    fetchRecommendations();
  }, [userId]);

  const scroll = (direction: 'left' | 'right') => {
    const container = document.getElementById('recommendations-container');
    if (container) {
      const scrollAmount = 350;
      container.scrollBy({
        left: direction === 'left' ? -scrollAmount : scrollAmount,
        behavior: 'smooth',
      });
    }
  };

  if (loading) {
    return (
      <div className="space-y-4 py-6">
        <div className="flex items-center justify-between">
          <div className="h-6 bg-slate-800 rounded w-1/4 animate-pulse" />
          <div className="flex gap-2">
            <div className="w-8 h-8 rounded-full bg-slate-800 animate-pulse" />
            <div className="w-8 h-8 rounded-full bg-slate-800 animate-pulse" />
          </div>
        </div>
        <div className="flex gap-6 overflow-hidden">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="w-[300px] flex-shrink-0 glass rounded-2xl overflow-hidden animate-pulse">
              <div className="h-40 bg-slate-800/60" />
              <div className="p-4 space-y-3">
                <div className="h-4 bg-slate-700/50 rounded w-3/4" />
                <div className="h-3 bg-slate-700/40 rounded w-1/2" />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (recommendations.length === 0) {
    return null;
  }

  return (
    <div className="relative space-y-4 py-6 border-b border-slate-800/60">
      <div className="flex items-center justify-between px-1">
        <div className="flex items-center gap-2">
          <Sparkles className="w-5 h-5 text-primary" />
          <h2 className="text-lg font-bold text-slate-200">Recommended for You</h2>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => scroll('left')}
            className="p-1.5 rounded-full bg-slate-800/70 border border-slate-700/50 text-slate-400 hover:text-slate-200 transition-colors"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
          <button
            onClick={() => scroll('right')}
            className="p-1.5 rounded-full bg-slate-800/70 border border-slate-700/50 text-slate-400 hover:text-slate-200 transition-colors"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>

      <div
        id="recommendations-container"
        className="flex gap-6 overflow-x-auto no-scrollbar scroll-smooth pb-3 px-1"
      >
        {recommendations.map(({ property, reasons }) => (
          <a
            key={property.propertyId}
            href={`/properties/${property.propertyId}`}
            className="w-[300px] flex-shrink-0 glass glass-hover rounded-2xl overflow-hidden block transition-all duration-300 hover:-translate-y-1"
          >
            {/* Header image fallback */}
            <div className="relative h-40 bg-gradient-to-br from-slate-800 to-slate-900 flex items-center justify-center">
              <span className="text-xs text-slate-600 font-semibold">Listing Image</span>
              
              {/* Overlapping recommendation reason badges */}
              <div className="absolute bottom-2 left-2 flex flex-wrap gap-1">
                {reasons.slice(0, 2).map((reason, idx) => (
                  <span
                    key={idx}
                    className="px-2 py-0.5 rounded-full bg-primary/20 border border-primary/30 text-primary text-[9px] font-semibold uppercase tracking-wider"
                  >
                    {reason}
                  </span>
                ))}
              </div>

              {property.ownerVerified && (
                <div className="absolute top-2 right-2 flex items-center gap-0.5 px-2 py-0.5 rounded-full bg-emerald-500/20 border border-emerald-500/30 text-emerald-400 text-[9px] font-semibold uppercase tracking-wider">
                  <ShieldCheck className="w-2.5 h-2.5" />
                  Verified
                </div>
              )}
            </div>

            {/* Content info */}
            <div className="p-4 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-md font-bold text-slate-200">
                  {formatPrice(property.price)}
                  {property.listingPurpose === 'RENT' && <span className="text-[10px] text-muted-foreground font-normal">/mo</span>}
                </span>
                <span className="text-[9px] text-muted-foreground uppercase font-medium bg-slate-800 px-2 py-0.5 rounded border border-slate-700/50">
                  {property.propertyType?.replace(/_/g, ' ')}
                </span>
              </div>

              <h3 className="text-xs font-semibold text-slate-300 line-clamp-1">
                {property.title}
              </h3>

              <div className="flex items-center gap-1 text-[10px] text-muted-foreground">
                <MapPin className="w-3 h-3 text-primary/60" />
                <span>{property.locality}, {property.city}</span>
              </div>
            </div>
          </a>
        ))}
      </div>
    </div>
  );
}
