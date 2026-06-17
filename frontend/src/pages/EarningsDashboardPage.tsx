import React, { useState, useEffect } from 'react';
import { paymentService, PayoutDto } from '../services/paymentService';
import {
  TrendingUp, Wallet, ArrowUpRight, RefreshCw, AlertCircle, Plus, X, Loader2
} from 'lucide-react';

// Simple SVG bar chart for last 6 months
function MonthlyBarChart({ data }: { data: { month: string; amount: number }[] }) {
  const max = Math.max(...data.map(d => d.amount), 1);
  return (
    <div className="flex items-end gap-3 h-32 px-2" role="img" aria-label="Monthly earnings chart">
      {data.map((d, i) => {
        const heightPct = (d.amount / max) * 100;
        return (
          <div key={i} className="flex flex-col items-center flex-1 gap-1">
            <span className="text-[9px] text-muted-foreground font-semibold">
              {d.amount > 0 ? `₹${(d.amount / 1000).toFixed(0)}k` : ''}
            </span>
            <div className="w-full rounded-t-lg bg-gradient-to-t from-indigo-600 to-purple-500 transition-all duration-700"
              style={{ height: `${Math.max(heightPct, 4)}%` }}
              aria-label={`${d.month}: ₹${d.amount}`}
            />
            <span className="text-[9px] text-muted-foreground">{d.month}</span>
          </div>
        );
      })}
    </div>
  );
}

function StatCard({ label, value, icon, gradient }: {
  label: string; value: string; icon: React.ReactNode; gradient: string;
}) {
  return (
    <div className={`glass rounded-2xl p-5 border border-white/5 bg-gradient-to-br ${gradient} bg-opacity-5`}>
      <div className="flex justify-between items-start mb-3">
        <span className="text-xs font-bold uppercase tracking-wider text-muted-foreground">{label}</span>
        <div className="w-8 h-8 rounded-lg bg-white/10 flex items-center justify-center">{icon}</div>
      </div>
      <p className="text-2xl font-extrabold tracking-tight">{value}</p>
    </div>
  );
}

