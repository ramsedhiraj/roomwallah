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
                      strokeDashoffset={376.8 - (376.8 * explanation.currentScore) / 100}
                      strokeLinecap="round"
                      stroke="currentColor"
                      fill="transparent"
                    />
                  </svg>
                  <div className="absolute flex flex-col items-center">
                    <span className="text-4xl font-extrabold font-outfit text-slate-800 dark:text-white">
                      {explanation.currentScore}
                    </span>
                    <span className="text-[10px] uppercase tracking-wider text-slate-400">Trust Rating</span>
                  </div>
                </div>

                <div className="mt-4 text-center">
                  <p className="text-xs text-slate-400">
                    Algorithm Version: {explanation.algorithmVersion} • Rules: {explanation.ruleVersion}
                  </p>
                  <p className="text-[11px] text-slate-400 mt-0.5">
                    Calculated on: {new Date(explanation.calculatedAt).toLocaleString()}
                  </p>
                </div>
              </div>

              {/* Factors Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                
                {/* Positive Factors */}
                <div className="space-y-3">
                  <h3 className="text-sm font-bold uppercase tracking-wider text-emerald-600 dark:text-emerald-400 flex items-center space-x-1.5">
                    <ShieldCheck className="w-4 h-4" />
                    <span>Positive Factors</span>
                  </h3>
                  <div className="space-y-2">
                    {explanation.positiveFactors && explanation.positiveFactors.length > 0 ? (
                      explanation.positiveFactors.map((factor, idx) => (
                        <div
                          key={idx}
                          className="p-3 bg-emerald-50/50 dark:bg-emerald-950/20 border border-emerald-100/30 dark:border-emerald-900/30 rounded-lg flex items-start justify-between"
                        >
                          <div>
                            <p className="text-sm font-semibold text-slate-700 dark:text-slate-200">
                              {factor.name}
                            </p>
                            <p className="text-xs text-slate-500 mt-0.5">{factor.description}</p>
                          </div>
                          <span className="text-xs font-bold text-emerald-600 dark:text-emerald-400 whitespace-nowrap ml-2">
                            +{factor.scoreImpact}
                          </span>
                        </div>
                      ))
                    ) : (
                      <p className="text-xs text-slate-400 dark:text-slate-500 italic">
                        No positive trust factors recorded yet. Submit identity verification to begin.
                      </p>
                    )}
                  </div>
                </div>

                {/* Negative Factors */}
                <div className="space-y-3">
                  <h3 className="text-sm font-bold uppercase tracking-wider text-rose-600 dark:text-rose-400 flex items-center space-x-1.5">
                    <ShieldAlert className="w-4 h-4" />
                    <span>Negative Factors</span>
                  </h3>
                  <div className="space-y-2">
                    {explanation.negativeFactors && explanation.negativeFactors.length > 0 ? (
                      explanation.negativeFactors.map((factor, idx) => (
                        <div
                          key={idx}
                          className="p-3 bg-rose-50/50 dark:bg-rose-950/20 border border-rose-100/30 dark:border-rose-900/30 rounded-lg flex items-start justify-between"
                        >
                          <div>
                            <p className="text-sm font-semibold text-slate-700 dark:text-slate-200">
                              {factor.name}
                            </p>
                            <p className="text-xs text-slate-500 mt-0.5">{factor.description}</p>
                          </div>
                          <span className="text-xs font-bold text-rose-600 dark:text-rose-400 whitespace-nowrap ml-2">
                            -{factor.scoreImpact}
                          </span>
                        </div>
                      ))
                    ) : (
                      <div className="p-3 bg-slate-50 dark:bg-slate-950/40 border border-slate-100 dark:border-slate-900 rounded-lg">
                        <p className="text-xs text-slate-500 dark:text-slate-400 italic">
                          No negative flags. Excellent profile health!
                        </p>
                      </div>
                    )}
                  </div>
                </div>

              </div>

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
