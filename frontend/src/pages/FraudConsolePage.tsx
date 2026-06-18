import React, { useState, useEffect } from 'react';
import { paymentService, PaymentDto } from '../services/paymentService';
import {
  ShieldAlert, RefreshCw, AlertTriangle, Shield, Globe, Users, Clock
} from 'lucide-react';

export default function FraudConsolePage() {
  const [payments, setPayments] = useState<PaymentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await paymentService.getAllPayments();
      // Only keep payments with some risk signals evaluated
      setPayments(data.filter(p => p.riskScore !== undefined && p.riskScore > 0));
    } catch (err) {
      setError('Failed to fetch fraud evaluation log.');
    } finally {
      setLoading(false);
    }
  };

  const highRisk = payments.filter(p => p.riskScore && p.riskScore > 65);
  const mediumRisk = payments.filter(p => p.riskScore && p.riskScore > 30 && p.riskScore <= 65);

  const fmt = (amount: number, cur?: string) => {
    const c = cur || 'INR';
    return `${c === 'INR' ? '₹' : '$'}${amount.toLocaleString('en-IN')}`;
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
            <ShieldAlert className="w-8 h-8 text-rose-500 animate-pulse" />
            Fraud & Risk Console
          </h1>
          <p className="text-muted-foreground text-sm">
            Platform risk assessment, velocity checks, and geo-ip mismatch alerts.
          </p>
        </div>
        <button
          onClick={fetchData}
          className="flex items-center gap-2 px-4 py-2 border border-border rounded-xl text-sm font-semibold hover:bg-card transition-all"
        >
          <RefreshCw className="w-4 h-4" />
          Scan Risks
        </button>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-xl p-4 flex items-center gap-3">
          <AlertTriangle className="w-5 h-5 shrink-0" />
          <span className="text-sm">{error}</span>
        </div>
      )}

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <div className="glass rounded-2xl p-6 border-l-4 border-l-rose-500 shadow-md">
          <div className="text-xs font-bold uppercase tracking-widest text-muted-foreground">High Risk Incidents</div>
          <p className="text-4xl font-extrabold mt-2 text-rose-400">{highRisk.length}</p>
          <p className="text-xs text-muted-foreground mt-2">Score greater than 65%. Requires immediate review.</p>
        </div>
        <div className="glass rounded-2xl p-6 border-l-4 border-l-amber-500 shadow-md">
          <div className="text-xs font-bold uppercase tracking-widest text-muted-foreground">Medium Risk Incidents</div>
          <p className="text-4xl font-extrabold mt-2 text-amber-400">{mediumRisk.length}</p>
          <p className="text-xs text-muted-foreground mt-2">Score 31% - 65%. Flagged for auditing.</p>
        </div>
        <div className="glass rounded-2xl p-6 border-l-4 border-l-emerald-500 shadow-md">
          <div className="text-xs font-bold uppercase tracking-widest text-muted-foreground">Risk Decision Ratio</div>
          <p className="text-4xl font-extrabold mt-2 text-emerald-400">
            {payments.length > 0 ? `${Math.round(((payments.length - highRisk.length) / payments.length) * 100)}%` : '100%'}
          </p>
          <p className="text-xs text-muted-foreground mt-2">Percentage of clean, authorized transactions.</p>
        </div>
      </div>

      {/* Rules Engine Configuration Overview */}
      <div className="glass rounded-2xl p-6 border border-white/5 space-y-4">
        <h2 className="text-lg font-bold">Active Risk Evaluation Rules</h2>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-card/50 border border-border p-4 rounded-xl flex items-start gap-3">
            <Globe className="w-5 h-5 text-indigo-400 mt-0.5" />
            <div>
              <div className="text-sm font-semibold">Geo-IP Location Mismatch</div>
              <p className="text-xs text-muted-foreground mt-1">Triggers if client country differs from billing address country (+40% Risk).</p>
            </div>
          </div>
          <div className="bg-card/50 border border-border p-4 rounded-xl flex items-start gap-3">
            <Clock className="w-5 h-5 text-purple-400 mt-0.5" />
            <div>
              <div className="text-sm font-semibold">Velocity Rate Limits</div>
              <p className="text-xs text-muted-foreground mt-1">More than 3 checkout attempts per tenant ID within 15 minutes (+25% Risk).</p>
            </div>
          </div>
          <div className="bg-card/50 border border-border p-4 rounded-xl flex items-start gap-3">
            <Users className="w-5 h-5 text-pink-400 mt-0.5" />
            <div>
              <div className="text-sm font-semibold">Multi-account Card Use</div>
              <p className="text-xs text-muted-foreground mt-1">Same card identifier used across multiple tenant accounts (+50% Risk).</p>
            </div>
          </div>
        </div>
      </div>

      {/* Flagged Incidents Stream */}
      <div className="glass rounded-2xl p-6 border border-white/5 space-y-4">
        <h2 className="text-lg font-bold">Risk Assessment Queue</h2>
        {loading ? (
          <div className="space-y-3">
            {[1, 2].map(i => (
              <div key={i} className="h-16 rounded-xl bg-card border border-border animate-pulse" />
            ))}
          </div>
        ) : payments.length === 0 ? (
          <div className="text-center py-12 text-muted-foreground">
            <Shield className="w-12 h-12 mx-auto text-emerald-400 mb-2" />
            <p className="font-semibold text-slate-300">No active risk flags detected on the platform.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {payments.map(p => {
              const isHigh = p.riskScore !== undefined && p.riskScore > 65;
              return (
                <div
                  key={p.id}
                  className={`p-4 rounded-xl border flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 bg-card ${
                    isHigh ? 'border-rose-500/30 hover:border-rose-500/50' : 'border-amber-500/30 hover:border-amber-500/50'
                  }`}
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className={`text-xs font-bold px-2 py-0.5 rounded-full ${isHigh ? 'bg-rose-500/20 text-rose-300' : 'bg-amber-500/20 text-amber-300'}`}>
                        {p.riskDecision}
                      </span>
                      <span className="text-xs font-semibold text-slate-400">Payment: {p.id.substring(0, 16)}...</span>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      Booking: {p.bookingId} · Amount: <span className="font-semibold text-slate-100">{fmt(p.amount, p.currency)}</span>
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="text-right">
                      <div className={`text-lg font-extrabold ${isHigh ? 'text-rose-400' : 'text-amber-400'}`}>
                        {p.riskScore}%
                      </div>
                      <div className="text-[10px] text-muted-foreground">Risk Score</div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
