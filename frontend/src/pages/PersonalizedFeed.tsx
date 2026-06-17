import React, { useState } from 'react';
import { Sparkles, Sliders, CheckCircle2, Heart, HeartOff, Trash2, MapPin, BadgePercent, Settings, BookOpen, Languages, Check } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { apiClient } from '../services/api';
import { Locale, getTranslation } from '../utils/i18n';

interface RecommendedProperty {
  id: string;
  title: string;
  price: number;
  city: string;
  locality: string;
  bedrooms: number;
  bathrooms: number;
  recommendationReason: string;
  matchScore: number;
  thumbnailUrl: string;
}

const INITIAL_RECOMMENDATIONS: RecommendedProperty[] = [
  {
    id: 'rec-1',
    title: 'Modern 2-BHK Apartment near Tech Hub',
    price: 25000,
    city: 'Bangalore',
    locality: 'Indiranagar',
    bedrooms: 2,
    bathrooms: 2,
    recommendationReason: 'Based on your interest in locations close to tech hubs with gym access.',
    matchScore: 98,
    thumbnailUrl: 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=400&q=80',
  },
  {
    id: 'rec-2',
    title: 'Spacious flat with large balcony and security',
    price: 18000,
    city: 'Noida',
    locality: 'Sector 62',
    bedrooms: 2,
    bathrooms: 2,
    recommendationReason: 'Highly matching your budget limit and request for high security standard.',
    matchScore: 94,
    thumbnailUrl: 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=400&q=80',
  },
  {
    id: 'rec-3',
    title: 'Affordable Cozy Studio Flat for Students',
    price: 8500,
    city: 'Delhi',
    locality: 'North Campus',
    bedrooms: 1,
    bathrooms: 1,
    recommendationReason: 'Matches your preferences for university proximity and low budget.',
    matchScore: 89,
    thumbnailUrl: 'https://images.unsplash.com/photo-1555854877-bab0e564b8d5?auto=format&fit=crop&w=400&q=80',
  },
];

