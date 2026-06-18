import React, { useState } from 'react';
import { AlertTriangle, Eye, Users, Percent, Sparkles, CheckCircle2, ChevronRight, X } from 'lucide-react';
import { apiClient } from '../services/api';

interface ListingHealth {
  id: string;
  title: string;
  locality: string;
  healthScore: number;
  viewsThisWeek: number;
  leadsThisWeek: number;
  alerts: string[];
  checklist: { id: string; text: string; points: number; solved: boolean }[];
}

const INITIAL_HEALTH_LISTINGS: ListingHealth[] = [
  {
    id: 'lh-1',
    title: 'Cozy 1-BHK Apartment near Tech Hub',
    locality: 'Indiranagar',
    healthScore: 78,
    viewsThisWeek: 350,
    leadsThisWeek: 8,
    alerts: [
      'Search ranking dropped 15% due to outdated availability calendar.',
    ],
    checklist: [
      { id: '1', text: 'Add 3D Virtual Tour link', points: 15, solved: false },
      { id: '2', text: 'Write detailed neighborhood description', points: 10, solved: false },
      { id: '3', text: 'Update calendar availability status', points: 7, solved: false },
    ],
  },
  {
    id: 'lh-2',
    title: 'Spacious 2-BHK Flat with Balcony & Gym',
    locality: 'Sector 62 Noida',
    healthScore: 92,
    viewsThisWeek: 620,
    leadsThisWeek: 21,
    alerts: [],
    checklist: [
      { id: '4', text: 'Complete owner profile verification', points: 8, solved: true },
    ],
  },
  {
    id: 'lh-3',
    title: 'Luxury 3-BHK Villa with Private Garden',
    locality: 'Bandra Mumbai',
    healthScore: 54,
    viewsThisWeek: 120,
    leadsThisWeek: 2,
    alerts: [
      'Traffic Warning: Listing views dropped 40% this week. Consider price review or adding high-resolution photos.',
    ],
    checklist: [
      { id: '5', text: 'Upload high-resolution photos', points: 20, solved: false },
      { id: '6', text: 'Specify security deposit breakdown', points: 10, solved: false },
      { id: '7', text: 'Add house policy tags (e.g. pets, smoking)', points: 10, solved: false },
      { id: '8', text: 'Complete owner identity verification', points: 6, solved: false },
    ],
  },
];

