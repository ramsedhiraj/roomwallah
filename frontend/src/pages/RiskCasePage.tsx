import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  ShieldAlert, User, ShieldCheck, Check, Trash2, ArrowLeft, 
  MapPin, ShieldCheck as VerifiedIcon, ShieldAlert as SuspiciousIcon, 
  Lock, Key, AlertTriangle, AlertCircle, Terminal, HelpCircle 
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

interface ForensicSignal {
  name: string;
  evaluated: string;
  riskWeight: number;
  status: 'CLEAN' | 'SUSPICIOUS' | 'CRITICAL';
}

interface CaseDetails {
  id: string;
  userId: string;
  userName: string;
  userEmail: string;
  userPhone: string;
  accountAge: string;
  paymentId: string;
  bookingId: string;
  amount: number;
  currency: string;
  gateway: string;
  riskScore: number;
  status: 'UNDER_REVIEW' | 'RESOLVED_CLEAN' | 'LOCKED_BLOCKED';
  createdAt: string;
  signals: ForensicSignal[];
  rulesetVersion: string;
}

const mockCases: Record<string, CaseDetails> = {
  'RC-1092': {
    id: 'RC-1092',
    userId: 'usr-9281',
    userName: 'Rajesh Kumar',
    userEmail: 'rajesh.kumar@example.com',
    userPhone: '+91 98765 43210',
    accountAge: '14 months',
    paymentId: 'PMT-94812048',
    bookingId: 'BKG-5219',
    amount: 45000,
    currency: 'INR',
    gateway: 'Razorpay',
    riskScore: 82,
    status: 'UNDER_REVIEW',
    createdAt: '2026-06-15T18:32:00Z',
    signals: [
      { name: 'Geo-IP Address Origin', evaluated: 'IP: 198.51.100.42 (Frankfurt, Germany)', riskWeight: 40, status: 'CRITICAL' },
      { name: 'Billing Mismatch Offset', evaluated: 'Billing Address (Mumbai, India) != IP origin country', riskWeight: 20, status: 'SUSPICIOUS' },
      { name: 'VPN / Proxy Node Detection', evaluated: 'Host: Frankfurt Server Farms (DigitalOcean node)', riskWeight: 22, status: 'CRITICAL' },
      { name: 'Card BIN Country Matching', evaluated: 'Issued in India (Matches Billing)', riskWeight: 0, status: 'CLEAN' },
    ],
    rulesetVersion: 'v1.4.2',
  },
  'RC-1093': {
    id: 'RC-1093',
    userId: 'usr-4412',
    userName: 'Priyah Sharma',
    userEmail: 'priyah.s@example.com',
    userPhone: '+91 88888 77777',
    accountAge: '2 days',
    paymentId: 'PMT-0129482',
    bookingId: 'BKG-4412',
    amount: 28000,
    currency: 'INR',
    gateway: 'Stripe',
    riskScore: 71,
    status: 'UNDER_REVIEW',
    createdAt: '2026-06-15T17:15:00Z',
    signals: [
      { name: 'Card Fingerprint Match', evaluated: 'Card hash md5_981a... matches 3 other tenant accounts', riskWeight: 50, status: 'CRITICAL' },
      { name: 'Velocity Account Rate', evaluated: '1 checkout attempt in 15 minutes', riskWeight: 0, status: 'CLEAN' },
      { name: 'Device Canvas Footprint', evaluated: 'User-agent string identical to verified profiles', riskWeight: 10, status: 'SUSPICIOUS' },
      { name: 'Geo-IP Address Match', evaluated: 'Access IP origin matching card origin BIN (IN)', riskWeight: 11, status: 'CLEAN' },
    ],
    rulesetVersion: 'v1.4.2',
  },
};

