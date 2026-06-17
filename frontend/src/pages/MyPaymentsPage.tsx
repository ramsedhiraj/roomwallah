import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { paymentService, PaymentDto } from '../services/paymentService';
import {
  CreditCard, RefreshCw, AlertCircle, FileText, RotateCcw, ChevronUp, ChevronDown
} from 'lucide-react';

type SortDir = 'asc' | 'desc';

const STATUS_BADGE: Record<string, string> = {
  PENDING:  'bg-amber-500/10 text-amber-300 border-amber-500/20',
  CAPTURED: 'bg-emerald-500/10 text-emerald-300 border-emerald-500/20',
  FAILED:   'bg-red-500/10 text-red-300 border-red-500/20',
  REFUNDED: 'bg-purple-500/10 text-purple-300 border-purple-500/20',
};

const STATUS_LEFT_BORDER: Record<string, string> = {
  PENDING:  'border-l-4 border-l-amber-500',
  CAPTURED: 'border-l-4 border-l-emerald-500',
  FAILED:   'border-l-4 border-l-red-500',
  REFUNDED: 'border-l-4 border-l-purple-500',
};

export default function MyPaymentsPage() {
  const navigate = useNavigate();
  const [payments, setPayments] = useState<PaymentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [sortDir, setSortDir] = useState<SortDir>('desc');

  useEffect(() => { fetchPayments(); }, []);

  const fetchPayments = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await paymentService.getMyPayments();
      setPayments(data);
    } catch {
      setError('Could not load your payment history. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const sorted = useMemo(() => {
    return [...payments].sort((a, b) => {
      const da = new Date(a.createdAt || 0).getTime();
      const db = new Date(b.createdAt || 0).getTime();
      return sortDir === 'desc' ? db - da : da - db;
    });
  }, [payments, sortDir]);

  const fmt = (n: number, currency: string) =>
    `${currency === 'INR' ? '₹' : '$'}${n.toLocaleString('en-IN')}`;

  const formatDate = (iso?: string) =>
    iso ? new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' }) : '—';

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
            <CreditCard className="w-7 h-7 text-indigo-400" />
            My Payments
          </h1>
          <p className="text-muted-foreground mt-1 text-sm">
            View all your payment transactions and download invoices.
          </p>
        </div>
        <button
          onClick={fetchPayments}
          className="flex items-center gap-2 px-4 py-2 border border-border rounded-xl text-sm font-semibold hover:bg-card transition-all"
          aria-label="Refresh payments"
        >
          <RefreshCw className="w-4 h-4" />
          Refresh
        </button>
      </div>

      {/* Error */}
      {error && (
        <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-xl p-4 mb-6 flex items-center gap-3">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span className="text-sm">{error}</span>
        </div>
      )}

      {/* Loading skeleton */}
      {loading ? (
        <div className="space-y-3">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-20 rounded-2xl bg-card border border-border animate-pulse" />
          ))}
        </div>
      ) : payments.length === 0 ? (
        <div className="text-center py-16 border border-dashed border-border rounded-2xl bg-card">
          <div className="text-5xl mb-4">💳</div>
          <p className="font-semibold text-muted-foreground">No payments found</p>
          <p className="text-xs text-muted-foreground mt-1">Complete a booking to see your payment history here.</p>
        </div>
      ) : (
        <>
          {/* Sort control */}
          <div className="flex items-center justify-end mb-3">
            <button
              onClick={() => setSortDir(d => d === 'desc' ? 'asc' : 'desc')}
              className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors"
              aria-label="Toggle sort direction"
            >
              Sort by Date
              {sortDir === 'desc' ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronUp className="w-3.5 h-3.5" />}
            </button>
          </div>

          {/* Desktop Table */}
          <div className="hidden md:block overflow-hidden rounded-2xl border border-border">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-card border-b border-border text-muted-foreground text-xs uppercase tracking-wider">
                  <th className="px-5 py-3 text-left font-semibold">Date</th>
                  <th className="px-5 py-3 text-left font-semibold">Booking ID</th>
                  <th className="px-5 py-3 text-left font-semibold">Amount</th>
                  <th className="px-5 py-3 text-left font-semibold">Status</th>
                  <th className="px-5 py-3 text-left font-semibold">Provider</th>
                  <th className="px-5 py-3 text-right font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {sorted.map(p => (
                  <tr
                    key={p.id}
                    className={`bg-card hover:shadow-md transition-all ${STATUS_LEFT_BORDER[p.status] || ''}`}
                  >
                    <td className="px-5 py-4 text-muted-foreground">{formatDate(p.createdAt)}</td>
                    <td className="px-5 py-4 font-mono text-xs text-slate-400">{p.bookingId.substring(0, 12)}…</td>
                    <td className="px-5 py-4 font-bold">{fmt(p.amount, p.currency)}</td>
                    <td className="px-5 py-4">
                      <span className={`px-2.5 py-1 text-xs font-semibold rounded-full border ${STATUS_BADGE[p.status] || 'bg-muted text-muted-foreground'}`}>
                        {p.status}
                      </span>
                    </td>
                    <td className="px-5 py-4 text-muted-foreground capitalize">{p.gatewayProvider}</td>
                    <td className="px-5 py-4">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => navigate(`/payments/invoices`)}
                          className="flex items-center gap-1 px-3 py-1.5 text-xs font-semibold border border-border rounded-lg hover:bg-background transition-all"
                          aria-label="View invoice"
                        >
                          <FileText className="w-3.5 h-3.5" />
                          Invoice
                        </button>
                        {p.status === 'CAPTURED' && (
                          <button
                            onClick={() => navigate('/payments/refunds')}
                            className="flex items-center gap-1 px-3 py-1.5 text-xs font-semibold border border-purple-500/30 text-purple-300 rounded-lg hover:bg-purple-500/10 transition-all"
                            aria-label="Request refund"
                          >
                            <RotateCcw className="w-3.5 h-3.5" />
                            Refund
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile card list */}
          <div className="md:hidden space-y-3">
            {sorted.map(p => (
              <div
                key={p.id}
                className={`glass rounded-2xl p-4 border border-white/5 ${STATUS_LEFT_BORDER[p.status] || ''}`}
              >
                <div className="flex justify-between items-start mb-2">
                  <span className="font-bold">{fmt(p.amount, p.currency)}</span>
                  <span className={`px-2 py-0.5 text-[10px] font-bold rounded-full border ${STATUS_BADGE[p.status] || ''}`}>
                    {p.status}
                  </span>
                </div>
                <p className="text-xs text-muted-foreground mb-1">{formatDate(p.createdAt)} · {p.gatewayProvider}</p>
                <p className="text-xs font-mono text-slate-500 mb-3">{p.bookingId.substring(0, 16)}…</p>
                <div className="flex gap-2">
                  <button onClick={() => navigate('/payments/invoices')} className="flex-1 text-center py-1.5 text-xs font-semibold border border-border rounded-lg hover:bg-background">Invoice</button>
                  {p.status === 'CAPTURED' && (
                    <button onClick={() => navigate('/payments/refunds')} className="flex-1 text-center py-1.5 text-xs font-semibold border border-purple-500/30 text-purple-300 rounded-lg hover:bg-purple-500/10">Refund</button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