export default function ListingHealthDashboard() {
  const [listings, setListings] = useState<ListingHealth[]>(INITIAL_HEALTH_LISTINGS);
  const [activeListingId, setActiveListingId] = useState<string>('lh-1');
  const [isWizardOpen, setIsWizardOpen] = useState(false);
  const [wizardTask, setWizardTask] = useState<{ id: string; text: string; points: number } | null>(null);
  const [wizardValue, setWizardValue] = useState('');

  const activeListing = listings.find(l => l.id === activeListingId) || listings[0];

  const handleResolveChecklistItem = (listingId: string, itemId: string) => {
    const itemToSolve = activeListing.checklist.find(c => c.id === itemId);
    if (!itemToSolve || itemToSolve.solved) return;

    setWizardTask(itemToSolve);
    setWizardValue('');
    setIsWizardOpen(true);
  };

  const handleSubmitWizardSolution = async () => {
    if (!wizardTask) return;

    // Simulate solving and updating score
    setListings(prev => prev.map(listing => {
      if (listing.id === activeListingId) {
        const updatedChecklist = listing.checklist.map(item =>
          item.id === wizardTask.id ? { ...item, solved: true } : item
        );
        const newScore = Math.min(100, listing.healthScore + wizardTask.points);
        return {
          ...listing,
          healthScore: newScore,
          checklist: updatedChecklist,
        };
      }
      return listing;
    }));

    try {
      await apiClient.post(`/listings/${activeListingId}/resolve-health`, {
        taskId: wizardTask.id,
        content: wizardValue,
      });
    } catch (err) {
      console.warn('API update failed. Listing health score simulated locally.');
    }

    setIsWizardOpen(false);
    setWizardTask(null);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 text-slate-100">
      {/* Header */}
      <div className="border-b border-slate-800 pb-6 mb-8">
        <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
          Listing Health & Completeness Dashboard
        </h1>
        <p className="text-slate-400 text-sm mt-1">
          Monitor room listing optimization checklist scores, traffic views, and get instant recommendations to improve discovery rankings.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        {/* Listings Sidebar Selector */}
        <div className="lg:col-span-1 space-y-4">
          <h2 className="text-xs font-bold text-slate-400 uppercase tracking-wider px-1">Your Properties</h2>
          <div className="space-y-2">
            {listings.map((item) => (
              <button
                key={item.id}
                onClick={() => setActiveListingId(item.id)}
                className={`w-full text-left p-4 rounded-xl border transition-all ${
                  activeListingId === item.id
                    ? 'bg-slate-900 border-indigo-500/80 shadow-md shadow-indigo-950/20'
                    : 'bg-slate-900/40 border-slate-850 hover:bg-slate-900 hover:border-slate-800'
                }`}
              >
                <h3 className="text-xs font-bold text-slate-200 truncate">{item.title}</h3>
                <span className="text-[10px] text-slate-450 block mb-2">{item.locality}</span>

                <div className="flex justify-between items-center text-xs">
                  <span className="text-slate-400">Health Index</span>
                  <span className={`px-2 py-0.5 rounded font-extrabold ${
                    item.healthScore >= 85 ? 'text-emerald-450' : item.healthScore >= 60 ? 'text-amber-450' : 'text-rose-450'
                  }`}>
                    {item.healthScore}/100
                  </span>
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* Selected Listing Health Details */}
        <div className="lg:col-span-3 space-y-6">
          {/* Main overview card */}
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
            <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 mb-6">
              <div>
                <span className="text-xs text-indigo-400 font-semibold uppercase tracking-wider block mb-1">Current Focus</span>
                <h2 className="text-xl font-bold text-white leading-snug">{activeListing.title}</h2>
                <span className="text-xs text-slate-450">{activeListing.locality}</span>
              </div>

              {/* Big Score circle */}
              <div className="flex items-center gap-3 bg-slate-950 px-5 py-3 rounded-xl border border-slate-850 shrink-0">
                <div className="w-12 h-12 rounded-full border-4 border-indigo-500/20 flex items-center justify-center font-extrabold text-white text-sm font-mono shadow shadow-indigo-900">
                  {activeListing.healthScore}
                </div>
                <div>
                  <span className="text-[10px] text-slate-400 font-semibold uppercase block">Health Grade</span>
                  <span className="text-xs font-bold text-slate-250">
                    {activeListing.healthScore >= 85 ? 'Optimized (Good)' : activeListing.healthScore >= 60 ? 'Fair (Needs Work)' : 'Critical Alert'}
                  </span>
                </div>
              </div>
            </div>

            {/* Performance Stats row */}
            <div className="grid grid-cols-3 gap-4 border-t border-b border-slate-800/80 py-4 my-6">
              <div className="text-center md:text-left">
                <span className="text-[10px] text-slate-500 font-semibold uppercase flex justify-center md:justify-start items-center gap-1">
                  <Eye className="w-3.5 h-3.5 text-indigo-400" />
                  <span>Views (Week)</span>
                </span>
                <p className="text-xl font-extrabold text-white mt-1">{activeListing.viewsThisWeek}</p>
              </div>

              <div className="text-center md:text-left">
                <span className="text-[10px] text-slate-500 font-semibold uppercase flex justify-center md:justify-start items-center gap-1">
                  <Users className="w-3.5 h-3.5 text-indigo-400" />
                  <span>Leads (Week)</span>
                </span>
                <p className="text-xl font-extrabold text-white mt-1">{activeListing.leadsThisWeek}</p>
              </div>

              <div className="text-center md:text-left">
                <span className="text-[10px] text-slate-500 font-semibold uppercase flex justify-center md:justify-start items-center gap-1">
                  <Percent className="w-3.5 h-3.5 text-indigo-400" />
                  <span>Conv. Ratio</span>
                </span>
                <p className="text-xl font-extrabold text-white mt-1">
                  {activeListing.viewsThisWeek > 0
                    ? ((activeListing.leadsThisWeek / activeListing.viewsThisWeek) * 100).toFixed(1)
                    : '0.0'}%
                </p>
              </div>
            </div>

            {/* Alerts section */}
            {activeListing.alerts.length > 0 && (
              <div className="space-y-2 mb-6">
                {activeListing.alerts.map((alert, idx) => (
                  <div
                    key={idx}
                    className="p-4 bg-amber-500/10 border border-amber-500/25 rounded-xl text-xs text-amber-250 flex items-start gap-3"
                    data-testid="health-alert-box"
                  >
                    <AlertTriangle className="w-4 h-4 shrink-0 text-amber-500 mt-0.5" />
                    <p className="leading-relaxed">{alert}</p>
                  </div>
                ))}
              </div>
            )}

            {/* Checklist items */}
            <div>
              <h3 className="text-sm font-bold text-white mb-3 flex items-center gap-1.5">
                <Sparkles className="w-4 h-4 text-indigo-400" />
                <span>Optimization Checklist</span>
              </h3>

              <div className="space-y-3">
                {activeListing.checklist.map((item) => (
                  <div
                    key={item.id}
                    className={`p-4 rounded-xl border flex justify-between items-center transition-colors ${
                      item.solved
                        ? 'bg-slate-950/40 border-slate-900 text-slate-400'
                        : 'bg-slate-950 border-slate-850 hover:border-slate-800'
                    }`}
                  >
                    <div className="flex items-center space-x-3">
                      {item.solved ? (
                        <CheckCircle2 className="w-5 h-5 text-emerald-500 shrink-0" />
                      ) : (
                        <div className="w-5 h-5 rounded-full border-2 border-slate-700 shrink-0" />
                      )}
                      <div>
                        <p className={`text-xs font-semibold ${item.solved ? 'line-through text-slate-500' : 'text-slate-200'}`}>
                          {item.text}
                        </p>
                        <span className="text-[10px] text-indigo-400 font-bold block mt-0.5">+{item.points} Points health boost</span>
                      </div>
                    </div>

                    {!item.solved && (
                      <button
                        onClick={() => handleResolveChecklistItem(activeListing.id, item.id)}
                        className="flex items-center space-x-1 text-xs text-indigo-400 hover:text-white bg-indigo-500/5 hover:bg-indigo-500/10 px-3.5 py-1.5 rounded-lg border border-indigo-500/15 hover:border-indigo-500/30 transition-all font-semibold"
                      >
                        <span>Resolve</span>
                        <ChevronRight className="w-3.5 h-3.5" />
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Resolve Task Wizard Dialog Mock */}
      {isWizardOpen && wizardTask && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md overflow-hidden shadow-2xl animate-scale-in">
            {/* Header */}
            <div className="bg-slate-950 px-5 py-4 border-b border-slate-850 flex justify-between items-center">
              <h3 className="text-sm font-bold text-white flex items-center gap-1.5">
                <Sparkles className="w-4 h-4 text-indigo-400 animate-pulse" />
                <span>Health Optimization Wizard</span>
              </h3>
              <button
                onClick={() => {
                  setIsWizardOpen(false);
                  setWizardTask(null);
                }}
                className="p-1 bg-slate-900 hover:bg-slate-800 rounded-lg text-slate-400 hover:text-white transition-colors"
              >
                <X className="w-4.5 h-4.5" />
              </button>
            </div>

            {/* Content Body */}
            <div className="p-5 space-y-4">
              <div>
                <span className="text-[10px] text-slate-400 uppercase font-semibold">Active Resolution Task</span>
                <p className="text-sm text-slate-100 font-bold mt-1 leading-snug">{wizardTask.text}</p>
                <span className="text-[10px] text-indigo-400 font-bold block mt-0.5">Completion will boost listing health index by {wizardTask.points} points.</span>
              </div>

              <div>
                <label className="block text-[10px] text-slate-400 uppercase font-semibold mb-1.5">Submit Resolution Details</label>
                <textarea
                  rows={4}
                  value={wizardValue}
                  onChange={(e) => setWizardValue(e.target.value)}
                  placeholder="Enter details here... e.g. Link to 3D tour, neighborhood description copy, or tags."
                  className="w-full bg-slate-950 border border-slate-850 rounded-lg p-3 text-xs text-slate-200 placeholder-slate-650 focus:outline-none focus:border-indigo-500"
                />
              </div>
            </div>

            {/* Footer buttons */}
            <div className="px-5 py-3.5 bg-slate-950/80 border-t border-slate-850 flex justify-end gap-2">
              <button
                onClick={() => {
                  setIsWizardOpen(false);
                  setWizardTask(null);
                }}
                className="px-4 py-2 bg-slate-900 hover:bg-slate-850 text-xs font-semibold rounded-lg text-slate-400 hover:text-white transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleSubmitWizardSolution}
                disabled={!wizardValue.trim()}
                className="bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold px-4 py-2 rounded-lg transition-colors disabled:opacity-50"
              >
                Complete Optimization
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