export default function EarningsDashboardPage() {
  const [payouts, setPayouts] = useState<PayoutDto[]>([]);
  const [escrowBalance, setEscrowBalance] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showPayoutModal, setShowPayoutModal] = useState(false);

  // Payout form
  const [payoutAmount, setPayoutAmount] = useState('');
  const [payoutAccount, setPayoutAccount] = useState('');
  const [payoutSubmitting, setPayoutSubmitting] = useState(false);
  const [payoutError, setPayoutError] = useState<string | null>(null);

  useEffect(() => { fetchData(); }, []);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [payoutsData, escrowData] = await Promise.all([
        paymentService.getOwnerPayouts(),
        paymentService.getOwnerEscrowAccounts(),
      ]);
      setPayouts(payoutsData);
      setEscrowBalance(escrowData.filter(e => e.status === 'HELD').reduce((sum, e) => sum + e.balance, 0));
    } catch {
      setError('Could not load earnings data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const totalEarned = payouts.filter(p => p.status === 'SUCCEEDED').reduce((sum, p) => sum + p.amount, 0);
  const totalRequested = payouts.reduce((sum, p) => sum + p.amount, 0);

  const fmt = (n: number) => `₹${n.toLocaleString('en-IN')}`;

  // Generate last 6 months chart data from payouts
  const chartData = (() => {
    const now = new Date();
    return Array.from({ length: 6 }, (_, i) => {
      const d = new Date(now.getFullYear(), now.getMonth() - (5 - i), 1);
      const month = d.toLocaleDateString(undefined, { month: 'short' });
      const amount = payouts
        .filter(p => {
          const pd = new Date(p.createdAt || '');
          return pd.getMonth() === d.getMonth() && pd.getFullYear() === d.getFullYear() && p.status === 'SUCCEEDED';
        })
        .reduce((sum, p) => sum + p.amount, 0);
      return { month, amount };
    });
  })();

  const handleRequestPayout = async (e: React.FormEvent) => {
    e.preventDefault();
    setPayoutError(null);
    const amt = parseFloat(payoutAmount);
    if (!amt || amt <= 0) { setPayoutError('Enter a valid amount.'); return; }
    if (!payoutAccount.trim()) { setPayoutError('Enter a destination account.'); return; }

    setPayoutSubmitting(true);
    try {
      const payout = await paymentService.initiatePayout({ amount: amt, currency: 'INR', destinationAccount: payoutAccount });
      setPayouts(prev => [payout, ...prev]);
      setShowPayoutModal(false);
      setPayoutAmount('');
      setPayoutAccount('');
    } catch (err: any) {
      setPayoutError(err.response?.data?.message || 'Failed to request payout.');
    } finally {
      setPayoutSubmitting(false);
    }
  };

  const STATUS_BADGE: Record<string, string> = {
    PENDING:    'bg-amber-500/10 text-amber-300 border-amber-500/20',
    PROCESSING: 'bg-blue-500/10 text-blue-300 border-blue-500/20',
    SUCCEEDED:  'bg-emerald-500/10 text-emerald-300 border-emerald-500/20',
    FAILED:     'bg-red-500/10 text-red-300 border-red-500/20',
  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
            <TrendingUp className="w-7 h-7 text-emerald-400" />
            Earnings Dashboard
          </h1>
          <p className="text-muted-foreground mt-1 text-sm">Track your income, pending escrow, and payout history.</p>
        </div>
        <button onClick={fetchData} className="flex items-center gap-2 px-4 py-2 border border-border rounded-xl text-sm font-semibold hover:bg-card transition-all">
          <RefreshCw className="w-4 h-4" /> Refresh
        </button>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-xl p-4 mb-6 flex items-center gap-3">
          <AlertCircle className="w-5 h-5 shrink-0" /><span className="text-sm">{error}</span>
        </div>
      )}

      {loading ? (
        <div className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            {[1,2,3].map(i => <div key={i} className="h-28 rounded-2xl bg-card border border-border animate-pulse" />)}
          </div>
          <div className="h-40 rounded-2xl bg-card border border-border animate-pulse" />
        </div>
      ) : (
        <>
          {/* Stats Row */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
            <StatCard
              label="Total Earned"
              value={fmt(totalEarned)}
              gradient="from-emerald-600/10"
              icon={<TrendingUp className="w-4 h-4 text-emerald-400" />}
            />
            <StatCard
              label="Pending in Escrow"
              value={fmt(escrowBalance)}
              gradient="from-amber-600/10"
              icon={<Wallet className="w-4 h-4 text-amber-400" />}
            />
            <StatCard
              label="Total Payout Requests"
              value={fmt(totalRequested)}
              gradient="from-indigo-600/10"
              icon={<ArrowUpRight className="w-4 h-4 text-indigo-400" />}
            />
          </div>

          {/* Chart */}
          <div className="glass rounded-2xl p-5 border border-white/5 mb-6">
            <h2 className="text-sm font-bold uppercase tracking-wider text-muted-foreground mb-4">
              Earnings — Last 6 Months
            </h2>
            <MonthlyBarChart data={chartData} />
          </div>

          {/* Recent Payouts Table */}
          <div className="glass rounded-2xl border border-white/5 overflow-hidden">
            <div className="flex items-center justify-between p-5 border-b border-border">
              <h2 className="font-bold text-sm uppercase tracking-wider text-muted-foreground">Recent Payouts</h2>
            </div>
            {payouts.length === 0 ? (
              <div className="text-center py-12">
                <div className="text-4xl mb-3">💸</div>
                <p className="text-sm text-muted-foreground">No payouts yet</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border text-xs uppercase tracking-wider text-muted-foreground">
                      <th className="px-5 py-3 text-left font-semibold">Date</th>
                      <th className="px-5 py-3 text-left font-semibold">Amount</th>
                      <th className="px-5 py-3 text-left font-semibold">Status</th>
                      <th className="px-5 py-3 text-left font-semibold">Gateway ID</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    {payouts.slice(0, 10).map(p => (
                      <tr key={p.id} className="hover:bg-card/50 transition-colors">
                        <td className="px-5 py-4 text-muted-foreground">
                          {p.createdAt ? new Date(p.createdAt).toLocaleDateString() : '—'}
                        </td>
                        <td className="px-5 py-4 font-bold">{fmt(p.amount)}</td>
                        <td className="px-5 py-4">
                          <span className={`px-2.5 py-1 text-xs font-semibold rounded-full border ${STATUS_BADGE[p.status] || ''}`}>
                            {p.status}
                          </span>
                        </td>
                        <td className="px-5 py-4 font-mono text-xs text-slate-500">
                          {p.gatewayPayoutId || '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}

      {/* Floating Action Button */}
      <button
        onClick={() => setShowPayoutModal(true)}
        className="fixed bottom-8 right-8 w-14 h-14 rounded-full bg-gradient-to-br from-indigo-600 to-purple-600 text-white shadow-xl flex items-center justify-center hover:opacity-90 transition-all hover:scale-110 focus:outline-none focus:ring-4 focus:ring-indigo-500/40 z-40"
        aria-label="Request payout"
        title="Request Payout"
      >
        <Plus className="w-6 h-6" />
      </button>

      {/* Payout Modal */}
      {showPayoutModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4 bg-black/60 backdrop-blur-sm" role="dialog" aria-modal="true" aria-label="Request payout">
          <div className="glass rounded-2xl p-6 w-full max-w-md border border-white/10 shadow-2xl">
            <div className="flex justify-between items-center mb-5">
              <h3 className="font-bold text-lg">Request Payout</h3>
              <button onClick={() => setShowPayoutModal(false)} className="p-2 rounded-lg hover:bg-card transition-all" aria-label="Close modal">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleRequestPayout} className="space-y-4">
              {payoutError && (
                <div className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-xl p-3">{payoutError}</div>
              )}

              <div className="space-y-1">
                <label htmlFor="payout-amount" className="text-sm font-semibold">Amount (INR)</label>
                <input
                  id="payout-amount"
                  type="number"
                  min={1}
                  value={payoutAmount}
                  onChange={e => setPayoutAmount(e.target.value)}
                  placeholder="Enter amount"
                  className="w-full px-4 py-3 rounded-xl border border-border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  required
                />
              </div>

              <div className="space-y-1">
                <label htmlFor="payout-account" className="text-sm font-semibold">Destination Account</label>
                <input
                  id="payout-account"
                  type="text"
                  value={payoutAccount}
                  onChange={e => setPayoutAccount(e.target.value)}
                  placeholder="Bank account / UPI ID"
                  className="w-full px-4 py-3 rounded-xl border border-border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  required
                />
              </div>

              <button
                type="submit"
                disabled={payoutSubmitting}
                className="w-full py-3 rounded-xl font-bold text-sm text-white bg-gradient-to-r from-indigo-600 to-purple-600 hover:opacity-90 disabled:opacity-50 transition-all flex items-center justify-center gap-2"
              >
                {payoutSubmitting && <Loader2 className="w-4 h-4 animate-spin" />}
                {payoutSubmitting ? 'Requesting…' : 'Request Payout'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
