import React, { useState, useEffect } from 'react';
import { Award, Zap } from 'lucide-react';
import { apiClient } from '../services/api';

export default function TrustScoreConsole() {
  const [score, setScore] = useState<any>({
    userId: 'user-123',
    currentScore: 82,
    scoreVersion: 3,
    overallScore: 82,
    identityScore: 90,
    propertyScore: 85,
    reviewScore: 78,
    activityScore: 80,
    fraudPenalty: 0,
    explanationJson: '{"kyc": "PASSED", "reviews": "EXCELLENT", "cancellations": "NONE"}'
  });

  const [loading, setLoading] = useState(false);
  const [overrideValue, setOverrideValue] = useState(85);
  const [overrideReason, setOverrideReason] = useState('');

  const fetchScore = async () => {
    try {
      setLoading(true);
      const res = await apiClient.get('/admin/trust/score/user-123');
      if (res.data && res.data.data) {
        setScore(res.data.data);
      }
    } catch (e) {
      console.warn("Failed fetching trust score, using defaults");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchScore();
  }, []);

  const handleOverride = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await apiClient.post('/admin/trust/score/override', {
        userId: 'user-123',
        newScore: overrideValue,
        reason: overrideReason
      });
      setOverrideReason('');
      fetchScore();
    } catch (err) {
      setScore({ ...score, currentScore: overrideValue });
      setOverrideReason('');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 text-slate-100">
      <div className="border-b border-slate-800 pb-6 mb-8 flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
            Trust Score Insights & Audit
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Check dynamic trust score factors (KYC status, bookings completion, payment histories, and fraud checks).
          </p>
        </div>
        <Award className="w-8 h-8 text-indigo-400 animate-pulse" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 flex flex-col md:flex-row justify-between items-center gap-6">
            <div className="text-center md:text-left space-y-1">
              <h3 className="text-lg font-bold text-white">Current Tenant Trust Score</h3>
              <p className="text-xs text-slate-400">Recalculated dynamically via system events.</p>
              <div className="flex items-center gap-2 mt-4 justify-center md:justify-start">
                <span className="text-5xl font-extrabold text-indigo-400">{score.currentScore}</span>
                <span className="text-xs text-slate-500 font-bold uppercase tracking-wider">/ 100 max</span>
              </div>
            </div>

            <div className="bg-slate-950/60 border border-slate-850 p-4 rounded-xl flex items-center gap-3 w-full md:w-auto">
              <Zap className="w-5 h-5 text-indigo-400" />
              <div className="text-xs">
                <p className="text-slate-300 font-semibold">Verification Badging</p>
                <p className="text-slate-500 mt-0.5">Confidence metric score limits active.</p>
              </div>
            </div>
          </div>

          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
            <h3 className="text-base font-bold text-white mb-4">Calculation Metrics Breakdown</h3>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
              <div className="bg-slate-950 p-4 rounded-xl text-center">
                <span className="text-[10px] text-slate-500 uppercase font-semibold">Identity Verified</span>
                <p className="text-lg font-bold text-slate-200 mt-1">{score.identityScore}%</p>
              </div>
              <div className="bg-slate-950 p-4 rounded-xl text-center">
                <span className="text-[10px] text-slate-500 uppercase font-semibold">Listing Accuracy</span>
                <p className="text-lg font-bold text-slate-200 mt-1">{score.propertyScore}%</p>
              </div>
              <div className="bg-slate-950 p-4 rounded-xl text-center">
                <span className="text-[10px] text-slate-500 uppercase font-semibold">Reviews Quality</span>
                <p className="text-lg font-bold text-slate-200 mt-1">{score.reviewScore}%</p>
              </div>
              <div className="bg-slate-950 p-4 rounded-xl text-center">
                <span className="text-[10px] text-slate-500 uppercase font-semibold">Booking Completion</span>
                <p className="text-lg font-bold text-slate-200 mt-1">{score.activityScore}%</p>
              </div>
              <div className="bg-slate-950 p-4 rounded-xl text-center">
                <span className="text-[10px] text-slate-500 uppercase font-semibold">Fraud Incidents</span>
                <p className="text-lg font-bold text-rose-400 mt-1">-{score.fraudPenalty}</p>
              </div>
            </div>
          </div>
        </div>

        <div className="lg:col-span-1">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
            <h3 className="text-base font-bold text-white mb-4">Admin Score Override</h3>
            <form onSubmit={handleOverride} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-400 uppercase mb-1.5">Override Target Score: {overrideValue}</label>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={overrideValue}
                  onChange={(e) => setOverrideValue(Number(e.target.value))}
                  className="w-full h-1 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-400 uppercase mb-1.5">Justification Reason</label>
                <textarea
                  required
                  placeholder="e.g. Verified additional government credentials manually"
                  value={overrideReason}
                  onChange={(e) => setOverrideReason(e.target.value)}
                  className="w-full h-20 bg-slate-950 border border-slate-850 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
                />
              </div>

              <button
                type="submit"
                className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 text-xs font-bold text-white rounded-xl shadow-md transition-colors"
              >
                Apply Score Override
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
