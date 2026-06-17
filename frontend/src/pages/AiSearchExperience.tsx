import React, { useState, useEffect } from 'react';
import { Search, Sparkles, Sliders, Brain, ArrowRight, CheckCircle2, AlertTriangle, ShieldCheck, MapPin, Heart } from 'lucide-react';
import { apiClient } from '../services/api';
import { motion, AnimatePresence } from 'framer-motion';

interface PropertyResult {
  id: string;
  title: string;
  price: number;
  city: string;
  locality: string;
  bedrooms: number;
  bathrooms: number;
  petFriendly: boolean;
  amenities: string[];
  thumbnailUrl: string;
  description: string;
  matchScore: number;
  insights: {
    pros: string[];
    cons: string[];
  };
}

const MOCK_PROPERTIES: Omit<PropertyResult, 'matchScore' | 'insights'>[] = [
  {
    id: 'prop-1',
    title: 'Cozy 1-BHK Apartment near Tech Hub',
    price: 15000,
    city: 'Bangalore',
    locality: 'Indiranagar',
    bedrooms: 1,
    bathrooms: 1,
    petFriendly: true,
    amenities: ['Balcony', 'High-speed Internet', 'Parking', 'Gym'],
    thumbnailUrl: 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=400&q=80',
    description: 'A cozy 1 BHK with a nice balcony, perfect for software professionals. Located 5 mins from the metro station. High-speed internet pre-installed.',
  },
  {
    id: 'prop-2',
    title: 'Spacious 2-BHK Flat with Balcony & Gym',
    price: 28000,
    city: 'Noida',
    locality: 'Sector 62',
    bedrooms: 2,
    bathrooms: 2,
    petFriendly: false,
    amenities: ['Balcony', 'Gym', 'Power Backup', 'Security'],
    thumbnailUrl: 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=400&q=80',
    description: 'Beautiful 2 BHK in a high-rise society. Comes with a functional gymnasium, large balcony, and 24/7 security. Walking distance from major corporate parks.',
  },
  {
    id: 'prop-3',
    title: 'Luxury 3-BHK Villa with Private Garden',
    price: 65000,
    city: 'Mumbai',
    locality: 'Bandra',
    bedrooms: 3,
    bathrooms: 3,
    petFriendly: true,
    amenities: ['Private Garden', 'Gym', 'Parking', 'Swimming Pool', 'Security'],
    thumbnailUrl: 'https://images.unsplash.com/photo-1613977257363-707ba9348227?auto=format&fit=crop&w=400&q=80',
    description: 'Stunning villa in Bandra. Features a private landscaped garden, home automation, modular kitchen, and shared access to a premium swimming pool and gym.',
  },
  {
    id: 'prop-4',
    title: 'Affordable PG Bed for Students',
    price: 6000,
    city: 'Delhi',
    locality: 'North Campus',
    bedrooms: 1,
    bathrooms: 1,
    petFriendly: false,
    amenities: ['High-speed Internet', 'Air Conditioning', 'Power Backup'],
    thumbnailUrl: 'https://images.unsplash.com/photo-1555854877-bab0e564b8d5?auto=format&fit=crop&w=400&q=80',
    description: 'Shared room PG accommodation for students. Minutes away from Delhi University North Campus. Includes meals, Wi-Fi, and AC.',
  },
];

