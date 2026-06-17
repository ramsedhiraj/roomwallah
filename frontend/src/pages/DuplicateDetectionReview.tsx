import React, { useState } from 'react';
import { AlertTriangle, Trash2, ShieldAlert, Sparkles, Check, ChevronRight, CheckCircle2, UserCheck, AlertOctagon } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { apiClient } from '../services/api';

interface DuplicateCandidate {
  id: string;
  title: string;
  ownerName: string;
  price: number;
  postedTime: string;
  trustScore: number;
  thumbnailUrl: string;
}

interface DuplicateCluster {
  id: string;
  similarityScore: number;
  locality: string;
  city: string;
  candidates: [DuplicateCandidate, DuplicateCandidate];
  matchInsights: string[];
}

const INITIAL_CLUSTERS: DuplicateCluster[] = [
  {
    id: 'cluster-1',
    similarityScore: 97.8,
    locality: 'Sector 62',
    city: 'Noida',
    candidates: [
      {
        id: 'prop-101',
        title: 'Spacious 2-BHK flat Noida Sector 62',
        ownerName: 'Ramesh Sharma',
        price: 18000,
        postedTime: '2 days ago',
        trustScore: 88,
        thumbnailUrl: 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=300&q=80',
      },
      {
        id: 'prop-102',
        title: '2 BHK Noida Sec 62 Gym Balcony near corporate hubs',
        ownerName: 'Suresh Kumar',
        price: 18500,
        postedTime: '3 hours ago',
        trustScore: 42,
        thumbnailUrl: 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=300&q=80',
      },
    ],
    matchInsights: ['Titles match 94%', 'Photos match 98% (structural histogram similarity)', 'Descriptions overlap by 91%'],
  },
  {
    id: 'cluster-2',
    similarityScore: 92.4,
    locality: 'Indiranagar',
    city: 'Bangalore',
    candidates: [
      {
        id: 'prop-201',
        title: 'Cozy 1 BHK Indiranagar',
        ownerName: 'Anjali Gupta',
        price: 15000,
        postedTime: '1 week ago',
        trustScore: 95,
        thumbnailUrl: 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=300&q=80',
      },
      {
        id: 'prop-202',
        title: 'Furnished 1 BHK metro station Indiranagar',
        ownerName: 'Vikram Mehta',
        price: 14800,
        postedTime: '1 day ago',
        trustScore: 80,
        thumbnailUrl: 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=300&q=80',
      },
    ],
    matchInsights: ['Image feature vector cosine similarity: 93%', 'Address fields correspond exactly'],
  },
];

