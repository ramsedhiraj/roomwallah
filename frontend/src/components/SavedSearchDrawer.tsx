import { useState, useEffect } from 'react';
import { X, Search, Bell, BellOff, Trash2, Bookmark } from 'lucide-react';
import { searchService } from '../services/searchService';
import type { SavedSearch } from '../services/searchService';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSelectSearch: (serializedQuery: string) => void;
}

export default function SavedSearchDrawer({ isOpen, onClose, onSelectSearch }: Props) {
  const [savedSearches, setSavedSearches] = useState<SavedSearch[]>([]);
  const [loading, setLoading] = useState<boolean>(false);

  const fetchSavedSearches = async () => {
    try {
      setLoading(true);
      const data = await searchService.getSavedSearches();
      setSavedSearches(data);
    } catch (err) {
      console.error('Failed to load saved searches', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchSavedSearches();
    }
  }, [isOpen]);

  const handleDelete = async (id: string) => {
    try {
      await searchService.deleteSavedSearch(id);
      setSavedSearches((prev) => prev.filter((s) => s.id !== id));
    } catch (err) {
      console.error('Failed to delete saved search', err);
    }
  };

  const parseQuerySummary = (serialized: string): string => {
    try {
      const parsed = JSON.parse(serialized);
      const parts: string[] = [];
      if (parsed.text) parts.push(`"${parsed.text}"`);
      if (parsed.filter?.city) parts.push(`in ${parsed.filter.city}`);
      if (parsed.filter?.propertyType) parts.push(parsed.filter.propertyType.replace(/_/g, ' '));
      if (parsed.filter?.bedrooms) parts.push(`${parsed.filter.bedrooms} BHK`);
      if (parsed.filter?.priceRange?.minPrice || parsed.filter?.priceRange?.maxPrice) {
        const min = parsed.filter.priceRange.minPrice || 0;
        const max = parsed.filter.priceRange.maxPrice ? `₹${parsed.filter.priceRange.maxPrice.toLocaleString()}` : 'Max';
        parts.push(`₹${min.toLocaleString()} - ${max}`);
      }
      return parts.join(', ') || 'All Listings';
    } catch (e) {
      return 'Custom Search';
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-slate-950/60 backdrop-blur-sm" onClick={onClose} />

      {/* Drawer */}
      <div className="relative w-full max-w-md h-full bg-slate-900 border-l border-slate-800/80 shadow-2xl flex flex-col z-10 animate-slide-in">
        {/* Header */}
        <div className="p-6 border-b border-slate-800/80 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Bookmark className="w-5 h-5 text-primary" />
            <h2 className="text-xl font-bold text-slate-200">Saved Searches</h2>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800/50">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-4 no-scrollbar">
          {loading ? (
            <div className="space-y-4">
              {Array.from({ length: 3 }).map((_, i) => (
                <div key={i} className="h-24 bg-slate-800/50 rounded-xl animate-pulse" />
              ))}
            </div>
          ) : savedSearches.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-20 text-center text-muted-foreground">
              <Search className="w-10 h-10 mb-4 opacity-55" />
              <p className="text-sm">You haven't saved any searches yet.</p>
              <p className="text-xs mt-1">Save your filters on the search page to receive alerts.</p>
            </div>
          ) : (
            savedSearches.map((search) => (
              <div
                key={search.id}
                className="p-4 rounded-xl border border-slate-800/80 bg-slate-800/30 hover:bg-slate-800/50 hover:border-slate-700/80 transition-all group flex items-start justify-between gap-4"
              >
                <button
                  onClick={() => {
                    onSelectSearch(search.serializedQuery);
                    onClose();
                  }}
                  className="flex-1 text-left"
                >
                  <p className="font-semibold text-sm text-slate-200 group-hover:text-primary transition-colors line-clamp-2">
                    {parseQuerySummary(search.serializedQuery)}
                  </p>
                  <div className="flex items-center gap-3 mt-2 text-[10px] text-muted-foreground">
                    <span className="flex items-center gap-1">
                      {search.notificationEnabled ? (
                        <>
                          <Bell className="w-3.5 h-3.5 text-emerald-400" />
                          <span className="text-emerald-400 font-medium">Alerts Active</span>
                        </>
                      ) : (
                        <>
                          <BellOff className="w-3.5 h-3.5" />
                          <span>Alerts Off</span>
                        </>
                      )}
                    </span>
                    <span>Saved {new Date(search.createdAt).toLocaleDateString()}</span>
                  </div>
                </button>

                <button
                  onClick={() => handleDelete(search.id)}
                  className="p-1.5 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 transition-all"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