export default function AiSearchExperience() {
  const [query, setQuery] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [results, setResults] = useState<PropertyResult[]>([]);
  const [showTuning, setShowTuning] = useState(false);
  const [showIntent, setShowIntent] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  // Intent analysis details extracted from query
  const [parsedIntent, setParsedIntent] = useState<{
    location?: string;
    budgetLimit?: number;
    bedrooms?: number;
    amenities: string[];
    petPreference?: boolean;
    vectorRep: number[];
  } | null>(null);

  // Importance weights (0 - 100)
  const [weights, setWeights] = useState({
    budget: 80,
    location: 70,
    amenities: 50,
    petFriendly: 40,
  });

  const handleSearchSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!query.trim()) return;

    setIsSearching(true);
    setHasSearched(true);

    // Analyze query intent local mock
    const lowerQuery = query.toLowerCase();
    const extractedLoc = MOCK_PROPERTIES.find(p => lowerQuery.includes(p.locality.toLowerCase()) || lowerQuery.includes(p.city.toLowerCase()))
      ? MOCK_PROPERTIES.find(p => lowerQuery.includes(p.locality.toLowerCase()) || lowerQuery.includes(p.city.toLowerCase()))?.locality
      : undefined;

    let extractedBedrooms = undefined;
    if (lowerQuery.includes('1 bhk') || lowerQuery.includes('1-bhk') || lowerQuery.includes('1 bedroom') || lowerQuery.includes('single')) {
      extractedBedrooms = 1;
    } else if (lowerQuery.includes('2 bhk') || lowerQuery.includes('2-bhk') || lowerQuery.includes('2 bedroom') || lowerQuery.includes('double')) {
      extractedBedrooms = 2;
    } else if (lowerQuery.includes('3 bhk') || lowerQuery.includes('3-bhk') || lowerQuery.includes('3 bedroom') || lowerQuery.includes('villa')) {
      extractedBedrooms = 3;
    }

    let extractedBudget = undefined;
    const budgetMatch = lowerQuery.match(/(?:under|below|max|budget)\s*(?:rs\.?|\$)?\s*(\d+)(?:k|000)?/i);
    if (budgetMatch) {
      const value = parseInt(budgetMatch[1]);
      extractedBudget = lowerQuery.includes(`${value}k`) ? value * 1000 : value;
    }

    const extractedAmenities: string[] = [];
    if (lowerQuery.includes('balcony')) extractedAmenities.push('Balcony');
    if (lowerQuery.includes('gym') || lowerQuery.includes('fitness')) extractedAmenities.push('Gym');
    if (lowerQuery.includes('internet') || lowerQuery.includes('wifi')) extractedAmenities.push('High-speed Internet');
    if (lowerQuery.includes('garden') || lowerQuery.includes('yard')) extractedAmenities.push('Private Garden');
    if (lowerQuery.includes('pool') || lowerQuery.includes('swim')) extractedAmenities.push('Swimming Pool');

    const petPreference = lowerQuery.includes('pet') || lowerQuery.includes('dog') || lowerQuery.includes('cat');

    // Vector mock representation
    const vec = Array.from({ length: 8 }, () => parseFloat((Math.random() * 2 - 1).toFixed(3)));

    setParsedIntent({
      location: extractedLoc,
      budgetLimit: extractedBudget,
      bedrooms: extractedBedrooms,
      amenities: extractedAmenities,
      petPreference,
      vectorRep: vec,
    });

    try {
      // Call actual backend if active
      const response = await apiClient.post('/ai/search', {
        query,
        weights,
      });
      if (response.data?.results) {
        setResults(response.data.results);
      } else {
        calculateScoresAndDisplay(extractedLoc, extractedBudget, extractedBedrooms, extractedAmenities, petPreference);
      }
    } catch (err) {
      // Fallback
      calculateScoresAndDisplay(extractedLoc, extractedBudget, extractedBedrooms, extractedAmenities, petPreference);
    } finally {
      setIsSearching(false);
    }
  };

  const calculateScoresAndDisplay = (
    loc?: string,
    budget?: number,
    beds?: number,
    amenities: string[] = [],
    petFriendly?: boolean
  ) => {
    // Generate scores based on weights
    const scored = MOCK_PROPERTIES.map(p => {
      let score = 50; // baseline

      // Budget score
      let budgetDiffPct = 0;
      if (budget) {
        if (p.price <= budget) {
          score += 20 * (weights.budget / 100);
        } else {
          budgetDiffPct = (p.price - budget) / budget;
          score -= Math.min(30, budgetDiffPct * 30) * (weights.budget / 100);
        }
      } else {
        score += 10;
      }

      // Location score
      if (loc) {
        if (p.locality.toLowerCase() === loc.toLowerCase() || p.city.toLowerCase() === loc.toLowerCase()) {
          score += 25 * (weights.location / 100);
        } else {
          score -= 10 * (weights.location / 100);
        }
      } else {
        score += 15;
      }

      // Bedrooms score
      if (beds) {
        if (p.bedrooms === beds) {
          score += 15;
        } else {
          score -= Math.abs(p.bedrooms - beds) * 10;
        }
      }

      // Amenities score
      let matchingAmenitiesCount = 0;
      amenities.forEach(a => {
        if (p.amenities.map(x => x.toLowerCase()).includes(a.toLowerCase())) {
          matchingAmenitiesCount++;
        }
      });
      if (amenities.length > 0) {
        const matchRatio = matchingAmenitiesCount / amenities.length;
        score += matchRatio * 20 * (weights.amenities / 100);
      } else {
        score += 10;
      }

      // Pet score
      if (petFriendly) {
        if (p.petFriendly) {
          score += 15 * (weights.petFriendly / 100);
        } else {
          score -= 15 * (weights.petFriendly / 100);
        }
      }

      // bound score between 40 and 99
      const finalScore = Math.max(45, Math.min(99, Math.round(score)));

      // Generate AI Insights dynamically
      const pros: string[] = [];
      const cons: string[] = [];

      if (budget && p.price <= budget) {
        pros.push(`Fits budget: Under your limit of ₹${budget.toLocaleString()}`);
      } else if (budget) {
        cons.push(`Slightly above budget by ₹${(p.price - budget).toLocaleString()}`);
      } else {
        pros.push(`Excellent value at ₹${p.price.toLocaleString()}/mo`);
      }

      if (loc && (p.locality.toLowerCase() === loc.toLowerCase() || p.city.toLowerCase() === loc.toLowerCase())) {
        pros.push(`Perfect match in ${p.locality}, ${p.city}`);
      } else if (loc) {
        cons.push(`Located in ${p.locality} instead of ${loc}`);
      }

      if (beds && p.bedrooms === beds) {
        pros.push(`Exactly ${beds} bedroom(s) as requested`);
      }

      if (matchingAmenitiesCount > 0) {
        pros.push(`Includes ${matchingAmenitiesCount} of your requested amenities (${amenities.join(', ')})`);
      } else if (amenities.length > 0) {
        cons.push(`Missing requested amenities`);
      }

      if (petFriendly && p.petFriendly) {
        pros.push('Pet friendly environment');
      } else if (petFriendly && !p.petFriendly) {
        cons.push('Does not support pets');
      }

      return {
        ...p,
        matchScore: finalScore,
        insights: {
          pros: pros.length > 0 ? pros : ['Good overall criteria match'],
          cons: cons.length > 0 ? cons : ['No major compromise points identified'],
        },
      };
    });

    // Sort by matchScore descending
    scored.sort((a, b) => b.matchScore - a.matchScore);
    setResults(scored);
  };

  // Recalculate if weights change and we have intent parsed
  useEffect(() => {
    if (parsedIntent) {
      calculateScoresAndDisplay(
        parsedIntent.location,
        parsedIntent.budgetLimit,
        parsedIntent.bedrooms,
        parsedIntent.amenities,
        parsedIntent.petPreference
      );
    }
  }, [weights]);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 text-slate-100">
      {/* Page Header */}
      <div className="text-center max-w-3xl mx-auto mb-10">
        <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/25 text-indigo-400 text-xs font-semibold mb-3">
          <Sparkles className="w-3.5 h-3.5" />
          <span>Next-Gen Semantic Search</span>
        </div>
        <h1 className="text-4xl font-extrabold tracking-tight bg-gradient-to-r from-white via-indigo-200 to-indigo-400 bg-clip-text text-transparent mb-4">
          Natural Language AI Search
        </h1>
        <p className="text-slate-400 text-base">
          Describe your dream home in your own words. Our AI model interprets your exact intent, budget boundaries, and amenities to find the absolute best match.
        </p>
      </div>

      {/* Main Search Input Form */}
      <div className="max-w-4xl mx-auto mb-8">
        <form onSubmit={handleSearchSubmit} className="relative">
          <div className="relative flex items-center bg-slate-900/95 border-2 border-indigo-900/50 focus-within:border-indigo-500 rounded-2xl p-2 shadow-2xl transition-all">
            <div className="pl-3 text-indigo-400">
              <Brain className="w-6 h-6 animate-pulse-subtle" />
            </div>
            <input
              type="text"
              className="w-full bg-transparent border-0 focus:outline-none focus:ring-0 text-slate-100 placeholder-slate-500 text-lg px-4 py-3"
              placeholder="e.g., Cozy 2 BHK flat near Indiranagar Bangalore with balcony and gym under 30k"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              data-testid="semantic-search-input"
            />
            <button
              type="submit"
              disabled={isSearching}
              className="bg-indigo-600 hover:bg-indigo-500 text-white font-semibold rounded-xl px-6 py-3 transition-colors flex items-center space-x-2"
              data-testid="semantic-search-btn"
            >
              {isSearching ? (
                <span>Thinking...</span>
              ) : (
                <>
                  <span>Search</span>
                  <Search className="w-4 h-4" />
                </>
              )}
            </button>
          </div>
        </form>

        {/* Suggestion Chips */}
        <div className="flex flex-wrap items-center justify-center gap-2 mt-4 text-sm text-slate-400">
          <span>Try:</span>
          {[
            'Cheap 1 BHK in Indiranagar under 20000',
            'Luxury villa with private garden in Bandra',
            '2 BHK flat Noida Sector 62 with gym and balcony',
            'Pet friendly apartment with internet',
          ].map((chip) => (
            <button
              key={chip}
              onClick={() => setQuery(chip)}
              className="px-3.5 py-1 rounded-full bg-slate-900 border border-slate-800 hover:border-slate-700 hover:text-white transition-colors text-xs"
            >
              {chip}
            </button>
          ))}
        </div>
      </div>

      {/* Accordion Controls for Weight Tuning & Parsed Intent */}
      {hasSearched && (
        <div className="max-w-4xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
          {/* AI Weight Calibration Panel */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 backdrop-blur-md">
            <button
              onClick={() => setShowTuning(!showTuning)}
              className="flex items-center justify-between w-full font-bold text-slate-200 hover:text-white transition-colors"
            >
              <span className="flex items-center space-x-2">
                <Sliders className="w-4 h-4 text-indigo-400" />
                <span>Adjust AI Weight Preferences</span>
              </span>
              <span className="text-xs text-indigo-400 font-semibold bg-indigo-500/10 px-2 py-0.5 rounded">
                {showTuning ? 'Hide' : 'Show'}
              </span>
            </button>

            {showTuning && (
              <div className="mt-4 space-y-4">
                <p className="text-xs text-slate-400">
                  Calibrate what matters most. The AI will recalculate match confidence values immediately.
                </p>
                <div>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-slate-300">Budget Match Weight</span>
                    <span className="text-indigo-400 font-semibold">{weights.budget}%</span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="100"
                    value={weights.budget}
                    onChange={(e) => setWeights({ ...weights, budget: parseInt(e.target.value) })}
                    className="w-full h-1 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                  />
                </div>
                <div>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-slate-300">Location Proximity Weight</span>
                    <span className="text-indigo-400 font-semibold">{weights.location}%</span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="100"
                    value={weights.location}
                    onChange={(e) => setWeights({ ...weights, location: parseInt(e.target.value) })}
                    className="w-full h-1 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                  />
                </div>
                <div>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-slate-300">Amenities Match Weight</span>
                    <span className="text-indigo-400 font-semibold">{weights.amenities}%</span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="100"
                    value={weights.amenities}
                    onChange={(e) => setWeights({ ...weights, amenities: parseInt(e.target.value) })}
                    className="w-full h-1 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                  />
                </div>
                <div>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-slate-300">Pet Friendliness Priority</span>
                    <span className="text-indigo-400 font-semibold">{weights.petFriendly}%</span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="100"
                    value={weights.petFriendly}
                    onChange={(e) => setWeights({ ...weights, petFriendly: parseInt(e.target.value) })}
                    className="w-full h-1 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                  />
                </div>
              </div>
            )}
          </div>

          {/* Parsed Intent & Embeddings Panel */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 backdrop-blur-md">
            <button
              onClick={() => setShowIntent(!showIntent)}
              className="flex items-center justify-between w-full font-bold text-slate-200 hover:text-white transition-colors"
            >
              <span className="flex items-center space-x-2">
                <Brain className="w-4 h-4 text-indigo-400" />
                <span>View Parsed Intent & Vector Schema</span>
              </span>
              <span className="text-xs text-indigo-400 font-semibold bg-indigo-500/10 px-2 py-0.5 rounded">
                {showIntent ? 'Hide' : 'Show'}
              </span>
            </button>

            {showIntent && parsedIntent && (
              <div className="mt-4 space-y-3 text-xs">
                <div className="grid grid-cols-2 gap-2 text-slate-300">
                  <div className="bg-slate-950 p-2 rounded">
                    <span className="block text-slate-500 font-medium">Extracted Location</span>
                    <span className="font-semibold text-indigo-300">{parsedIntent.location || 'Anywhere'}</span>
                  </div>
                  <div className="bg-slate-950 p-2 rounded">
                    <span className="block text-slate-500 font-medium">Max Budget Cap</span>
                    <span className="font-semibold text-indigo-300">
                      {parsedIntent.budgetLimit ? `₹${parsedIntent.budgetLimit.toLocaleString()}` : 'No limit'}
                    </span>
                  </div>
                  <div className="bg-slate-950 p-2 rounded">
                    <span className="block text-slate-500 font-medium">Bedrooms Needed</span>
                    <span className="font-semibold text-indigo-300">{parsedIntent.bedrooms || 'Any'}</span>
                  </div>
                  <div className="bg-slate-950 p-2 rounded">
                    <span className="block text-slate-500 font-medium">Pet Friendly Request</span>
                    <span className="font-semibold text-indigo-300">{parsedIntent.petPreference ? 'Yes' : 'No'}</span>
                  </div>
                </div>

                <div className="bg-slate-950 p-2.5 rounded">
                  <span className="block text-slate-500 font-medium mb-1">Target Keywords & Amenities</span>
                  <div className="flex flex-wrap gap-1">
                    {parsedIntent.amenities.length > 0 ? (
                      parsedIntent.amenities.map(a => (
                        <span key={a} className="px-1.5 py-0.5 bg-indigo-900/40 text-indigo-300 rounded text-[10px]">
                          {a}
                        </span>
                      ))
                    ) : (
                      <span className="text-slate-500 italic">None identified</span>
                    )}
                  </div>
                </div>

                <div className="bg-slate-950 p-2.5 rounded">
                  <span className="block text-slate-500 font-medium mb-1">Mock Dense Vector Embedding Representation (8-dim excerpt)</span>
                  <div className="font-mono text-slate-400 overflow-x-auto whitespace-nowrap bg-slate-900 p-1.5 rounded">
                    [{parsedIntent.vectorRep.join(', ')}]
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Results Section */}
      {isSearching ? (
        <div className="flex flex-col items-center justify-center py-20">
          <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mb-4"></div>
          <p className="text-slate-400">AI search engine mapping queries to vector indexes...</p>
        </div>
      ) : hasSearched && results.length === 0 ? (
        <div className="text-center py-12 bg-slate-900/30 border border-slate-800 rounded-2xl max-w-4xl mx-auto">
          <AlertTriangle className="w-8 h-8 text-amber-500 mx-auto mb-3" />
          <p className="text-slate-300 font-medium">No direct matches found</p>
          <p className="text-slate-500 text-sm mt-1">Try broadening your sentence description or lower weights.</p>
        </div>
      ) : hasSearched ? (
        <div className="max-w-4xl mx-auto space-y-6">
          <div className="flex justify-between items-center px-1">
            <h2 className="text-lg font-semibold text-slate-200">
              Matched Properties ({results.length})
            </h2>
            <span className="text-xs text-slate-400">Sorted by AI Match score</span>
          </div>

          <div className="space-y-6">
            {results.map((item) => (
              <div
                key={item.id}
                className="bg-slate-900/80 border border-slate-800 hover:border-slate-700 rounded-2xl overflow-hidden shadow-lg transition-all flex flex-col md:flex-row animate-fade-in"
              >
                {/* Property Image */}
                <div className="md:w-1/3 relative h-48 md:h-auto min-h-[200px]">
                  <img
                    src={item.thumbnailUrl}
                    alt={item.title}
                    className="w-full h-full object-cover"
                  />
                  {/* Heart / Favorite Overlay */}
                  <button className="absolute top-3 right-3 p-2 bg-slate-950/60 hover:bg-slate-950/80 rounded-full text-slate-300 hover:text-rose-500 transition-colors">
                    <Heart className="w-4 h-4" />
                  </button>
                </div>

                {/* Property Details */}
                <div className="flex-1 p-6 flex flex-col justify-between">
                  <div>
                    {/* Header: Title and Match Score */}
                    <div className="flex justify-between items-start gap-4 mb-2">
                      <div>
                        <div className="flex items-center gap-1.5 text-xs text-slate-400 mb-1">
                          <MapPin className="w-3.5 h-3.5 text-indigo-400" />
                          <span>{item.locality}, {item.city}</span>
                        </div>
                        <h3 className="text-lg font-bold text-white leading-tight">
                          {item.title}
                        </h3>
                      </div>

                      {/* Match Score circular progress gauge */}
                      <div className="flex flex-col items-center">
                        <div className="relative w-14 h-14 flex items-center justify-center">
                          <svg className="w-full h-full transform -rotate-90" viewBox="0 0 36 36">
                            <path
                              className="text-slate-800"
                              strokeWidth="3"
                              stroke="currentColor"
                              fill="none"
                              d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                            />
                            <path
                              className="text-indigo-500"
                              strokeWidth="3.2"
                              strokeDasharray={`${item.matchScore}, 100`}
                              strokeLinecap="round"
                              stroke="currentColor"
                              fill="none"
                              d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                            />
                          </svg>
                          <span className="absolute text-[12px] font-extrabold text-white">
                            {item.matchScore}%
                          </span>
                        </div>
                        <span className="text-[10px] text-indigo-400 font-semibold tracking-wider uppercase mt-1">
                          Match
                        </span>
                      </div>
                    </div>

                    {/* Price and Key Stats */}
                    <div className="flex items-baseline gap-2 mb-4">
                      <span className="text-xl font-extrabold text-indigo-400">
                        ₹{item.price.toLocaleString()}
                      </span>
                      <span className="text-xs text-slate-400">/ month</span>
                      <span className="text-xs bg-slate-850 px-2 py-0.5 rounded text-slate-350 border border-slate-800 ml-2">
                        {item.bedrooms} BHK
                      </span>
                    </div>

                    {/* AI Match Insights */}
                    <div className="bg-slate-950/80 border border-slate-850 rounded-xl p-3.5 mb-4">
                      <div className="flex items-center space-x-1.5 text-xs text-indigo-400 font-semibold mb-2">
                        <Sparkles className="w-3.5 h-3.5" />
                        <span>AI Match Insights</span>
                      </div>
                      <div className="space-y-1 text-xs">
                        {item.insights.pros.map((pro, index) => (
                          <div key={index} className="flex items-start gap-1.5 text-emerald-400">
                            <CheckCircle2 className="w-3.5 h-3.5 mt-0.5 shrink-0" />
                            <span>{pro}</span>
                          </div>
                        ))}
                        {item.insights.cons.map((con, index) => (
                          <div key={index} className="flex items-start gap-1.5 text-slate-400">
                            <AlertTriangle className="w-3.5 h-3.5 text-amber-500 mt-0.5 shrink-0" />
                            <span>{con}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>

                  {/* Footer actions */}
                  <div className="flex items-center justify-between pt-2 border-t border-slate-800/60">
                    <div className="flex items-center gap-1">
                      {item.amenities.slice(0, 3).map((amenity) => (
                        <span
                          key={amenity}
                          className="px-2 py-0.5 rounded bg-slate-800 text-[10px] text-slate-300"
                        >
                          {amenity}
                        </span>
                      ))}
                      {item.amenities.length > 3 && (
                        <span className="text-[10px] text-slate-500 pl-1">
                          +{item.amenities.length - 3} more
                        </span>
                      )}
                    </div>
                    <button className="flex items-center space-x-1 bg-slate-850 hover:bg-slate-800 border border-slate-700 hover:border-slate-600 px-4 py-2 rounded-xl text-xs font-semibold text-slate-200 transition-all">
                      <span>Explore Tour & Details</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <div className="text-center py-20 max-w-xl mx-auto">
          <div className="w-16 h-16 bg-slate-900 border border-indigo-900/50 rounded-2xl flex items-center justify-center text-indigo-400 mx-auto mb-4 shadow-xl">
            <Brain className="w-8 h-8 animate-pulse" />
          </div>
          <p className="text-slate-300 font-semibold text-lg">Awaiting Your Input</p>
          <p className="text-slate-500 text-sm mt-1">
            Type your specific housing needs above, and click search to run our semantic AI match models.
          </p>
        </div>
      )}
    </div>
  );
}
