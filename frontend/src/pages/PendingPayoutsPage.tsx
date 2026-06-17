import React, { useState, useEffect } from 'react';
import { paymentService, PayoutDto } from '../services/paymentService';
import { ArrowUpRight, RefreshCw, AlertCircle, Loader2, AlertTriangle, ChevronDown, ChevronUp } from 'lucide-react';

const STATUS_BADGE: Record<string, string> = {
  PENDING:    'bg-amber-500/10 text-amber-300 border-amber-500/20',
  PROCESSING: 'bg-blue-500/10 text-blue-300 border-blue-500/20',
  SUCCEEDED:  'bg-emerald-500/10 text-emerald-300 border-emerald-500/20',
  FAILED:     'bg-red-500/10 text-red-300 border-red-500/20',
};

const STATUS_TIMELINE: Record<string, { label: string; step: number }[]> = {
  PENDING:    [{ label: 'Requested', step: 1 }, { label: 'Processing', step: 0 }, { label: 'Completed', step: 0 }],
  PROCESSING: [{ label: 'Requested', step: 2 }, { label: 'Processing', step: 1 }, { label: 'Completed', step: 0 }],
  SUCCEEDED:  [{ label: 'Requested', step: 2 }, { label: 'Processing', step: 2 }, { label: 'Completed', step: 2 }],
  FAILED:     [{ label: 'Requested', step: 2 }, { label: 'Processing', step: -1 }, { label: 'Failed', step: -1 }],
};