export default function RiskCasePage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [caseItem, setCaseItem] = useState<CaseDetails | null>(() => {
    return mockCases[id || ''] || null;
  });
  const [showConfirmLock, setShowConfirmLock] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  if (!caseItem) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-12 text-center text-slate-100">
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 mb-6 text-xs">
          Risk Case ID: "{id}" not found.
        </div>
        <button
          onClick={() => navigate('/admin/fraud')}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-200 text-xs"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Dashboard</span>
        </button>
      </div>
    );
  }

  const triggerToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const handleResolveClean = () => {
    setCaseItem(prev => prev ? { ...prev, status: 'RESOLVED_CLEAN' } : null);
    triggerToast('Case resolved as CLEAN. Risk score overridden.');
  };

  const handleBlockUser = () => {
    setCaseItem(prev => prev ? { ...prev, status: 'LOCKED_BLOCKED' } : null);
    setShowConfirmLock(false);
    triggerToast('Account LOCKED permanently. IP ranges blocklisted.');
  };

  const getStatusIndicator = (status: string) => {
    switch (status) {
      case 'RESOLVED_CLEAN':
        return (
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-emerald-500/20 border border-emerald-500/30 text-emerald-400 text-xs font-bold">
            <ShieldCheck className="w-4 h-4" />
            RESOLVED CLEAN (Approved)
          </div>
        );
      case 'LOCKED_BLOCKED':
        return (
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-rose-500/20 border border-rose-500/30 text-rose-450 text-xs font-bold">
            <Lock className="w-4 h-4" />
            LOCKED & BANNED (System Action)
          </div>
        );
      default:
        return (
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-amber-500/20 border border-amber-500/30 text-amber-400 text-xs font-bold">
            <AlertCircle className="w-4 h-4 text-amber-400 animate-pulse" />
            UNDER MANUAL REVIEW
          </div>
        );
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-8 space-y-6 text-slate-100 animate-fade-in relative">
      {/* Back button */}
      <button 
        onClick={() => navigate('/admin/payments/fraud')} 
        className="flex items-center gap-2 text-slate-400 hover:text-white transition-colors text-xs"
      >
        <ArrowLeft className="w-4 h-4" />
        <span>Back to Fraud queue</span>
      </button>

      {/* Title */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
            <ShieldAlert className="w-8 h-8 text-rose-500" />
            Risk Audit: {caseItem.id}
          </h1>
          <p className="text-muted-foreground text-sm">
            Investigating transactional anomalies, card BIN locations, and server origin routing flags.
          </p>
        </div>
        <div>{getStatusIndicator(caseItem.status)}</div>
      </div>

      {/* Main Grid: Signals list vs User card */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column: Forensic Signals */}
        <div className="lg:col-span-2 space-y-6">
          {/* Explainable Risk Dials */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="glass p-5 rounded-3xl border border-white/5 text-center flex flex-col justify-between items-center">
              <span className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">IP reputation dial</span>
              <div className="relative w-20 h-20 flex items-center justify-center mt-3">
                <svg className="w-full h-full transform -rotate-90">
                  <circle cx="40" cy="40" r="34" className="stroke-slate-900 fill-none" strokeWidth="6" />
                  <circle cx="40" cy="40" r="34" className="stroke-rose-500 fill-none" strokeWidth="6" strokeDasharray="213" strokeDashoffset={213 - (213 * (caseItem.riskScore > 65 ? 85 : 30)) / 100} />
                </svg>
                <div className="absolute text-sm font-black text-white">{caseItem.riskScore > 65 ? 85 : 30}%</div>
              </div>
              <span className={`text-[10px] font-bold mt-3 uppercase tracking-widest ${caseItem.riskScore > 65 ? 'text-rose-450' : 'text-slate-400'}`}>
                {caseItem.riskScore > 65 ? 'CRITICAL ANOMALY' : 'CLEAN IP'}
              </span>
            </div>

            <div className="glass p-5 rounded-3xl border border-white/5 text-center flex flex-col justify-between items-center">
              <span className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Velocity limit dial</span>
              <div className="relative w-20 h-20 flex items-center justify-center mt-3">
                <svg className="w-full h-full transform -rotate-90">
                  <circle cx="40" cy="40" r="34" className="stroke-slate-900 fill-none" strokeWidth="6" />
                  <circle cx="40" cy="40" r="34" className="stroke-amber-500 fill-none" strokeWidth="6" strokeDasharray="213" strokeDashoffset={213 - (213 * (caseItem.riskScore > 50 ? 55 : 15)) / 100} />
                </svg>
                <div className="absolute text-sm font-black text-white">{caseItem.riskScore > 50 ? 55 : 15}%</div>
              </div>
              <span className={`text-[10px] font-bold mt-3 uppercase tracking-widest ${caseItem.riskScore > 50 ? 'text-amber-400 animate-pulse' : 'text-slate-400'}`}>
                {caseItem.riskScore > 50 ? 'SUSPICIOUS VELOCITY' : 'CLEAN VELOCITY'}
              </span>
            </div>

            <div className="glass p-5 rounded-3xl border border-white/5 text-center flex flex-col justify-between items-center">
              <span className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Device signature dial</span>
              <div className="relative w-20 h-20 flex items-center justify-center mt-3">
                <svg className="w-full h-full transform -rotate-90">
                  <circle cx="40" cy="40" r="34" className="stroke-slate-900 fill-none" strokeWidth="6" />
                  <circle cx="40" cy="40" r="34" className="stroke-emerald-500 fill-none" strokeWidth="6" strokeDasharray="213" strokeDashoffset={213 - (213 * 15) / 100} />
                </svg>
                <div className="absolute text-sm font-black text-white">15%</div>
              </div>
              <span className="text-[10px] text-emerald-450 font-bold mt-3 uppercase tracking-widest">CLEAN FINGERPRINT</span>
            </div>
          </div>

          <div className="glass rounded-3xl p-6 md:p-8 border border-white/5 space-y-6">
            <h2 className="text-md font-bold text-slate-200">Evaluated Risk Indicators</h2>
            <div className="space-y-4">
              {caseItem.signals.map((sig, idx) => (
                <div 
                  key={idx} 
                  className={`p-4 rounded-xl border flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 bg-slate-950/20 ${
                    sig.status === 'CRITICAL' ? 'border-rose-500/20' : sig.status === 'SUSPICIOUS' ? 'border-amber-500/20' : 'border-slate-900'
                  }`}
                >
                  <div className="space-y-1">
                    <div className="text-xs font-bold text-slate-250 flex items-center gap-1.5">
                      {sig.status === 'CRITICAL' ? (
                        <SuspiciousIcon className="w-3.5 h-3.5 text-rose-500" />
                      ) : sig.status === 'SUSPICIOUS' ? (
                        <AlertTriangle className="w-3.5 h-3.5 text-amber-500" />
                      ) : (
                        <VerifiedIcon className="w-3.5 h-3.5 text-emerald-400" />
                      )}
                      {sig.name}
                    </div>
                    <p className="text-[11px] text-slate-400 font-mono leading-relaxed">{sig.evaluated}</p>
                  </div>
                  <div className="text-right">
                    <div className={`text-sm font-extrabold ${sig.status === 'CRITICAL' ? 'text-rose-500' : sig.status === 'SUSPICIOUS' ? 'text-amber-500' : 'text-slate-400'}`}>
                      +{sig.riskWeight}%
                    </div>
                    <div className="text-[9px] text-slate-500 font-semibold uppercase">Impact</div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Action Log history */}
          <div className="glass rounded-3xl p-6 border border-white/5 space-y-4">
            <h2 className="text-md font-bold text-slate-200 flex items-center gap-2">
              <Terminal className="w-5 h-5 text-indigo-400" />
              Risk Review Operations
            </h2>
            <div className="p-4 bg-slate-950/80 border border-slate-900 rounded-xl font-mono text-[10px] space-y-2 text-slate-400">
              <div className="flex gap-2">
                <span className="text-indigo-400">[2026-06-15 18:32:00]</span>
                <span>Case {caseItem.id} initialized. Risk score calculated: {caseItem.riskScore}%.</span>
              </div>
              <div className="flex gap-2">
                <span className="text-indigo-400">[2026-06-15 18:32:01]</span>
                <span>Webhook notifications broadcasted to admin channels.</span>
              </div>
              {caseItem.status !== 'UNDER_REVIEW' && (
                <div className="flex gap-2 text-white">
                  <span className="text-emerald-400">&gt;&gt;&gt;</span>
                  <span>Case status transitioned to: {caseItem.status}. Logged by reviewer.</span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right Column: User & Account card */}
        <div className="space-y-6">
          {/* User Details */}
          <div className="glass rounded-3xl p-6 border border-white/5 space-y-5 text-xs">
            <div className="flex items-center gap-3 border-b border-slate-900 pb-4">
              <div className="w-10 h-10 rounded-xl bg-slate-950 border border-slate-900 flex items-center justify-center text-primary shrink-0">
                <User className="w-5 h-5" />
              </div>
              <div>
                <h3 className="font-bold text-sm text-white">{caseItem.userName}</h3>
                <span className="text-[10px] text-slate-550 block font-mono mt-0.5">{caseItem.userId}</span>
              </div>
            </div>

            <div className="space-y-3">
              <div className="flex justify-between items-center text-slate-400">
                <span>Email address:</span>
                <span className="font-bold text-slate-250 font-mono">{caseItem.userEmail}</span>
              </div>
              <div className="flex justify-between items-center text-slate-400">
                <span>Phone number:</span>
                <span className="font-bold text-slate-250 font-mono">{caseItem.userPhone}</span>
              </div>
              <div className="flex justify-between items-center text-slate-400">
                <span>Account age:</span>
                <span className="font-bold text-slate-250">{caseItem.accountAge}</span>
              </div>
              <div className="flex justify-between items-center text-slate-400">
                <span>Booking ID:</span>
                <span className="font-bold text-primary font-mono">{caseItem.bookingId}</span>
              </div>
              <div className="flex justify-between items-center text-slate-400">
                <span>Payment Reference:</span>
                <span className="font-bold text-slate-200 font-mono">{caseItem.paymentId}</span>
              </div>
              <div className="flex justify-between items-center text-slate-400">
                <span>Total Amount:</span>
                <span className="font-extrabold text-slate-200">₹{caseItem.amount.toLocaleString()}</span>
              </div>
              <div className="flex justify-between items-center text-slate-400 border-t border-slate-900 pt-2 mt-2">
                <span>Ruleset Evaluated:</span>
                <span className="font-extrabold text-indigo-400 font-mono">{caseItem.rulesetVersion}</span>
              </div>
            </div>
          </div>

          {/* Danger Decision Panel */}
          {caseItem.status === 'UNDER_REVIEW' && (
            <div className="glass rounded-3xl p-6 border border-white/5 space-y-4">
              <h3 className="font-bold text-slate-200 text-sm">Review Action Required</h3>
              <p className="text-[11px] text-slate-450 leading-relaxed">
                As an administrator, evaluate the logs. You can either approve this transaction (Override Clean) or ban the user.
              </p>
              
              <div className="space-y-3 pt-2">
                <button
                  onClick={handleResolveClean}
                  className="w-full py-3 bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-bold text-xs rounded-xl flex items-center justify-center gap-2 shadow-md transition-all active:scale-[0.98]"
                >
                  <Check className="w-4 h-4" />
                  <span>Override Clean (Approve)</span>
                </button>
                <button
                  onClick={() => setShowConfirmLock(true)}
                  className="w-full py-3 bg-slate-900 border border-rose-900/30 text-rose-450 hover:bg-rose-950/20 font-bold text-xs rounded-xl flex items-center justify-center gap-2 transition-all active:scale-[0.98]"
                >
                  <Lock className="w-4 h-4" />
                  <span>Lock Account & Ban</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Confirmation Modal */}
      <AnimatePresence>
        {showConfirmLock && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 0.6 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowConfirmLock(false)}
              className="fixed inset-0 bg-slate-950 z-40"
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9 }}
              className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full max-w-md bg-slate-950 border border-slate-900 p-6 rounded-2xl z-50 text-xs space-y-5"
            >
              <div className="p-3 bg-rose-500/10 border border-rose-500/20 rounded-2xl w-fit text-rose-400">
                <AlertCircle className="w-6 h-6 animate-pulse" />
              </div>
              <div className="space-y-2">
                <h3 className="text-base font-extrabold text-white">Confirm Permanent Ban?</h3>
                <p className="text-slate-400 leading-relaxed">
                  This action is irreversible. The account for <span className="text-white font-bold">{caseItem.userName}</span> will be locked, all properties will be hidden, and their client device footprint hashes will be blacklisted.
                </p>
              </div>
              <div className="flex gap-3 justify-end border-t border-slate-900 pt-4 text-xs font-semibold">
                <button
                  onClick={() => setShowConfirmLock(false)}
                  className="px-4 py-2 border border-slate-800 rounded-xl hover:bg-slate-900 text-slate-400 hover:text-white transition-all"
                >
                  Cancel
                </button>
                <button
                  onClick={handleBlockUser}
                  className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl transition-all shadow-md shadow-rose-950/20"
                >
                  Lock Account Permanently
                </button>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>

      {/* Toast Notification */}
      <AnimatePresence>
        {toastMessage && (
          <motion.div
            initial={{ opacity: 0, y: 50, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 50, scale: 0.9 }}
            className="fixed bottom-6 right-6 p-4 bg-indigo-950/90 border border-indigo-500/30 text-indigo-300 rounded-2xl flex items-center gap-3 shadow-2xl z-50 text-xs"
          >
            <ShieldCheck className="w-5 h-5 text-indigo-400 shrink-0" />
            <div>
              <span className="font-bold text-white block">Action Completed</span>
              <span>{toastMessage}</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
