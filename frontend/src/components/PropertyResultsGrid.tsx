import { useRef, useState, useEffect } from 'react';
import { MapPin, Bed, Bath, Car, Shield, ShieldCheck, PawPrint, Clock, ImageIcon } from 'lucide-react';
import type { PropertyCard } from '../services/searchService';
import { WishlistButton } from './WishlistButton';

interface Props {
  results: PropertyCard[];
  loading: boolean;
}

function formatPrice(price: number): string {
  if (price >= 10000000) return `₹${(price / 10000000).toFixed(2)} Cr`;
  if (price >= 100000) return `₹${(price / 100000).toFixed(2)} L`;
  return `₹${price.toLocaleString('en-IN')}`;
}

function timeAgo(dateStr: string | null): string {
  if (!dateStr) return '';
  const diff = Date.now() - new Date(dateStr).getTime();
  const days = Math.floor(diff / 86400000);
  if (days === 0) return 'Today';
  if (days === 1) return 'Yesterday';
  if (days < 30) return `${days}d ago`;
  if (days < 365) return `${Math.floor(days / 30)}mo ago`;
  return `${Math.floor(days / 365)}y ago`;
}


export function LazyCardContainer({ children }: { children: React.ReactNode }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true);
          observer.disconnect();
        }
      },
      { rootMargin: '200px' }
    );

    if (containerRef.current) {
      observer.observe(containerRef.current);
    }

    return () => observer.disconnect();
  }, []);

  return (
    <div ref={containerRef} className="min-h-[380px]">
      {isVisible ? children : (
        <div className="glass rounded-2xl h-[380px] overflow-hidden animate-pulse">
          <div className="h-48 bg-slate-800/60" />
          <div className="p-5 space-y-3">
            <div className="h-5 bg-slate-700/50 rounded w-3/4" />
            <div className="h-4 bg-slate-700/40 rounded w-1/2" />
            <div className="h-4 bg-slate-700/30 rounded w-full" />
          </div>
        </div>
      )}
    </div>
  );
}

