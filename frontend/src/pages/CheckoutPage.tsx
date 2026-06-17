import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { paymentService, PaymentDto } from '../services/paymentService';
import { apiClient } from '../services/api';
import {
  CreditCard, Zap, Shield, ChevronLeft, Loader2, AlertTriangle, CheckCircle2, Receipt
} from 'lucide-react';

type PaymentStage = 'IDLE' | 'PROCESSING' | 'SUCCESS' | 'ERROR';

const GATEWAYS = [
  {
    id: 'STRIPE' as const,
    name: 'Stripe',
    tagline: 'International cards & wallets',
    color: 'from-indigo-600 to-purple-600',
    border: 'border-indigo-500/40',
    icon: '💳',
  },
  {
    id: 'RAZORPAY' as const,
    name: 'Razorpay',
    tagline: 'UPI, NetBanking, Cards',
    color: 'from-blue-600 to-cyan-600',
    border: 'border-blue-500/40',
    icon: '⚡',
  },
  {
    id: 'CASHFREE' as const,
    name: 'Cashfree',
    tagline: 'Instant bank transfers',
    color: 'from-emerald-600 to-teal-600',
    border: 'border-emerald-500/40',
    icon: '🏦',
  },
];

export default function CheckoutPage() {
  const { bookingId } = useParams<{ bookingId: string }>();
  const navigate = useNavigate();

  const [booking, setBooking] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [stage, setStage] = useState<PaymentStage>('IDLE');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [payment, setPayment] = useState<PaymentDto | null>(null);
  const [selectedGateway, setSelectedGateway] = useState<'STRIPE' | 'RAZORPAY' | 'CASHFREE'>('RAZORPAY');
  const [showConfetti, setShowConfetti] = useState(false);

  useEffect(() => {
    if (!bookingId) return;
    setLoading(true);
    apiClient.get(`/bookings/${bookingId}`)
      .then(res => setBooking(res.data.data))
      .catch(() => setBooking(null))
      .finally(() => setLoading(false));
  }, [bookingId]);

  const amount = booking?.priceAmount || 0;
  const currency = booking?.priceCurrency || 'INR';
  const platformFee = Math.round(amount * 0.02);
  const gst = Math.round(platformFee * 0.18);
  const total = amount + platformFee + gst;
  const fmt = (n: number) => `${currency === 'INR' ? '₹' : '$'}${n.toLocaleString('en-IN')}`;

  const handlePayNow = async () => {
    if (!bookingId) return;
    setStage('PROCESSING');
    setErrorMsg(null);
    try {
      const idempotencyKey = `pay_${bookingId}_${Date.now()}`;
      const initiated = await paymentService.initiatePayment({
        bookingId,
        amount: total,
        currency,
        gatewayProvider: selectedGateway,
        idempotencyKey,
      });
      // Simulate gateway flow: in production, redirect to gateway
      const captured = await paymentService.capturePayment(initiated.id, `gw_${Date.now()}`);
      setPayment(captured);
      setStage('SUCCESS');
      setShowConfetti(true);
      setTimeout(() => setShowConfetti(false), 4000);
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || 'Payment failed. Please try again.');
      setStage('ERROR');
    }
  };

  if (loading) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-12 space-y-4">
        {[1, 2, 3].map(i => (
          <div key={i} className="h-20 rounded-2xl bg-card border border-border animate-pulse" />
        ))}
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 relative">
      {/* CSS Confetti */}
      {showConfetti && (
        <div className="fixed inset-0 pointer-events-none z-50 overflow-hidden" aria-hidden="true">
          {Array.from({ length: 40 }).map((_, i) => (
            <div
              key={i}
              className="absolute w-2 h-2 rounded-sm animate-bounce"
              style={{
                left: `${Math.random() * 100}%`,
                top: `${Math.random() * 40}%`,
                backgroundColor: ['#6366f1', '#a855f7', '#ec4899', '#10b981', '#f59e0b', '#3b82f6'][i % 6],
                animationDelay: `${Math.random() * 1}s`,
                animationDuration: `${0.6 + Math.random() * 0.8}s`,
                transform: `rotate(${Math.random() * 360}deg)`,
              }}
            />
          ))}
        </div>
      )}

      {/* Back */}
      <button
        onClick={() => navigate(-1)}
        className="flex items-center text-sm text-muted-foreground hover:text-foreground transition-colors mb-6"
        aria-label="Go back"
      >
        <ChevronLeft className="w-4 h-4 mr-1" />
        Back
      </button>

      {/* SUCCESS STATE */}
      {stage === 'SUCCESS' && payment ? (
        <div className="glass rounded-3xl p-10 text-center space-y-4 border border-emerald-500/20 glow-indigo">
          <div className="flex justify-center">
            <div className="w-20 h-20 rounded-full bg-emerald-500/10 border-2 border-emerald-400 flex items-center justify-center animate-scale-in">
              <CheckCircle2 className="w-10 h-10 text-emerald-400" />
            </div>
          </div>
          <h2 className="text-3xl font-extrabold">Payment Successful!</h2>
          <p className="text-muted-foreground text-sm">
            Your payment of <span className="font-bold text-foreground">{fmt(payment.amount)}</span> via{' '}
            <span className="font-bold text-indigo-300">{payment.gatewayProvider}</span> has been confirmed.
          </p>
          <p className="text-xs font-mono text-slate-500">Ref: {payment.id}</p>
          <div className="flex flex-col sm:flex-row gap-3 justify-center pt-2">
            <button
              onClick={() => navigate('/payments/invoices')}
              className="flex items-center gap-2 px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold rounded-xl text-sm transition-all"
            >
              <Receipt className="w-4 h-4" />
              View Invoice
            </button>
            <button
              onClick={() => navigate('/bookings')}
              className="px-5 py-2.5 border border-border rounded-xl text-sm font-semibold hover:bg-card transition-all"
            >
              Back to Bookings
            </button>
          </div>
        </div>
      ) : (
        <>
          <div className="mb-6">
            <h1 className="text-3xl font-extrabold tracking-tight">Complete Your Payment</h1>
            <p className="text-muted-foreground mt-1 text-sm">Secure checkout powered by industry-grade encryption.</p>
          </div>

          {/* Error Banner */}
          {stage === 'ERROR' && errorMsg && (
            <div className="mb-5 bg-destructive/10 border border-destructive/30 text-destructive rounded-xl p-4 flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-sm">Payment Failed</p>
                <p className="text-xs mt-0.5">{errorMsg}</p>
              </div>
            </div>
          )}

          {/* Booking Summary */}
          {booking && (
            <div className="glass rounded-2xl p-5 mb-5 border border-white/5">
              <p className="text-xs font-bold uppercase tracking-widest text-muted-foreground mb-3">Booking Summary</p>
              <div className="flex justify-between text-sm mb-1">
                <span className="text-muted-foreground">Booking ID</span>
                <span className="font-mono font-semibold text-xs">{booking.id.substring(0, 16)}…</span>
              </div>
              <div className="flex justify-between text-sm mb-1">
                <span className="text-muted-foreground">Rent Amount</span>
                <span className="font-semibold">{fmt(amount)}</span>
              </div>
              <div className="flex justify-between text-sm mb-1">
                <span className="text-muted-foreground">Platform Fee (2%)</span>
                <span className="font-semibold">{fmt(platformFee)}</span>
              </div>
              <div className="flex justify-between text-sm mb-1">
                <span className="text-muted-foreground">GST on Fee (18%)</span>
                <span className="font-semibold">{fmt(gst)}</span>
              </div>
              <div className="border-t border-border my-3" />
              <div className="flex justify-between font-bold text-lg">
                <span>Total</span>
                <span className="text-indigo-300">{fmt(total)}</span>
              </div>
            </div>
          )}

          {/* Gateway Selector */}
          <div className="glass rounded-2xl p-5 mb-5 border border-white/5">
            <p className="text-xs font-bold uppercase tracking-widest text-muted-foreground mb-3">Select Payment Method</p>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {GATEWAYS.map(gw => (
                <button
                  key={gw.id}
                  onClick={() => setSelectedGateway(gw.id)}
                  className={`relative p-4 rounded-xl border-2 text-left transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-primary ${
                    selectedGateway === gw.id
                      ? `${gw.border} bg-gradient-to-br ${gw.color} bg-opacity-10 shadow-lg`
                      : 'border-border hover:border-slate-500 bg-card'
                  }`}
                  aria-pressed={selectedGateway === gw.id}
                  aria-label={`Select ${gw.name}`}
                >
                  {selectedGateway === gw.id && (
                    <div className="absolute top-2 right-2 w-4 h-4 rounded-full bg-emerald-400 flex items-center justify-center">
                      <CheckCircle2 className="w-3 h-3 text-white" />
                    </div>
                  )}
                  <div className="text-2xl mb-1">{gw.icon}</div>
                  <div className="font-bold text-sm">{gw.name}</div>
                  <div className="text-[10px] text-muted-foreground">{gw.tagline}</div>
                </button>
              ))}
            </div>
          </div>

          {/* Security note */}
          <div className="flex items-center gap-2 text-xs text-muted-foreground mb-6">
            <Shield className="w-4 h-4 text-emerald-400" />
            <span>Your payment is secured with 256-bit TLS encryption. Funds held in escrow until move-in.</span>
          </div>

          {/* Pay Button */}
          <button
            onClick={handlePayNow}
            disabled={stage === 'PROCESSING'}
            className="w-full py-4 rounded-2xl font-bold text-lg text-white bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-lg flex items-center justify-center gap-3 focus:outline-none focus:ring-4 focus:ring-indigo-500/40"
            aria-label="Complete payment"
          >
            {stage === 'PROCESSING' ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                Processing…
              </>
            ) : (
              <>
                <Zap className="w-5 h-5" />
                Pay {booking ? fmt(total) : '…'} Now
                <CreditCard className="w-5 h-5" />
              </>
            )}
          </button>
        </>
      )}
    </div>
  );
}