export default function DuplicateDetectionReview() {
  const [clusters, setClusters] = useState<DuplicateCluster[]>(INITIAL_CLUSTERS);
  const [activeClusterId, setActiveClusterId] = useState<string>('cluster-1');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const activeCluster = clusters.find(c => c.id === activeClusterId) || clusters[0];

  const triggerToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const handleAction = async (actionType: 'merge' | 'dismiss' | 'flag', clusterId: string) => {
    // Determine details for logs
    let feedback = '';
    if (actionType === 'merge') {
      feedback = 'Suspected duplicate resolved: Listing merged successfully.';
    } else if (actionType === 'dismiss') {
      feedback = 'Ignored: Listings whitelisted and marked as separate entities.';
    } else {
      feedback = 'Flagged: Newer listing suspended due to suspected fraud matching.';
    }

    triggerToast(feedback);

    // Fade active item and adjust current selected index
    setClusters(prev => prev.filter(c => c.id !== clusterId));
    
    // Choose next cluster
    const remaining = clusters.filter(c => c.id !== clusterId);
    if (remaining.length > 0) {
      setActiveClusterId(remaining[0].id);
    }

    try {
      await apiClient.post(`/admin/duplicates/${clusterId}/resolve`, {
        action: actionType,
      });
    } catch (err) {
      console.warn('API post failed, resolution performed locally.');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 text-slate-100 relative">
      {/* Toast Notification */}
      <AnimatePresence>
        {toastMessage && (
          <motion.div
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="fixed top-24 left-1/2 -translate-x-1/2 z-50 bg-indigo-650 text-white font-semibold text-xs px-5 py-3 rounded-xl border border-indigo-500/25 shadow-2xl flex items-center space-x-2"
          >
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
            <span>{toastMessage}</span>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Page Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b border-slate-800 pb-6 mb-8">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
            AI Duplicate Detection Review
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Review listings flagged by the platform's vision & text matching models. Take actions to clean spam and prevent bait listings.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <span className="flex h-2 w-2 rounded-full bg-indigo-500 animate-pulse"></span>
          <span className="text-xs text-indigo-400 font-semibold bg-indigo-500/10 px-3 py-1 rounded-full border border-indigo-500/20">
            Duplicate Detection Queue Active
          </span>
        </div>
      </div>

      {clusters.length === 0 ? (
        <div className="text-center py-20 bg-slate-900/40 border border-slate-850 rounded-2xl max-w-2xl mx-auto">
          <CheckCircle2 className="w-12 h-12 text-emerald-500 mx-auto mb-4" />
          <h2 className="text-lg font-bold text-white">All Clear! No Flagged Duplicates</h2>
          <p className="text-slate-500 text-sm mt-1">Suspected cluster queue is empty. AI models will continue real-time scanning.</p>
          <button
            onClick={() => setClusters(INITIAL_CLUSTERS)}
            className="mt-4 text-xs bg-indigo-600 hover:bg-indigo-500 text-white font-semibold px-4 py-2 rounded-lg transition-all"
          >
            Reload Mock Clusters
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
          {/* Left sidebar clusters selector */}
          <div className="lg:col-span-1 space-y-4">
            <h2 className="text-xs font-bold text-slate-400 uppercase tracking-wider px-1">Flagged Groups ({clusters.length})</h2>
            <div className="space-y-2">
              {clusters.map((item) => (
                <button
                  key={item.id}
                  onClick={() => setActiveClusterId(item.id)}
                  className={`w-full text-left p-4 rounded-xl border transition-all ${
                    activeClusterId === item.id
                      ? 'bg-slate-900 border-indigo-500/80 shadow'
                      : 'bg-slate-900/40 border-slate-850 hover:bg-slate-900 hover:border-slate-800'
                  }`}
                  data-testid="duplicate-cluster-item"
                >
                  <div className="flex justify-between items-center mb-1">
                    <span className="text-xs font-bold text-white">Group #{item.id.split('-')[1]}</span>
                    <span className="text-[10px] text-amber-400 font-bold bg-amber-500/10 px-2 py-0.5 rounded border border-amber-500/20">
                      {item.similarityScore}% Similar
                    </span>
                  </div>
                  <span className="text-[10px] text-slate-400 block">{item.locality}, {item.city}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Right side-by-side candidates comparator */}
          <div className="lg:col-span-3 space-y-6">
            {/* AI Insights Bar */}
            <div className="bg-indigo-950/20 border border-indigo-900/40 rounded-xl p-4 flex items-center space-x-3 text-xs text-indigo-300">
              <Sparkles className="w-5 h-5 text-indigo-400 shrink-0" />
              <div>
                <span className="font-bold block">AI Engine Verdict</span>
                <p className="mt-0.5 text-slate-450">{activeCluster.matchInsights.join(' | ')}</p>
              </div>
            </div>

            {/* Candidates comparator */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {activeCluster.candidates.map((candidate, idx) => (
                <div
                  key={candidate.id}
                  className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-lg p-5 flex flex-col justify-between"
                >
                  <div>
                    {/* Badge Candidate */}
                    <div className="flex justify-between items-center mb-3">
                      <span className="text-[10px] font-bold text-slate-400 uppercase">
                        Candidate {idx === 0 ? 'A (Original / Older)' : 'B (Duplicate / Newer)'}
                      </span>
                      <span className="text-[10px] text-slate-400">ID: {candidate.id}</span>
                    </div>

                    <img
                      src={candidate.thumbnailUrl}
                      alt={candidate.title}
                      className="w-full h-36 object-cover rounded-xl mb-4 border border-slate-850"
                    />

                    <h3 className="text-sm font-bold text-white mb-2 leading-snug">{candidate.title}</h3>

                    <div className="space-y-1.5 text-xs text-slate-400 border-t border-slate-800/80 pt-3">
                      <div className="flex justify-between">
                        <span>Owner:</span>
                        <span className="font-semibold text-slate-200">{candidate.ownerName}</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Rent:</span>
                        <span className="font-mono font-semibold text-slate-200">₹{candidate.price.toLocaleString()}</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Posted:</span>
                        <span>{candidate.postedTime}</span>
                      </div>
                      <div className="flex justify-between items-center">
                        <span>Trust Index:</span>
                        <span className={`font-bold font-mono ${
                          candidate.trustScore >= 80 ? 'text-emerald-400' : 'text-amber-450'
                        }`}>{candidate.trustScore}/100</span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Admin Resolution Toolbar */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 flex flex-col md:flex-row justify-between items-center gap-4">
              <div>
                <h3 className="text-xs font-bold text-slate-400 uppercase mb-0.5">Admin Action Pipeline</h3>
                <p className="text-xs text-slate-500">Apply platform moderation rules to resolve this duplication case.</p>
              </div>

              <div className="flex items-center gap-2 w-full md:w-auto">
                <button
                  onClick={() => handleAction('dismiss', activeCluster.id)}
                  className="flex-1 md:flex-none flex items-center justify-center space-x-1 px-4 py-2.5 rounded-lg border border-slate-800 hover:border-slate-700 bg-slate-950 text-slate-400 hover:text-white transition-all text-xs font-semibold"
                  data-testid="duplicate-dismiss-btn"
                >
                  <Check className="w-3.5 h-3.5" />
                  <span>Dismiss / Mark Unique</span>
                </button>

                <button
                  onClick={() => handleAction('flag', activeCluster.id)}
                  className="flex-1 md:flex-none flex items-center justify-center space-x-1 px-4 py-2.5 rounded-lg border border-rose-900/30 hover:border-rose-900/50 bg-rose-950/20 text-rose-400 hover:text-rose-300 transition-all text-xs font-semibold"
                  data-testid="duplicate-flag-fraud-btn"
                >
                  <AlertTriangle className="w-3.5 h-3.5" />
                  <span>Flag Candidate B</span>
                </button>

                <button
                  onClick={() => handleAction('merge', activeCluster.id)}
                  className="flex-1 md:flex-none flex items-center justify-center space-x-1 px-4 py-2.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white transition-all text-xs font-semibold"
                  data-testid="duplicate-merge-btn"
                >
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>Merge Listings</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
