import React, { useEffect, useState } from 'react';
import { getTrustScoreExplanation, TrustExplanationDto } from '../services/trustService';
import { Shield, ShieldCheck, ShieldAlert, X, HelpCircle, Loader2 } from 'lucide-react';

interface TrustExplanationDialogProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function TrustExplanationDialog({ isOpen, onClose }: TrustExplanationDialogProps) {
  const [explanation, setExplanation] = useState<TrustExplanationDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      setLoading(true);
      setError(null);
      getTrustScoreExplanation()
        .then((data) => {
          setExplanation(data);
          setLoading(false);
        })
        .catch((err) => {
          console.error(err);
          setError('Failed to load trust score breakdown. Please try again.');
          setLoading(false);
        });
    }
  }, [isOpen]);

  // Handle escape key to close modal
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-labelledby="trust-dialog-title"
    >
      <div className="w-full max-w-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center space-x-2">
            <Shield className="w-6 h-6 text-indigo-500" />
            <h2 id="trust-dialog-title" className="text-xl font-bold font-outfit text-slate-800 dark:text-white">
              Trust Score Breakdown
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition"
            aria-label="Close dialog"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto max-h-[70vh]">
          {loading ? (
            <div className="flex flex-col items-center justify-center py-12 space-y-3">
              <Loader2 className="w-8 h-8 text-indigo-500 animate-spin" />
              <p className="text-sm text-slate-500">Calculating your trust score details...</p>
            </div>
          ) : error ? (
            <div className="text-center py-8">
              <p className="text-sm text-red-500 mb-4">{error}</p>
              <button
                onClick={onClose}
                className="px-4 py-2 bg-indigo-500 hover:bg-indigo-600 text-white rounded-lg transition text-sm font-medium"
              >
                Close
              </button>
            </div>
          ) : explanation ? (
            <div className="space-y-6">
              
              {/* Dial Chart */}
              <div className="flex flex-col items-center py-4 bg-slate-50 dark:bg-slate-950/40 rounded-xl border border-slate-100 dark:border-slate-900">
                <div className="relative flex items-center justify-center w-36 h-36">
                  {/* Outer Circular Gradient */}
                  <svg className="w-full h-full transform -rotate-90">
                    <circle
                      cx="72"
                      cy="72"
                      r="60"
                      className="text-slate-200 dark:text-slate-800"
                      strokeWidth="10"
                      stroke="currentColor"
                      fill="transparent"
                    />
                    <circle
                      cx="72"
                      cy="72"
                      r="60"
                      className="text-indigo-500 transition-all duration-1000"
                      strokeWidth="10"
                      strokeDasharray={376.8}
                      strokeDashoffset={376.8 - (376.8 * explanation.overallScore) / 100}
                      strokeLinecap="round"
                      stroke="currentColor"
                      fill="transparent"
                    />
                  </svg>
                  <div className="absolute flex flex-col items-center">
                    <span className="text-4xl font-extrabold font-outfit text-slate-800 dark:text-white">
                      {explanation.overallScore}
                    </span>
                    <span className="text-[10px] uppercase tracking-wider text-slate-400">Trust Rating</span>
                  </div>
                </div>

                <div className="mt-4 text-center">
                  <p className="text-[11px] text-slate-400 mt-0.5">
                    Calculated on: {new Date(explanation.calculatedAt).toLocaleString()}
                  </p>
                </div>
              </div>

              {/* Category Scores Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                
                {/* Identity Verification */}
                <div className="p-4 bg-slate-50 dark:bg-slate-950/40 border border-slate-100 dark:border-slate-800 rounded-xl space-y-2">
                  <div className="flex justify-between items-center">
                    <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">Identity Score</span>
                    <span className="text-sm font-bold text-indigo-500">{explanation.identityScore}/100</span>
                  </div>
                  <div className="w-full bg-slate-200 dark:bg-slate-800 rounded-full h-2">
                    <div 
                      className="bg-indigo-500 h-2 rounded-full transition-all duration-500" 
                      style={{ width: `${explanation.identityScore}%` }}
                    />
                  </div>
                </div>

                {/* Property Verification */}
                <div className="p-4 bg-slate-50 dark:bg-slate-950/40 border border-slate-100 dark:border-slate-800 rounded-xl space-y-2">
                  <div className="flex justify-between items-center">
                    <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">Property Verification</span>
                    <span className="text-sm font-bold text-emerald-500">{explanation.propertyScore}/100</span>
                  </div>
                  <div className="w-full bg-slate-200 dark:bg-slate-800 rounded-full h-2">
                    <div 
                      className="bg-emerald-500 h-2 rounded-full transition-all duration-500" 
                      style={{ width: `${explanation.propertyScore}%` }}
                    />
                  </div>
                </div>

                {/* Reviews & Feedback */}
                <div className="p-4 bg-slate-50 dark:bg-slate-950/40 border border-slate-100 dark:border-slate-800 rounded-xl space-y-2">
                  <div className="flex justify-between items-center">
                    <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">Reviews & Feedback</span>
                    <span className="text-sm font-bold text-blue-500">{explanation.reviewScore}/100</span>
                  </div>
                  <div className="w-full bg-slate-200 dark:bg-slate-800 rounded-full h-2">
                    <div 
                      className="bg-blue-500 h-2 rounded-full transition-all duration-500" 
                      style={{ width: `${explanation.reviewScore}%` }}
                    />
                  </div>
                </div>

                {/* Platform Activity */}
                <div className="p-4 bg-slate-50 dark:bg-slate-950/40 border border-slate-100 dark:border-slate-800 rounded-xl space-y-2">
                  <div className="flex justify-between items-center">
                    <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">Platform Activity</span>
                    <span className="text-sm font-bold text-violet-500">{explanation.activityScore}/100</span>
                  </div>
                  <div className="w-full bg-slate-200 dark:bg-slate-800 rounded-full h-2">
                    <div 
                      className="bg-violet-500 h-2 rounded-full transition-all duration-500" 
                      style={{ width: `${explanation.activityScore}%` }}
                    />
                  </div>
                </div>
              </div>

              {/* Fraud Penalty Banner if exists */}
              {explanation.fraudPenalty > 0 && (
                <div className="p-4 bg-rose-500/10 border border-rose-500/20 rounded-xl flex items-start space-x-2.5">
                  <ShieldAlert className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
                  <div>
                    <h4 className="text-sm font-bold text-rose-500">Active Fraud Penalty</h4>
                    <p className="text-xs text-rose-600 dark:text-rose-450 mt-0.5">
                      A penalty of -{explanation.fraudPenalty} points has been applied to this profile due to security system flags.
                    </p>
                  </div>
                </div>
              )}

              {/* Note */}
              <div className="p-4 bg-slate-50 dark:bg-slate-950/40 border border-slate-100 dark:border-slate-900 rounded-xl flex items-start space-x-2.5">
                <HelpCircle className="w-5 h-5 text-slate-400 shrink-0 mt-0.5" />
                <p className="text-xs text-slate-500 leading-normal">
                  Trust Scores are re-evaluated dynamically as profile verifications are completed, expired documents renew, or listings receive community feedback. Factors list only high-level status criteria and exclude internal proprietary security metrics.
                </p>
              </div>

            </div>
          ) : (
            <div className="text-center py-8">
              <p className="text-sm text-slate-500">No score details available for this account.</p>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 bg-slate-50 dark:bg-slate-950 border-t border-slate-100 dark:border-slate-800 flex justify-end">
          <button
            onClick={onClose}
            className="px-5 py-2 bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 font-semibold text-sm rounded-lg transition"
          >
            Done
          </button>
        </div>

      </div>
    </div>
  );
}
