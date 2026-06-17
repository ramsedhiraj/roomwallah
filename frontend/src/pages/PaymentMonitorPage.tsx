import React, { useState, useEffect } from 'react';
import { paymentService, PaymentDto } from '../services/paymentService';
import {
  Activity, ShieldAlert, CheckCircle, XCircle, Search, RefreshCw, AlertTriangle, Play, Ban, Shield
} from 'lucide-react';

export default function PaymentMonitorPage() {
  const [payments, setPayments] = useState<PaymentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  useEffect(() => {
    fetchPayments();
  }, []);

  const fetchPayments = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await paymentService.getAllPayments();
      setPayments(data);
    } catch (err) {
      setError('Could not load administrative payment logs.');
    } finally {
      setLoading(false);
    }
  };

  const handleCapture = async (id: string) => {
    if (!window.confirm('Are you sure you want to manually authorize and capture this payment?')) return;
    try {
      await paymentService.capturePayment(id, `manual_${Date.now()}`);
      alert('Payment captured successfully.');
      fetchPayments();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to capture payment.');
    }
  };

  const handleFail = async (id: string) => {
    const reason = window.prompt('Specify the failure reason:');
    if (reason === null) return;
    try {
      await paymentService.failPayment(id, reason || 'Manual admin intervention');
      alert('Payment marked as failed.');
      fetchPayments();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to cancel payment.');
    }
  };

  const filtered = payments.filter(p => {
    const matchesSearch =
      p.id.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.bookingId.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.tenantId.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.ownerId.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || p.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

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
            <Activity className="w-8 h-8 text-indigo-400" />
            Real-time Payment Stream
          </h1>
          <p className="text-muted-foreground text-sm">
            Monitor transaction operations across all platform bookings.
          </p>
        </div>
        <button
          onClick={fetchPayments}
          className="flex items-center gap-2 px-4 py-2 border border-border rounded-xl text-sm font-semibold hover:bg-card transition-all"
        >
          <RefreshCw className="w-4 h-4" />
          Refresh Stream
        </button>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-xl p-4 flex items-center gap-3">
          <AlertTriangle className="w-5 h-5 shrink-0" />
          <span className="text-sm">{error}</span>
        </div>
      )}

      {/* Filter Toolbar */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="relative col-span-2">
          <Search className="absolute left-3 top-3 w-4 h-4 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search by Payment ID, Booking ID, Tenant, or Owner..."
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-card border border-border rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary"
          />
        </div>
        <div>
          <select
            value={statusFilter}
            onChange={e => setStatusFilter(e.target.value)}
            className="w-full px-4 py-2 bg-card border border-border rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="ALL">All Statuses</option>
            <option value="PENDING">PENDING</option>
            <option value="CAPTURED">CAPTURED</option>
            <option value="FAILED">FAILED</option>
            <option value="REFUNDED">REFUNDED</option>
          </select>
        </div>
      </div>

      {/* Main Table */}
      {loading ? (
        <div className="space-y-3">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="h-16 rounded-xl bg-card border border-border animate-pulse" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-16 border border-dashed border-border rounded-2xl bg-card">
          <div className="text-5xl mb-4">🔬</div>
          <p className="font-semibold text-muted-foreground">No payments match your criteria.</p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-border bg-card">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border text-muted-foreground text-xs uppercase tracking-wider text-left bg-slate-900/60">
                <th className="px-5 py-4 font-semibold">Payment ID</th>
                <th className="px-5 py-4 font-semibold">Booking ID</th>
                <th className="px-5 py-4 font-semibold">Amount</th>
                <th className="px-5 py-4 font-semibold">Gateway / ID</th>
                <th className="px-5 py-4 font-semibold">Risk Score</th>
                <th className="px-5 py-4 font-semibold">Status</th>
                <th className="px-5 py-4 font-semibold text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {filtered.map(p => (
                <tr key={p.id} className="hover:bg-slate-800/30 transition-all">
                  <td className="px-5 py-4">
                    <span className="font-mono text-xs text-indigo-300 block">{p.id.substring(0, 16)}...</span>
                  </td>
                  <td className="px-5 py-4">
                    <span className="font-mono text-xs text-slate-400 block">{p.bookingId.substring(0, 16)}...</span>
                  </td>
                  <td className="px-5 py-4 font-bold text-slate-100">
                    {fmt(p.amount, p.currency)}
                  </td>
                  <td className="px-5 py-4">
                    <div className="text-xs font-semibold capitalize text-slate-300">{p.gatewayProvider}</div>
                    <div className="text-[10px] font-mono text-slate-500">{p.gatewayPaymentId || '—'}</div>
                  </td>
                  <td className="px-5 py-4">
                    {p.riskScore !== undefined ? (
                      <div className="flex items-center gap-1.5">
                        <span className={`w-2 h-2 rounded-full ${p.riskScore > 65 ? 'bg-red-400 animate-ping' : p.riskScore > 35 ? 'bg-amber-400' : 'bg-emerald-400'}`} />
                        <span className={`font-semibold text-xs ${p.riskScore > 65 ? 'text-red-300 font-extrabold' : p.riskScore > 35 ? 'text-amber-300' : 'text-emerald-300'}`}>
                          {p.riskScore}% ({p.riskDecision || 'PASS'})
                        </span>
                      </div>
                    ) : (
                      <span className="text-xs text-slate-500">—</span>
                    )}
                  </td>
                  <td className="px-5 py-4">
                    <span className={`px-2.5 py-1 text-xs font-semibold rounded-full border ${
                      p.status === 'CAPTURED' ? 'bg-emerald-500/10 text-emerald-300 border-emerald-500/20' :
                      p.status === 'PENDING' ? 'bg-amber-500/10 text-amber-300 border-amber-500/20' :
                      p.status === 'REFUNDED' ? 'bg-purple-500/10 text-purple-300 border-purple-500/20' :
                      'bg-red-500/10 text-red-300 border-red-500/20'
                    }`}>
                      {p.status}
                    </span>
                  </td>
                  <td className="px-5 py-4">
                    <div className="flex justify-end items-center gap-2">
                      {p.status === 'PENDING' && (
                        <>
                          <button
                            onClick={() => handleCapture(p.id)}
                            className="flex items-center gap-1 px-3 py-1.5 text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg transition-all"
                          >
                            <Play className="w-3 h-3" />
                            Capture
                          </button>
                          <button
                            onClick={() => handleFail(p.id)}
                            className="flex items-center gap-1 px-3 py-1.5 text-xs font-semibold bg-red-600 hover:bg-red-500 text-white rounded-lg transition-all"
                          >
                            <Ban className="w-3 h-3" />
                            Fail
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