export default function PersonalizedFeed() {
  const [locale, setLocale] = useState<Locale>('en-IN');
  const [recommendations, setRecommendations] = useState<RecommendedProperty[]>(INITIAL_RECOMMENDATIONS);
  const [budgetLimit, setBudgetLimit] = useState(30000);
  const [targetCity, setTargetCity] = useState('Bangalore');
  const [hasGym, setHasGym] = useState(true);
  const [hasParking, setHasParking] = useState(false);
  const [savedIds, setSavedIds] = useState<string[]>([]);
  const [isUpdating, setIsUpdating] = useState(false);

  // Interaction logs (CTR tracker)
  const [clickLogs, setClickLogs] = useState<string[]>([]);

  const trackInteraction = async (action: string, propertyId: string, propertyTitle: string) => {
    const timestamp = new Date().toLocaleTimeString();
    const logEntry = `[${timestamp}] CTR Event: ${action} on "${propertyTitle}" (ID: ${propertyId})`;
    setClickLogs(prev => [logEntry, ...prev]);

    try {
      await apiClient.post('/ai/recommendations/ctr', {
        action,
        propertyId,
      });
    } catch (err) {
      console.warn('CTR endpoint response: logged locally.');
    }
  };

  const handleRemoveRecommendation = (id: string, title: string) => {
    trackInteraction('Dismiss / Hide', id, title);
    setRecommendations(recommendations.filter(r => r.id !== id));
  };

  const handleToggleSave = (id: string, title: string) => {
    const isSaved = savedIds.includes(id);
    trackInteraction(isSaved ? 'Unsave' : 'Save', id, title);

    if (isSaved) {
      setSavedIds(savedIds.filter(x => x !== id));
    } else {
      setSavedIds([...savedIds, id]);
    }
  };

  const handleRefinePreferences = async () => {
    setIsUpdating(true);
    setTimeout(() => {
      // Mocking recommendation update based on parameters
      const updated = INITIAL_RECOMMENDATIONS.map(item => {
        let scoreAdjust = 0;
        if (item.city === targetCity) scoreAdjust += 5;
        if (item.price <= budgetLimit) scoreAdjust += 3;
        
        return {
          ...item,
          matchScore: Math.min(99, item.matchScore + scoreAdjust),
        };
      }).sort((a, b) => b.matchScore - a.matchScore);

      setRecommendations(updated);
      setIsUpdating(false);
    }, 800);

    try {
      await apiClient.post('/ai/recommendations/refine', {
        budgetLimit,
        targetCity,
        hasGym,
        hasParking,
      });
    } catch (err) {
      console.warn('Backend call failed, updated parameters locally.');
    }
  };

  const t = (key: Parameters<typeof getTranslation>[1]) => getTranslation(locale, key);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 text-slate-100">
      {/* Top Banner */}
      <div className="relative overflow-hidden bg-slate-900 border border-indigo-950 rounded-2xl p-6 md:p-8 mb-8 shadow-2xl">
        <div className="absolute top-0 right-0 p-8 opacity-10 pointer-events-none">
          <Sparkles className="w-40 h-40 text-indigo-400" />
        </div>

        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
          <div className="max-w-2xl">
            <div className="inline-flex items-center space-x-1.5 px-2.5 py-0.5 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 text-xs font-semibold mb-3">
              <Sparkles className="w-3.5 h-3.5 animate-pulse" />
              <span>AI Recommendation Engine Active</span>
            </div>
            <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white via-indigo-100 to-indigo-300 bg-clip-text text-transparent">
              Your Personalized Feed
            </h1>
            <p className="text-slate-400 text-sm mt-2">
              These listings were handpicked by RoomWallah AI based on your recent searches, saved filters, and location preferences. Adjust preferences below to train the model.
            </p>
          </div>

          <div className="flex flex-col items-end gap-2 shrink-0">
            {/* Language Selection */}
            <div className="flex items-center space-x-1 bg-slate-950 border border-slate-800 rounded-lg px-2 py-1 text-xs">
              <Languages className="w-3.5 h-3.5 text-indigo-400" />
              <select
                value={locale}
                onChange={(e) => setLocale(e.target.value as Locale)}
                className="bg-transparent border-none focus:ring-0 focus:outline-none cursor-pointer py-0.5"
              >
                <option value="en-IN" className="bg-slate-950">EN (India)</option>
                <option value="hi-IN" className="bg-slate-950">HI (India)</option>
                <option value="mr-IN" className="bg-slate-950">MR (India)</option>
              </select>
            </div>

            {/* Active Recommendation Engine & Algorithm Version */}
            <span className="text-[10px] text-indigo-300 font-bold bg-indigo-550/10 border border-indigo-500/20 px-2.5 py-1 rounded-full">
              Algorithm: RoomGNN-v3-cf
            </span>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        {/* Sidebar Preference Calibration */}
        <div className="lg:col-span-1 space-y-6">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 h-fit">
            <h2 className="text-base font-bold text-white flex items-center gap-2 mb-4">
              <Settings className="w-4 h-4 text-indigo-400" />
              <span>Refine Recommendations</span>
            </h2>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-400 uppercase mb-1.5">Max Budget Limit (₹)</label>
                <input
                  type="number"
                  value={budgetLimit}
                  onChange={(e) => setBudgetLimit(parseInt(e.target.value) || 0)}
                  className="w-full bg-slate-950 border border-slate-850 rounded-lg p-2.5 text-xs text-slate-200 focus:border-indigo-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-400 uppercase mb-1.5">Target Location</label>
                <select
                  value={targetCity}
                  onChange={(e) => setTargetCity(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-850 rounded-lg p-2.5 text-xs text-slate-200 focus:border-indigo-500 focus:outline-none"
                >
                  <option value="Bangalore">Bangalore</option>
                  <option value="Noida">Noida</option>
                  <option value="Mumbai">Mumbai</option>
                  <option value="Delhi">Delhi</option>
                </select>
              </div>

              <div className="space-y-2 pt-2">
                <label className="flex items-center space-x-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={hasGym}
                    onChange={(e) => setHasGym(e.target.checked)}
                    className="rounded bg-slate-950 border-slate-800 text-indigo-500 focus:ring-0"
                  />
                  <span className="text-xs text-slate-350 font-medium">Must include Gymnasium</span>
                </label>

                <label className="flex items-center space-x-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={hasParking}
                    onChange={(e) => setHasParking(e.target.checked)}
                    className="rounded bg-slate-950 border-slate-800 text-indigo-500 focus:ring-0"
                  />
                  <span className="text-xs text-slate-350 font-medium">Must include Parking</span>
                </label>
              </div>

              <button
                onClick={handleRefinePreferences}
                disabled={isUpdating}
                className="w-full bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold rounded-lg py-2.5 mt-2 transition-colors animate-fade-in"
              >
                {isUpdating ? 'Re-training Models...' : 'Apply Feed Weights'}
              </button>
            </div>
          </div>

          {/* Click-Through Interaction Tracker Log Panel */}
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5">
            <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-3">Interaction Click-Tracker</h3>
            {clickLogs.length === 0 ? (
              <p className="text-[10px] text-slate-500 italic">No recommendations clicked yet. Interactions are logged to CTR metrics dashboard.</p>
            ) : (
              <div className="space-y-2 max-h-[160px] overflow-y-auto pr-1">
                {clickLogs.map((log, index) => (
                  <div key={index} className="p-2 bg-slate-950 border border-slate-850 rounded text-[9px] font-mono text-slate-400 leading-snug">
                    {log}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Main List Recommendations */}
        <div className="lg:col-span-3 space-y-6">
          <div className="flex justify-between items-center px-1">
            <h2 className="text-lg font-semibold text-slate-200">
              Recommended Properties ({recommendations.length})
            </h2>
            <span className="text-xs text-slate-400">Match score based on profile metrics</span>
          </div>

          <div className="space-y-4">
            <AnimatePresence mode="popLayout">
              {recommendations.map((item) => (
                <motion.div
                  key={item.id}
                  layout
                  initial={{ opacity: 0, y: 15 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.95 }}
                  transition={{ duration: 0.3 }}
                  className="bg-slate-900 border border-slate-800/80 rounded-2xl overflow-hidden shadow-lg p-5 flex flex-col md:flex-row gap-5 relative hover:border-slate-700 transition-colors"
                >
                  {/* Property Image & Match Badge */}
                  <div className="md:w-1/4 h-32 md:h-auto min-h-[120px] relative rounded-xl overflow-hidden shrink-0">
                    <img
                      src={item.thumbnailUrl}
                      alt={item.title}
                      className="w-full h-full object-cover"
                    />
                    <div className="absolute top-2 left-2 bg-indigo-600 text-white font-extrabold text-[10px] px-2 py-0.5 rounded-full flex items-center gap-0.5">
                      <BadgePercent className="w-3.5 h-3.5" />
                      <span>{item.matchScore}% Match</span>
                    </div>
                  </div>

                  {/* Recommendation Details */}
                  <div className="flex-grow flex flex-col justify-between">
                    <div>
                      {/* Reason text */}
                      <p className="text-xs text-indigo-400 font-semibold mb-1 flex items-center gap-1">
                        <Sparkles className="w-3.5 h-3.5" />
                        <span>{item.recommendationReason}</span>
                      </p>

                      <h3
                        onClick={() => trackInteraction('View Details Title Click', item.id, item.title)}
                        className="text-base font-bold text-white mb-2 leading-tight cursor-pointer hover:text-indigo-400 transition-colors"
                      >
                        {item.title}
                      </h3>

                      <div className="flex items-center gap-4 text-xs text-slate-400">
                        <span className="flex items-center gap-1">
                          <MapPin className="w-3.5 h-3.5 text-indigo-400" />
                          {item.locality}, {item.city}
                        </span>
                        <span>{item.bedrooms} Bedrooms</span>
                        <span>{item.bathrooms} Bathrooms</span>
                      </div>
                    </div>

                    <div className="flex items-center justify-between border-t border-slate-800/60 pt-3 mt-4">
                      <div className="text-sm font-extrabold text-indigo-400 font-mono">
                        ₹{item.price.toLocaleString()} <span className="text-[10px] text-slate-400 font-normal">/ month</span>
                      </div>

                      <div className="flex items-center gap-2">
                        {/* Save Action */}
                        <button
                          onClick={() => handleToggleSave(item.id, item.title)}
                          className={`p-2 rounded-lg border transition-colors ${
                            savedIds.includes(item.id)
                              ? 'bg-rose-500/10 border-rose-500/30 text-rose-500 hover:bg-rose-500/20'
                              : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-rose-500 hover:border-rose-500/30'
                          }`}
                          title="Save Recommendation"
                        >
                          <Heart className="w-4 h-4 fill-current" />
                        </button>

                        {/* Remove Action */}
                        <button
                          onClick={() => handleRemoveRecommendation(item.id, item.title)}
                          className="p-2 bg-slate-950 border border-slate-800 text-slate-400 hover:text-red-500 hover:border-red-500/30 rounded-lg transition-colors"
                          title="Not Interested"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>

                        <button
                          onClick={() => trackInteraction('Book Visit CTA Click', item.id, item.title)}
                          className="px-3 py-1.5 bg-slate-850 hover:bg-slate-800 border border-slate-700 hover:border-slate-650 rounded-lg text-xs font-semibold text-slate-200 transition-all"
                        >
                          Book Instant Visit
                        </button>
                      </div>
                    </div>
                  </div>
                </motion.div>
              ))}
            </AnimatePresence>

            {recommendations.length === 0 && (
              <div className="text-center py-12 border border-slate-800 rounded-2xl bg-slate-900/30">
                <p className="text-slate-400 text-sm">No recommendations left in your feed queue.</p>
                <button
                  onClick={() => setRecommendations(INITIAL_RECOMMENDATIONS)}
                  className="mt-3 text-xs bg-indigo-600 hover:bg-indigo-500 text-white font-semibold px-4 py-2 rounded-lg transition-all"
                >
                  Reload Mock recommendations
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
