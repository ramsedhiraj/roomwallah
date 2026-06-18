import React, { useState, useEffect } from 'react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from 'recharts';
import { 
  Sliders, RefreshCw, Save, MapPin, 
  Eye, CheckCircle2 
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

interface PreviewListing {
  id: string;
  title: string;
  price: number;
  score: number;
  locality: string;
  city: string;
  createdDaysAgo: number;
}

const mockListings: PreviewListing[] = [
  { id: '1', title: 'Premium 2 BHK Apartment near IT Hub', price: 42000, score: 95, locality: 'Indiranagar', city: 'Bengaluru', createdDaysAgo: 2 },
  { id: '2', title: 'Cozy PG Room with High-Speed Wi-Fi', price: 15000, score: 88, locality: 'Koramangala', city: 'Bengaluru', createdDaysAgo: 12 },
  { id: '3', title: 'Luxury 3 BHK Penthouse with Balcony', price: 85000, score: 82, locality: 'Bandra West', city: 'Mumbai', createdDaysAgo: 1 },
  { id: '4', title: 'Furnished Studio Flat in Gated Society', price: 24000, score: 79, locality: 'GK 2', city: 'Delhi', createdDaysAgo: 20 },
  { id: '5', title: 'Spacious 1 BHK flat for rent', price: 18000, score: 74, locality: 'DLF Phase 3', city: 'Gurugram', createdDaysAgo: 5 },
];

export default function RecommendationConfig() {
  const [budget, setBudget] = useState(30);
  const [proximity, setProximity] = useState(25);
  const [amenities, setAmenities] = useState(15);
  const [popularity, setPopularity] = useState(20);
  const [recency, setRecency] = useState(10);
  
  const [previewList, setPreviewList] = useState<PreviewListing[]>(mockListings);
  const [showToast, setShowToast] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Recalculate scores and resort preview list based on weights
  useEffect(() => {
    const total = budget + proximity + amenities + popularity + recency;
    if (total === 0) return;

    // Simulated scoring algorithm
    const updated = mockListings.map(listing => {
      // Base attributes
      const priceFactor = listing.price < 30000 ? 90 : 60;
      const distFactor = listing.locality === 'Indiranagar' || listing.locality === 'Bandra West' ? 95 : 70;
      const amenitiesFactor = 80;
      const popFactor = 85;
      const recencyFactor = Math.max(100 - listing.createdDaysAgo * 4, 10);

      // Weighted average score
      const score = Math.round(
        (priceFactor * budget +
         distFactor * proximity +
         amenitiesFactor * amenities +
         popFactor * popularity +
         recencyFactor * recency) / total
      );

      return { ...listing, score };
    });

    // Sort by score descending
    updated.sort((a, b) => b.score - a.score);
    setPreviewList(updated);
  }, [budget, proximity, amenities, popularity, recency]);

  const handlePublish = () => {
    setShowToast(true);
    setTimeout(() => setShowToast(false), 3000);
  };

  const handleReset = () => {
    setIsRefreshing(true);
    setTimeout(() => {
      setBudget(30);
      setProximity(25);
      setAmenities(15);
      setPopularity(20);
      setRecency(10);
      setIsRefreshing(false);
    }, 500);
  };

  // Prepare chart data
  const totalWeight = budget + proximity + amenities + popularity + recency;
  const chartData = [
    { name: 'Budget Similarity', value: budget, color: '#6366f1' },
    { name: 'Proximity', value: proximity, color: '#a855f7' },
    { name: 'Amenities Match', value: amenities, color: '#ec4899' },
    { name: 'Popularity', value: popularity, color: '#10b981' },
    { name: 'Recency Decay', value: recency, color: '#eab308' },
  ].filter(d => d.value > 0);

  const getPercentage = (val: number) => {
    if (totalWeight === 0) return '0%';
    return `${Math.round((val / totalWeight) * 100)}%`;
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6 text-slate-100 animate-fade-in relative">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
            <Sliders className="w-8 h-8 text-primary" />
            Recommendation Model Configurator
          </h1>
          <p className="text-muted-foreground text-sm">
            Tune algorithmic parameters for the matching search index and personal suggestions engine.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={handleReset}
            className="p-2.5 border border-slate-800 rounded-xl bg-slate-900/50 hover:bg-slate-900 transition-all text-slate-400 hover:text-white"
            title="Reset Defaults"
          >
            <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin text-primary' : ''}`} />
          </button>
          <button
            onClick={handlePublish}
            className="flex items-center gap-1.5 px-5 py-2.5 bg-gradient-to-r from-primary to-secondary text-white font-bold text-xs rounded-xl hover:opacity-95 shadow-md shadow-indigo-500/10 transition-all hover:translate-y-[-1px] active:translate-y-[0px]"
          >
            <Save className="w-4 h-4" />
            <span>Publish Weights</span>
          </button>
        </div>
      </div>

      {/* Main content grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Left Column: Sliders */}
        <div className="lg:col-span-2 space-y-6">
          <div className="glass rounded-3xl p-6 md:p-8 border border-white/5 space-y-6">
            <h2 className="text-md font-bold text-slate-200">Algorithmic Scoring Matrix</h2>
            <p className="text-xs text-slate-450 leading-relaxed">
              Adjust sliders below to modify the importance of each metric. Total weights ratio determines final ranking score placement.
            </p>

            <div className="space-y-6">
              {/* Budget Similarity */}
              <div className="space-y-2 p-4 bg-slate-950/40 border border-slate-900 rounded-2xl">
                <div className="flex justify-between items-center text-xs">
                  <div>
                    <span className="font-bold text-slate-200">Budget Similarity Weight</span>
                    <span className="text-[10px] text-slate-500 block">Prefers homes close to user target budget.</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <input 
                      type="number" 
                      min="0" 
                      max="100" 
                      value={budget} 
                      onChange={(e) => setBudget(Math.max(0, Math.min(100, parseInt(e.target.value) || 0)))}
                      className="w-12 text-center py-1 bg-slate-950 border border-slate-800 rounded font-mono font-bold text-slate-200 focus:outline-none focus:border-primary text-xs" 
                    />
                    <span className="text-[10px] font-bold text-indigo-400 font-mono w-8 text-right">{getPercentage(budget)}</span>
                  </div>
                </div>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={budget}
                  onChange={(e) => setBudget(parseInt(e.target.value))}
                  className="w-full accent-primary h-1.5 bg-slate-900 rounded-lg cursor-pointer"
                />
              </div>

              {/* Proximity */}
              <div className="space-y-2 p-4 bg-slate-950/40 border border-slate-900 rounded-2xl">
                <div className="flex justify-between items-center text-xs">
                  <div>
                    <span className="font-bold text-slate-200">Proximity & Distance Weight</span>
                    <span className="text-[10px] text-slate-500 block">Prefers listings close to client work coordinates.</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <input 
                      type="number" 
                      min="0" 
                      max="100" 
                      value={proximity} 
                      onChange={(e) => setProximity(Math.max(0, Math.min(100, parseInt(e.target.value) || 0)))}
                      className="w-12 text-center py-1 bg-slate-950 border border-slate-800 rounded font-mono font-bold text-slate-200 focus:outline-none focus:border-primary text-xs" 
                    />
                    <span className="text-[10px] font-bold text-purple-400 font-mono w-8 text-right">{getPercentage(proximity)}</span>
                  </div>
                </div>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={proximity}
                  onChange={(e) => setProximity(parseInt(e.target.value))}
                  className="w-full accent-purple-500 h-1.5 bg-slate-900 rounded-lg cursor-pointer"
                />
              </div>

              {/* Amenities Match */}
              <div className="space-y-2 p-4 bg-slate-950/40 border border-slate-900 rounded-2xl">
                <div className="flex justify-between items-center text-xs">
                  <div>
                    <span className="font-bold text-slate-200">Amenities Matching Weight</span>
                    <span className="text-[10px] text-slate-500 block">Prefers homes that overlap with user selected filters (Wi-Fi, parking, pets).</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <input 
                      type="number" 
                      min="0" 
                      max="100" 
                      value={amenities} 
                      onChange={(e) => setAmenities(Math.max(0, Math.min(100, parseInt(e.target.value) || 0)))}
                      className="w-12 text-center py-1 bg-slate-950 border border-slate-800 rounded font-mono font-bold text-slate-200 focus:outline-none focus:border-primary text-xs" 
                    />
                    <span className="text-[10px] font-bold text-pink-400 font-mono w-8 text-right">{getPercentage(amenities)}</span>
                  </div>
                </div>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={amenities}
                  onChange={(e) => setAmenities(parseInt(e.target.value))}
                  className="w-full accent-pink-500 h-1.5 bg-slate-900 rounded-lg cursor-pointer"
                />
              </div>

              {/* Popularity */}
              <div className="space-y-2 p-4 bg-slate-950/40 border border-slate-900 rounded-2xl">
                <div className="flex justify-between items-center text-xs">
                  <div>
                    <span className="font-bold text-slate-200">Popularity Weight</span>
                    <span className="text-[10px] text-slate-500 block">Boosts listings with high view counts and search click rates.</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <input 
                      type="number" 
                      min="0" 
                      max="100" 
                      value={popularity} 
                      onChange={(e) => setPopularity(Math.max(0, Math.min(100, parseInt(e.target.value) || 0)))}
                      className="w-12 text-center py-1 bg-slate-950 border border-slate-800 rounded font-mono font-bold text-slate-200 focus:outline-none focus:border-primary text-xs" 
                    />
                    <span className="text-[10px] font-bold text-emerald-400 font-mono w-8 text-right">{getPercentage(popularity)}</span>
                  </div>
                </div>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={popularity}
                  onChange={(e) => setPopularity(parseInt(e.target.value))}
                  className="w-full accent-emerald-500 h-1.5 bg-slate-900 rounded-lg cursor-pointer"
                />
              </div>

              {/* Recency Decay */}
              <div className="space-y-2 p-4 bg-slate-950/40 border border-slate-900 rounded-2xl">
                <div className="flex justify-between items-center text-xs">
                  <div>
                    <span className="font-bold text-slate-200">Recency Decay Weight</span>
                    <span className="text-[10px] text-slate-500 block">Boosts recently published properties (decreases score over time).</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <input 
                      type="number" 
                      min="0" 
                      max="100" 
                      value={recency} 
                      onChange={(e) => setRecency(Math.max(0, Math.min(100, parseInt(e.target.value) || 0)))}
                      className="w-12 text-center py-1 bg-slate-950 border border-slate-800 rounded font-mono font-bold text-slate-200 focus:outline-none focus:border-primary text-xs" 
                    />
                    <span className="text-[10px] font-bold text-amber-400 font-mono w-8 text-right">{getPercentage(recency)}</span>
                  </div>
                </div>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={recency}
                  onChange={(e) => setRecency(parseInt(e.target.value))}
                  className="w-full accent-amber-500 h-1.5 bg-slate-900 rounded-lg cursor-pointer"
                />
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Preview & Visual Composition */}
        <div className="space-y-6">
          {/* Weights Pie Chart */}
          <div className="glass rounded-3xl p-6 border border-white/5 space-y-4">
            <h2 className="text-sm font-bold">Weights Composition</h2>
            <div className="h-44 relative flex justify-center items-center">
              {totalWeight > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={chartData}
                      cx="50%"
                      cy="50%"
                      innerRadius={45}
                      outerRadius={60}
                      paddingAngle={3}
                      dataKey="value"
                    >
                      {chartData.map((entry) => (
                        <Cell key={`cell-${entry.name}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px' }} />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <span className="text-xs text-slate-500">No weights selected.</span>
              )}
              <div className="absolute text-center flex flex-col items-center">
                <span className="text-xl font-black text-white">{totalWeight}</span>
                <span className="text-[8px] text-slate-500 uppercase font-bold tracking-wider">Total Ratio</span>
              </div>
            </div>
          </div>

          {/* Preview Recommendations List */}
          <div className="glass rounded-3xl p-6 border border-white/5 space-y-4">
            <div className="flex items-center gap-1.5">
              <Eye className="w-4 h-4 text-primary shrink-0" />
              <h3 className="font-bold text-sm text-white">Preview Recommendation Rank</h3>
            </div>
            <p className="text-[10px] text-slate-550 leading-relaxed">
              Live preview ranking matches based on slider changes.
            </p>

            <div className="space-y-3 pt-1">
              {previewList.slice(0, 4).map((listing) => (
                <div 
                  key={listing.id}
                  className="p-3 rounded-xl bg-slate-950/50 border border-slate-900 flex justify-between items-center gap-3 text-xs"
                >
                  <div className="space-y-1 truncate">
                    <span className="font-bold text-slate-200 block truncate leading-none">{listing.title}</span>
                    <div className="text-[10px] text-slate-500 flex items-center gap-1.5 font-semibold">
                      <MapPin className="w-2.5 h-2.5 text-rose-500/60" />
                      <span>{listing.locality}, {listing.city}</span>
                      <span>·</span>
                      <span>₹{listing.price.toLocaleString()}</span>
                    </div>
                  </div>
                  <div className="text-right shrink-0">
                    <span className="text-base font-black text-primary-light block leading-none">{listing.score}</span>
                    <span className="text-[8px] text-slate-550 font-bold uppercase tracking-wider">Score</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Toast Notification */}
      <AnimatePresence>
        {showToast && (
          <motion.div
            initial={{ opacity: 0, y: 50, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 50, scale: 0.9 }}
            className="fixed bottom-6 right-6 p-4 bg-emerald-950/90 border border-emerald-500/30 text-emerald-300 rounded-2xl flex items-center gap-3 shadow-2xl z-55 text-xs"
          >
            <CheckCircle2 className="w-5 h-5 text-emerald-450 shrink-0" />
            <div>
              <span className="font-bold text-white block">Scoring Weights Published</span>
              <span>Personalized search recommendation cache refreshed.</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