export default function PropertyResultsGrid({ results, loading }: Props) {
  if (loading) {
    return (
      <div data-testid="property-grid" className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} className="glass rounded-2xl overflow-hidden animate-pulse">
            <div className="h-48 bg-slate-800/60" />
            <div className="p-5 space-y-3">
              <div className="h-5 bg-slate-700/50 rounded w-3/4" />
              <div className="h-4 bg-slate-700/40 rounded w-1/2" />
              <div className="h-4 bg-slate-700/30 rounded w-full" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (results.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center">
        <div className="p-4 rounded-full bg-slate-800/40 border border-slate-700/50 mb-6">
          <MapPin className="w-10 h-10 text-muted-foreground" />
        </div>
        <h3 className="text-xl font-bold text-slate-200 mb-2">No properties found</h3>
        <p className="text-sm text-muted-foreground max-w-md">
          Try adjusting your search filters or expanding the search area to find more listings.
        </p>
      </div>
    );
  }

  return (
    <div data-testid="property-grid" className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
      {results.map((property) => (
        <LazyCardContainer key={property.propertyId}>
          <a
            data-testid="property-card"
            href={`/properties/${property.propertyId}`}
            className="glass glass-hover rounded-2xl overflow-hidden group transition-all duration-300 hover:-translate-y-1 block h-full flex flex-col justify-between"
          >
            {/* Image Thumbnail */}
            <div className="relative h-48 bg-gradient-to-br from-slate-800 to-slate-900 flex items-center justify-center overflow-hidden">
              {property.thumbnailUrl ? (
                <img
                  src={property.thumbnailUrl}
                  alt={property.title}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                  loading="lazy"
                />
              ) : (
                <ImageIcon className="w-12 h-12 text-slate-700" />
              )}
              {/* Badge overlays */}
              <div className="absolute top-3 left-3 flex gap-2">
                {property.ownerVerified && (
                  <span className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-500/20 border border-emerald-500/30 text-emerald-400 text-[10px] font-semibold uppercase tracking-wider">
                    <ShieldCheck className="w-3 h-3" />
                    Verified
                  </span>
                )}
                {property.ownerBadge && (
                  <span className="px-2 py-0.5 rounded-full bg-indigo-500/20 border border-indigo-500/30 text-indigo-400 text-[10px] font-semibold uppercase tracking-wider">
                    {property.ownerBadge}
                  </span>
                )}
              </div>
              <div className="absolute top-3 right-3 flex items-center gap-1.5 z-10">
                <span className="px-2 py-0.5 rounded-full bg-slate-900/70 border border-slate-700/50 text-slate-300 text-[10px] font-medium">
                  {property.listingPurpose === 'RENT' ? 'For Rent' : 'For Sale'}
                </span>
                <WishlistButton propertyId={property.propertyId} className="!p-1" />
              </div>
              {property.mediaCount > 0 && (
                <div className="absolute bottom-3 right-3 flex items-center gap-1 px-2 py-0.5 rounded-full bg-slate-900/70 text-slate-400 text-[10px]">
                  <ImageIcon className="w-3 h-3" />
                  {property.mediaCount}
                </div>
              )}
            </div>

            {/* Content */}
            <div className="p-5 flex-1 flex flex-col justify-between space-y-3">
              <div className="space-y-3">
                {/* Price & Type */}
                <div className="flex items-center justify-between">
                  <span className="text-xl font-bold bg-gradient-to-r from-primary to-secondary bg-clip-text text-transparent">
                    {formatPrice(property.price)}
                    {property.listingPurpose === 'RENT' && <span className="text-xs text-muted-foreground font-normal">/mo</span>}
                  </span>
                  <span className="text-[10px] text-muted-foreground font-medium uppercase tracking-wider px-2 py-0.5 rounded-full bg-slate-800/60 border border-slate-700/40">
                    {property.propertyType?.replace(/_/g, ' ')}
                  </span>
                </div>

                {/* Title */}
                <h3 className="font-semibold text-slate-200 text-sm line-clamp-1 group-hover:text-primary transition-colors">
                  {property.title}
                </h3>

                {/* Location */}
                <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                  <MapPin className="w-3.5 h-3.5 text-primary/70" />
                  <span>{property.locality}, {property.city}</span>
                </div>

                {/* Amenity chips */}
                <div className="flex flex-wrap items-center gap-3 text-xs text-slate-400 pt-1">
                  {property.bedrooms !== null && (
                    <span className="flex items-center gap-1">
                      <Bed className="w-3.5 h-3.5" /> {property.bedrooms} BHK
                    </span>
                  )}
                  {property.bathrooms !== null && (
                    <span className="flex items-center gap-1">
                      <Bath className="w-3.5 h-3.5" /> {property.bathrooms} Bath
                    </span>
                  )}
                  {property.parkingCount > 0 && (
                    <span className="flex items-center gap-1">
                      <Car className="w-3.5 h-3.5" /> {property.parkingCount}
                    </span>
                  )}
                  {property.petFriendly && (
                    <span className="flex items-center gap-1 text-emerald-400">
                      <PawPrint className="w-3.5 h-3.5" /> Pets OK
                    </span>
                  )}
                </div>

                {/* Ranking Explanation */}
                {property.rankingExplanation && (
                  <div className="p-2.5 rounded-xl bg-slate-800/30 border border-slate-700/40 text-[10px] text-slate-400 space-y-1">
                    <div className="font-semibold text-primary">Explainable Ranking Match:</div>
                    {Object.entries(property.rankingExplanation).map(([key, val]) => (
                      <div key={key} className="flex justify-between gap-2">
                        <span className="text-slate-500">{key}:</span>
                        <span className="text-slate-300 text-right">{String(val)}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Footer: Trust Score & Freshness */}
              <div className="flex items-center justify-between pt-2 border-t border-slate-800/50 text-[10px] text-muted-foreground">
                <div className="flex items-center gap-1.5">
                  <Shield className="w-3.5 h-3.5 text-primary/60" />
                  <span>Trust: {property.trustScore}/100</span>
                </div>
                <div className="flex items-center gap-1">
                  <Clock className="w-3 h-3" />
                  <span>{timeAgo(property.publishedAt)}</span>
                </div>
              </div>
            </div>
          </a>
        </LazyCardContainer>
      ))}
    </div>
  );
}
