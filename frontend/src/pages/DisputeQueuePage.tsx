import React, { useState, useEffect } from 'react';
import { paymentService, DisputeDto } from '../services/paymentService';
import {
  Gavel, RefreshCw, AlertCircle, CheckCircle, Scale, Shield, FileText, ChevronDown, ChevronUp, AlertTriangle, Send
} from 'lucide-react';
import { apiClient } from '../services/api';

export default function DisputeQueuePage() {
  const [disputes, setDisputes] = useState<DisputeDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  // File upload evidence states
  const [evidenceText, setEvidenceText] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    fetchDisputes();
  }, []);

  const fetchDisputes = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await paymentService.getAllDisputes();
      setDisputes(data);
    } catch (err) {
      setError('Could not load administrative dispute logs.');
    } finally {
      setLoading(false);
    }
  };

  const handleResolveDispute = async (disputeId: string, status: 'WON' | 'LOST') => {
    if (!window.confirm(`Are you sure you want to mark this dispute as ${status}?`)) return;
    try {
      await apiClient.post(`/admin/payments/disputes/${disputeId}/resolve`, { status });
      alert('Dispute status resolved.');
      fetchDisputes();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to resolve dispute.');
    }
  };

  const handleSubmitEvidence = async (disputeId: string) => {
    if (!evidenceText.trim()) return;
    setIsSubmitting(true);
    try {
      await apiClient.post(`/admin/payments/disputes/${disputeId}/evidence`, {
        evidence: evidenceText
      });
      alert('Evidence uploaded successfully.');
      setEvidenceText('');
      fetchDisputes();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to submit evidence.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const toggleExpand = (id: string) => {
    setExpandedId(expandedId === id ? null : id);
  };

  const fmt = (amount: number, cur?: string) => {
    const c = cur || 'INR';
    return `${c === 'INR' ? '₹' : '$'}${amount.toLocaleString('en-IN')}`;
  };

  const formatDate = (iso?: string) =>
    iso ? new Date(iso).toLocaleDateString() : '—';

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
            <Scale className="w-8 h-8 text-indigo-400" />
            Chargeback Dispute Console
          </h1>
          <p className="text-muted-foreground text-sm">
            Auditing chargeback disputes raised by financial institutions and cardholders.
          </p>
        </div>
        <button
          onClick={fetchDisputes}
          className="flex items-center gap-2 px-4 py-2 border border-border rounded-xl text-sm font-semibold hover:bg-card transition-all"
        >
          <RefreshCw className="w-4 h-4" />
          Refresh Queue
        </button>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-xl p-4 flex items-center gap-3">
          <AlertTriangle className="w-5 h-5 shrink-0" />
          <span className="text-sm">{error}</span>
        </div>
      )}

      {/* Main Table */}
      {loading ? (
        <div className="space-y-3">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-16 rounded-xl bg-card border border-border animate-pulse" />
          ))}
        </div>
      ) : disputes.length === 0 ? (
        <div className="text-center py-16 border border-dashed border-border rounded-2xl bg-card">
          <div className="text-5xl mb-4">⚖️</div>
          <p className="font-semibold text-muted-foreground">No active dispute claims filed.</p>
        </div>
      ) : (
        <div className="overflow-hidden rounded-2xl border border-border bg-card">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border text-muted-foreground text-xs uppercase tracking-wider text-left bg-slate-900/60">
                <th className="px-5 py-4 w-10"></th>
                <th className="px-5 py-4 font-semibold">Dispute ID</th>
                <th className="px-5 py-4 font-semibold">Payment ID</th>
                <th className="px-5 py-4 font-semibold">Amount</th>
                <th className="px-5 py-4 font-semibold">Reason</th>
                <th className="px-5 py-4 font-semibold">Filed At</th>
                <th className="px-5 py-4 font-semibold">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {disputes.map(d => {
                const isExpanded = expandedId === d.id;
                return (
                  <React.Fragment key={d.id}>
                    <tr
                      onClick={() => toggleExpand(d.id)}
                      className="hover:bg-slate-800/30 transition-all cursor-pointer"
                    >
                      <td className="px-5 py-4 text-center">
                        {isExpanded ? <ChevronUp className="w-4 h-4 text-slate-400" /> : <ChevronDown className="w-4 h-4 text-slate-400" />}
                      </td>
                      <td className="px-5 py-4">
                        <span className="font-mono text-xs text-indigo-300">{d.id.substring(0, 16)}...</span>
                      </td>
                      <td className="px-5 py-4">
                        <span className="font-mono text-xs text-slate-400">{d.paymentId.substring(0, 16)}...</span>
                      </td>
                      <td className="px-5 py-4 font-bold text-slate-100">
                        {fmt(d.amount, d.currency)}
                      </td>
                      <td className="px-5 py-4 text-slate-400 capitalize">
                        {d.reason}
                      </td>
                      <td className="px-5 py-4 text-muted-foreground">
                        {formatDate(d.createdAt)}
                      </td>
                      <td className="px-5 py-4">
                        <span className={`px-2.5 py-0.5 text-xs font-semibold rounded-full border ${
                          d.status === 'UNDER_REVIEW' || d.status === 'OPEN' ? 'bg-amber-500/10 text-amber-300 border-amber-500/20' :
                          d.status === 'WON' ? 'bg-emerald-500/10 text-emerald-300 border-emerald-500/20' :
                          'bg-red-500/10 text-red-300 border-red-500/20'
                        }`}>
                          {d.status}
                        </span>
                      </td>
                    </tr>
                    {isExpanded && (
                      <tr className="bg-slate-950/60">
                        <td colSpan={7} className="px-8 py-4 space-y-4">
                          {/* Evidence list */}
                          {d.evidenceJson && (
                            <div className="space-y-1">
                              <span className="text-[10px] uppercase font-bold tracking-wider text-muted-foreground">Disputed Evidence Details</span>
                              <pre className="p-4 bg-slate-900 border border-border/40 rounded-xl text-xs text-slate-300 max-h-40 overflow-y-auto">
                                {d.evidenceJson}
                              </pre>
                            </div>
                          )}

                          {/* Submit evidence text */}
                          {(d.status === 'UNDER_REVIEW' || d.status === 'OPEN') && (
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 pt-2">
                              <div className="space-y-2">
                                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
                                  Upload/Submit Evidence Documents
                                </label>
                                <div className="flex gap-2">
                                  <textarea
                                    value={evidenceText}
                                    onChange={e => setEvidenceText(e.target.value)}
                                    rows={3}
                                    placeholder="Enter details, upload references, transaction trace details..."
                                    className="flex-1 p-3 bg-slate-900 border border-border/40 rounded-xl text-xs text-slate-300 focus:outline-none focus:ring-2 focus:ring-primary"
                                  />
                                  <button
                                    onClick={() => handleSubmitEvidence(d.id)}
                                    disabled={isSubmitting || !evidenceText.trim()}
                                    className="px-4 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-xl flex items-center justify-center transition-all"
                                  >
                                    <Send className="w-4 h-4" />
                                  </button>
                                </div>
                              </div>
                              <div className="space-y-2">
                                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
                                  Final Arbitration
                                </label>
                                <div className="flex gap-2">
                                  <button
                                    onClick={() => handleResolveDispute(d.id, 'WON')}
                                    className="flex-1 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-xl transition-all"
                                  >
                                    Resolve as WON (Keep Funds)
                                  </button>
                                  <button
                                    onClick={() => handleResolveDispute(d.id, 'LOST')}
                                    className="flex-1 py-2.5 bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold rounded-xl transition-all"
                                  >
                                    Resolve as LOST (Chargeback)
                                  </button>
                                </div>
                              </div>
                            </div>
                          )}
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