function PayoutTimeline({ status }: { status: string }) {
  const steps = STATUS_TIMELINE[status] || STATUS_TIMELINE['PENDING'];
  return (
    <div className="flex items-center gap-1 mt-3 pt-3 border-t border-border">
      {steps.map((s, i) => {
        const isDone = s.step === 2;
        const isActive = s.step === 1;
        const isFailed = s.step === -1;
        return (
          <React.Fragment key={i}>
            <div className="flex flex-col items-center text-center" style={{ minWidth: 56 }}>
              <div className={`w-5 h-5 rounded-full border-2 text-[9px] flex items-center justify-center font-bold transition-all ${
                isFailed ? 'border-red-500 bg-red-500/20 text-red-300' :
                isDone ? 'border-emerald-500 bg-emerald-500/20 text-emerald-300' :
                isActive ? 'border-indigo-400 bg-indigo-500/20 text-indigo-300 animate-pulse' :
                'border-slate-700 bg-slate-800 text-slate-600'
              }`}>
                {isFailed ? '✕' : isDone ? '✓' : i + 1}
              </div>
              <span className={`text-[9px] mt-0.5 font-semibold ${
                isFailed ? 'text-red-400' : isDone ? 'text-emerald-400' : isActive ? 'text-indigo-300' : 'text-slate-600'
              }`}>{s.label}</span>
            </div>
            {i < steps.length - 1 && (
              <div className={`flex-1 h-0.5 rounded-full mb-3 ${isDone ? 'bg-emerald-500' : 'bg-slate-700'}`} />
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
}

export default function PendingPayoutsPage() {
  const [payouts, setPayouts] = useState<PayoutDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  // Form
  const [amount, setAmount] = useState('');
  const [account, setAccount] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [formSuccess, setFormSuccess] = useState(false);

  useEffect(() => { fetchPayouts(); }, []);

  const fetchPayouts = async () => {
    setLoading(true);
    setError(null);
    try {
      setPayouts(await paymentService.getOwnerPayouts());
    } catch {
      setError('Failed to load payouts. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    const numAmount = parseFloat(amount);
    if (!numAmount || numAmount <= 0) { setFormError('Enter a valid amount.'); return; }
    if (!account.trim()) { setFormError('Enter a destination account or UPI ID.'); return; }

    setSubmitting(true);
    try {
      const p = await paymentService.initiatePayout({ amount: numAmount, currency: 'INR', destinationAccount: account });
      setPayouts(prev => [p, ...prev]);
      setAmount(''); setAccount('');
      setFormSuccess(true);
      setTimeout(() => setFormSuccess(false), 3000);
    } catch (err: any) {
      setFormError(err.response?.data?.message || 'Failed to request payout.');
    } finally {
      setSubmitting(false);
    }
  };

  const fmt = (n: number) => `₹${n.toLocaleString('en-IN')}`;

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
            <ArrowUpRight className="w-7 h-7 text-indigo-400" />
            Payout Management
          </h1>
          <p className="text-muted-foreground mt-1 text-sm">Request and track fund withdrawals to your bank.</p>
        </div>
        <button onClick={fetchPayouts} className="flex items-center gap-2 px-4 py-2 border border-border rounded-xl text-sm font-semibold hover:bg-card transition-all">
          <RefreshCw className="w-4 h-4" /> Refresh
        </button>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-xl p-4 mb-6 flex items-center gap-3">
          <AlertCircle className="w-5 h-5 shrink-0" /><span className="text-sm">{error}</span>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Request Form */}
        <div className="lg:col-span-1">
          <div className="glass rounded-2xl p-5 border border-white/5 sticky top-6">
            <h2 className="font-bold text-sm uppercase tracking-wider text-muted-foreground mb-4">New Payout Request</h2>
            {formSuccess && (
              <div className="mb-4 text-sm text-emerald-300 bg-emerald-500/10 border border-emerald-500/20 rounded-xl p-3">
                ✓ Payout requested successfully!
              </div>
            )}
            <form onSubmit={handleSubmit} className="space-y-4">
              {formError && (
                <div className="flex items-start gap-2 text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-xl p-3">
                  <AlertTriangle className="w-4 h-4 mt-0.5 shrink-0" />{formError}
                </div>
              )}
              <div className="space-y-1">
                <label htmlFor="payout-amount" className="text-sm font-semibold">Amount (INR)</label>
                <input
                  id="payout-amount"
                  type="number" min={1} value={amount}
                  onChange={e => setAmount(e.target.value)}
                  placeholder="e.g. 25000"
                  className="w-full px-4 py-3 rounded-xl border border-border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  required
                />
              </div>
              <div className="space-y-1">
                <label htmlFor="payout-dest" className="text-sm font-semibold">Destination Account</label>
                <input
                  id="payout-dest"
                  type="text" value={account}
                  onChange={e => setAccount(e.target.value)}
                  placeholder="Bank A/C or UPI ID"
                  className="w-full px-4 py-3 rounded-xl border border-border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  required
                />
              </div>
              <button
                type="submit" disabled={submitting}
                className="w-full py-3 rounded-xl font-bold text-sm text-white bg-gradient-to-r from-indigo-600 to-purple-600 hover:opacity-90 disabled:opacity-50 flex items-center justify-center gap-2 transition-all"
              >
                {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
                {submitting ? 'Requesting…' : 'Request Payout'}
              </button>
            </form>
          </div>
        </div>

        {/* Payout List */}
        <div className="lg:col-span-2">
          <h2 className="text-sm font-bold uppercase tracking-wider text-muted-foreground mb-4">
            All Payouts ({payouts.length})
          </h2>
          {loading ? (
            <div className="space-y-3">
              {[1,2,3].map(i => <div key={i} className="h-20 rounded-2xl bg-card border border-border animate-pulse" />)}
            </div>
          ) : payouts.length === 0 ? (
            <div className="text-center py-12 border border-dashed border-border rounded-2xl bg-card">
              <div className="text-4xl mb-3">💸</div>
              <p className="text-sm text-muted-foreground">No payout requests yet.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {payouts.map(p => (
                <div key={p.id} className="glass rounded-2xl border border-white/5 overflow-hidden">
                  <button
                    className="w-full flex items-center justify-between p-4 text-left hover:bg-white/5 transition-colors"
                    onClick={() => setExpandedId(expandedId === p.id ? null : p.id)}
                    aria-expanded={expandedId === p.id}
                    aria-label={`Toggle payout ${p.id} details`}
                  >
                    <div className="flex items-center gap-4">
                      <div className="text-left">
                        <p className="font-bold">{fmt(p.amount)}</p>
                        <p className="text-xs text-muted-foreground">{p.createdAt ? new Date(p.createdAt).toLocaleDateString() : '—'}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className={`px-2.5 py-1 text-xs font-semibold rounded-full border ${STATUS_BADGE[p.status] || ''}`}>
                        {p.status}
                      </span>
                      {expandedId === p.id ? <ChevronUp className="w-4 h-4 text-muted-foreground" /> : <ChevronDown className="w-4 h-4 text-muted-foreground" />}
                    </div>
                  </button>

                  {expandedId === p.id && (
                    <div className="px-4 pb-4">
                      <div className="grid grid-cols-2 gap-3 text-sm mb-2">
                        <div>
                          <span className="text-muted-foreground text-xs block">Destination</span>
                          <span className="font-mono text-xs">{p.destinationAccount || '—'}</span>
                        </div>
                        <div>
                          <span className="text-muted-foreground text-xs block">Gateway ID</span>
                          <span className="font-mono text-xs">{p.gatewayPayoutId || '—'}</span>
                        </div>
                      </div>
                      <PayoutTimeline status={p.status} />
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
