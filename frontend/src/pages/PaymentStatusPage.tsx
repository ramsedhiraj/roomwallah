import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { paymentService, PaymentDto } from '../services/paymentService';
import { CheckCircle2, XCircle, Download, RefreshCw, ArrowLeft, Loader2 } from 'lucide-react';

export default function PaymentStatusPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const status = params.get('status') as 'success' | 'failed' | null;
  const paymentId = params.get('paymentId');

  const [payment, setPayment] = useState<PaymentDto | null>(null);
  const [loading, setLoading] = useState(!!paymentId);

  useEffect(() => {
    if (!paymentId) return;
    setLoading(true);
    paymentService.getPayment(paymentId)
      .then(setPayment)
      .catch(() => setPayment(null))
      .finally(() => setLoading(false));
  }, [paymentId]);

  const fmt = (n: number, currency: string) =>
    `${currency === 'INR' ? '₹' : '$'}${n.toLocaleString('en-IN')}`;

  const formatDate = (iso?: string) =>
    iso
      ? new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
      : 'N/A';

  const isSuccess = status === 'success' || payment?.status === 'CAPTURED';

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <Loader2 className="w-8 h-8 text-indigo-400 animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-lg mx-auto px-4 py-12">
      <div className="glass rounded-3xl p-10 text-center border border-white/5 glow-indigo">
        {/* Animated Status Icon */}
        <div className="flex justify-center mb-6">
          {isSuccess ? (
            <div className="relative">
              <svg width="96" height="96" viewBox="0 0 96 96" aria-hidden="true">
                <circle
                  cx="48" cy="48" r="44"
                  fill="none"
                  stroke="#10b981"
                  strokeWidth="4"
                  strokeDasharray="276"
                  strokeDashoffset="0"
                  className="animate-[draw_0.6s_ease-out_forwards]"
                />
                <path
                  d="M28 50l14 14 26-28"
                  fill="none"
                  stroke="#10b981"
                  strokeWidth="5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  className="animate-[draw_0.4s_0.5s_ease-out_forwards]"
                />
              </svg>
            </div>
          ) : (
            <div className="relative">
              <svg width="96" height="96" viewBox="0 0 96 96" aria-hidden="true">
                <circle
                  cx="48" cy="48" r="44"
                  fill="none"
                  stroke="#ef4444"
                  strokeWidth="4"
                />
                <path
                  d="M32 32l32 32M64 32l-32 32"
                  fill="none"
                  stroke="#ef4444"
                  strokeWidth="5"
                  strokeLinecap="round"
                />
              </svg>
            </div>
          )}
        </div>

        <h1 className={`text-3xl font-extrabold mb-2 ${isSuccess ? 'text-emerald-400' : 'text-red-400'}`}>
          {isSuccess ? 'Payment Successful' : 'Payment Failed'}
        </h1>

        <p className="text-muted-foreground text-sm mb-6">
          {isSuccess
            ? 'Your funds have been securely held in escrow. You will be notified when the owner confirms.'
            : 'Your payment could not be processed. No amount has been charged.'}
        </p>

        {/* Payment Details */}
        {payment && (
          <div className="bg-card border border-border rounded-2xl p-4 text-left space-y-3 mb-6 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Amount</span>
              <span className="font-bold">{fmt(payment.amount, payment.currency)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Gateway</span>
              <span className="font-semibold capitalize">{payment.gatewayProvider}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Status</span>
              <span className={`font-bold ${
                payment.status === 'CAPTURED' ? 'text-emerald-400' :
                payment.status === 'FAILED' ? 'text-red-400' : 'text-amber-400'
              }`}>{payment.status}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Date</span>
              <span className="font-semibold">{formatDate(payment.createdAt)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Reference</span>
              <span className="font-mono text-xs text-slate-400">{payment.id.substring(0, 20)}…</span>
            </div>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex flex-col gap-3">
          {isSuccess ? (
            <>
              <a
                href={payment ? paymentService.getInvoicePdfUrl(payment.id) : '#'}
                className="flex items-center justify-center gap-2 w-full py-3 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold rounded-xl text-sm transition-all"
                aria-label="Download invoice"
                target="_blank"
                rel="noopener noreferrer"
              >
                <Download className="w-4 h-4" />
                Download Invoice
              </a>
              <Link
                to="/bookings"
                className="flex items-center justify-center gap-2 w-full py-3 border border-border rounded-xl text-sm font-semibold hover:bg-card transition-all"
              >
                <ArrowLeft className="w-4 h-4" />
                Back to Bookings
              </Link>
            </>
          ) : (
            <>
              <button
                onClick={() => navigate(-2)}
                className="flex items-center justify-center gap-2 w-full py-3 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold rounded-xl text-sm transition-all"
              >
                <RefreshCw className="w-4 h-4" />
                Retry Payment
              </button>
              <Link
                to="/bookings"
                className="flex items-center justify-center gap-2 w-full py-3 border border-border rounded-xl text-sm font-semibold hover:bg-card transition-all"
              >
                <ArrowLeft className="w-4 h-4" />
                Back to Bookings
              </Link>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
