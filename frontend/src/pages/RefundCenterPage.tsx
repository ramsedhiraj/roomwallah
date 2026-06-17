import React, { useState, useEffect } from 'react';
import { paymentService, PaymentDto, RefundDto } from '../services/paymentService';
import RefundTracker, { RefundStep } from '../components/RefundTracker';
import { AlertCircle, RefreshCw, RotateCcw, Loader2, AlertTriangle, CheckCircle2 } from 'lucide-react';

export default function RefundCenterPage() {
  const [payments, setPayments] = useState<PaymentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Refund form state
  const [selectedPayment, setSelectedPayment] = useState<PaymentDto | null>(null);
  const [refundAmount, setRefundAmount] = useState('');
  const [refundReason, setRefundReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [refundResult, setRefundResult] = useState<RefundDto | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => { fetchPayments(); }, []);

  const fetchPayments = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await paymentService.getMyPayments();
      setPayments(data.filter(p => p.status === 'CAPTURED'));
    } catch {
      setError('Could not load eligible payments. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const fmt = (n: number, c: string) => `${c === 'INR' ? '₹' : '$'}${n.toLocaleString('en-IN')}`;

  const getTrackerSteps = (refund: RefundDto): RefundStep[] => [
    { label: 'Requested', status: 'done' },
    { label: 'Processing', status: refund.status === 'PENDING' ? 'active' : 'done' },
    {
      label: refund.status === 'SUCCEEDED' ? 'Credited' : refund.status === 'FAILED' ? 'Failed' : 'Pending',
      status: refund.status === 'SUCCEEDED' ? 'done' : refund.status === 'FAILED' ? 'active' : 'pending',
    },
  ];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    if (!selectedPayment) return;

    const numAmount = parseFloat(refundAmount);
    if (isNaN(numAmount) || numAmount <= 0) {
      setFormError('Please enter a valid refund amount greater than 0.');
      return;
    }
    if (numAmount > selectedPayment.amount) {
      setFormError(`Amount cannot exceed the original payment of ${fmt(selectedPayment.amount, selectedPayment.currency)}.`);
      return;
    }

    setSubmitting(true);
    try {
      const result = await paymentService.initiateRefund(selectedPayment.id, {
        amount: numAmount,
        reason: refundReason.trim() || undefined,
      });
      setRefundResult(result);
      setRefundAmount('');
      setRefundReason('');
    } catch (err: any) {
      setFormError(err.response?.data?.message || 'Failed to initiate refund. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
            <RotateCcw className="w-7 h-7 text-purple-400" />
            Refund Center
          </h1>
          <p className="text-muted-foreground mt-1 text-sm">
            Request refunds for eligible completed payments.
          </p>
        </div>
        <button
          onClick={fetchPayments}
          className="flex items-center gap-2 px-4 py-2 border border-border rounded-xl text-sm font-semibold hover:bg-card transition-all"
        >
          <RefreshCw className="w-4 h-4" />
          Refresh
        </button>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-xl p-4 mb-6 flex items-center gap-3">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span className="text-sm">{error}</span>
        </div>
      )}

      {/* Refund success display */}
      {refundResult && (
        <div className="glass rounded-2xl border border-emerald-500/20 p-6 mb-6">
          <div className="flex items-center gap-3 mb-4">
            <CheckCircle2 className="w-6 h-6 text-emerald-400" />
            <h3 className="font-bold text-emerald-300">Refund Initiated Successfully</h3>
          </div>
          <div className="mb-4">
            <RefundTracker steps={getTrackerSteps(refundResult)} />
          </div>
          <div className="grid grid-cols-2 gap-3 text-sm">
            <div>
              <span className="text-muted-foreground block text-xs">Refund ID</span>
              <span className="font-mono text-xs">{refundResult.id.substring(0, 16)}…</span>
            </div>
            <div>
              <span className="text-muted-foreground block text-xs">Amount</span>
              <span className="font-bold">{fmt(refundResult.amount, refundResult.currency)}</span>
            </div>
            <div>
              <span className="text-muted-foreground block text-xs">Status</span>
              <span className={`font-semibold ${
                refundResult.status === 'SUCCEEDED' ? 'text-emerald-400' :
                refundResult.status === 'FAILED' ? 'text-red-400' : 'text-amber-400'
              }`}>{refundResult.status}</span>
            </div>
            {refundResult.reason && (
              <div>
                <span className="text-muted-foreground block text-xs">Reason</span>
                <span className="text-slate-300">{refundResult.reason}</span>
              </div>
            )}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Eligible Payments */}
        <div>
          <h2 className="text-sm font-bold uppercase tracking-widest text-muted-foreground mb-3">
            Eligible Payments ({payments.length})
          </h2>
          {loading ? (
            <div className="space-y-3">
              {[1, 2].map(i => <div key={i} className="h-20 rounded-2xl bg-card border border-border animate-pulse" />)}
            </div>
          ) : payments.length === 0 ? (
            <div className="text-center py-10 border border-dashed border-border rounded-2xl bg-card">
              <div className="text-4xl mb-3">🎉</div>
              <p className="text-sm font-semibold text-muted-foreground">No refund-eligible payments</p>
              <p className="text-xs text-muted-foreground mt-1">Only CAPTURED payments can be refunded.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {payments.map(p => (
                <button
                  key={p.id}
                  onClick={() => { setSelectedPayment(p); setRefundResult(null); setFormError(null); }}
                  className={`w-full text-left glass rounded-2xl p-4 border transition-all duration-200 hover:border-purple-500/40 focus:outline-none focus:ring-2 focus:ring-purple-500 ${
                    selectedPayment?.id === p.id ? 'border-purple-500/60 bg-purple-500/5' : 'border-white/5'
                  }`}
                  aria-pressed={selectedPayment?.id === p.id}
                >
                  <div className="flex justify-between items-center mb-1">
                    <span className="font-bold">{fmt(p.amount, p.currency)}</span>
                    <span className="text-xs text-emerald-400 font-semibold border border-emerald-500/20 px-2 py-0.5 rounded-full bg-emerald-500/10">CAPTURED</span>
                  </div>
                  <p className="text-xs text-muted-foreground font-mono">{p.bookingId.substring(0, 16)}…</p>
                  <p className="text-xs text-slate-500 mt-0.5">{p.gatewayProvider} · {new Date(p.createdAt || '').toLocaleDateString()}</p>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Refund Form */}
        <div>
          <h2 className="text-sm font-bold uppercase tracking-widest text-muted-foreground mb-3">
            Request Refund
          </h2>
          {!selectedPayment ? (
            <div className="text-center py-10 border border-dashed border-border rounded-2xl bg-card">
              <p className="text-sm text-muted-foreground">Select a payment from the left to request a refund.</p>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="glass rounded-2xl p-5 border border-white/5 space-y-4">
              <div className="bg-card border border-border rounded-xl p-3 text-sm">
                <span className="text-muted-foreground text-xs block mb-1">Selected Payment</span>
                <span className="font-bold">{fmt(selectedPayment.amount, selectedPayment.currency)}</span>
                <span className="text-xs text-muted-foreground ml-2">· Max refundable</span>
              </div>

              {formError && (
                <div className="flex items-start gap-2 text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-xl p-3">
                  <AlertTriangle className="w-4 h-4 mt-0.5 shrink-0" />
                  {formError}
                </div>
              )}

              <div className="space-y-1">
                <label htmlFor="refund-amount" className="text-sm font-semibold">
                  Refund Amount ({selectedPayment.currency})
                </label>
                <input
                  id="refund-amount"
                  type="number"
                  min={1}
                  max={selectedPayment.amount}
                  step={0.01}
                  value={refundAmount}
                  onChange={e => setRefundAmount(e.target.value)}
                  placeholder={`Max: ${selectedPayment.amount}`}
                  className="w-full px-4 py-3 rounded-xl border border-border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                  required
                  aria-describedby="refund-amount-hint"
                />
                <p id="refund-amount-hint" className="text-xs text-muted-foreground">
                  Enter an amount between 1 and {fmt(selectedPayment.amount, selectedPayment.currency)}
                </p>
              </div>

              <div className="space-y-1">
                <label htmlFor="refund-reason" className="text-sm font-semibold">Reason (Optional)</label>
                <textarea
                  id="refund-reason"
                  value={refundReason}
                  onChange={e => setRefundReason(e.target.value)}
                  placeholder="Describe why you are requesting a refund..."
                  rows={4}
                  maxLength={500}
                  className="w-full px-4 py-3 rounded-xl border border-border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 resize-none"
                />
                <div className="text-right text-[10px] text-muted-foreground">{refundReason.length}/500</div>
              </div>

              <button
                type="submit"
                disabled={submitting}
                className="w-full py-3 rounded-xl font-bold text-sm text-white bg-gradient-to-r from-purple-600 to-indigo-600 hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-all flex items-center justify-center gap-2"
              >
                {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
                {submitting ? 'Submitting…' : 'Submit Refund Request'}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
