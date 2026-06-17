import React, { useState, useEffect } from 'react';
import { paymentService, WebhookDto } from '../services/paymentService';
import {
  Code, RefreshCw, AlertCircle, CheckCircle, HelpCircle, Terminal, Play, ChevronDown, ChevronUp, AlertTriangle
} from 'lucide-react';

export default function WebhookEventsPage() {
  const [webhooks, setWebhooks] = useState<WebhookDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  useEffect(() => {
    fetchWebhooks();
  }, []);

  const fetchWebhooks = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await paymentService.getAllWebhooks();
      setWebhooks(data);
    } catch (err) {
      setError('Could not load administrative webhook event logs.');
    } finally {
      setLoading(false);
    }
  };

  const handleRetry = async (id: string) => {
    try {
      await paymentService.retryWebhook(id);
      alert('Webhook event processing retried.');
      fetchWebhooks();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to retry webhook.');
    }
  };

  const toggleExpand = (id: string) => {
    setExpandedId(expandedId === id ? null : id);
  };

  const formatDate = (iso?: string) =>
    iso ? new Date(iso).toLocaleString() : '—';

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
            <Terminal className="w-8 h-8 text-indigo-400" />
            Gateway Webhook Events
          </h1>
          <p className="text-muted-foreground text-sm">
            Auditing webhook payloads received from external gateway providers (Stripe, Razorpay, Cashfree).
          </p>
        </div>
        <button
          onClick={fetchWebhooks}
          className="flex items-center gap-2 px-4 py-2 border border-border rounded-xl text-sm font-semibold hover:bg-card transition-all"
        >
          <RefreshCw className="w-4 h-4" />
          Refresh Webhooks
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
      ) : webhooks.length === 0 ? (
        <div className="text-center py-16 border border-dashed border-border rounded-2xl bg-card">
          <div className="text-5xl mb-4">🔌</div>
          <p className="font-semibold text-muted-foreground">No webhook events recorded yet.</p>
        </div>
      ) : (
        <div className="overflow-hidden rounded-2xl border border-border bg-card">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border text-muted-foreground text-xs uppercase tracking-wider text-left bg-slate-900/60">
                <th className="px-5 py-4 w-10"></th>
                <th className="px-5 py-4 font-semibold">Event ID</th>
                <th className="px-5 py-4 font-semibold">Provider</th>
                <th className="px-5 py-4 font-semibold">Event Type</th>
                <th className="px-5 py-4 font-semibold">Received At</th>
                <th className="px-5 py-4 font-semibold">Status</th>
                <th className="px-5 py-4 font-semibold text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {webhooks.map(w => {
                const isExpanded = expandedId === w.id;
                return (
                  <React.Fragment key={w.id}>
                    <tr
                      onClick={() => toggleExpand(w.id)}
                      className="hover:bg-slate-800/30 transition-all cursor-pointer"
                    >
                      <td className="px-5 py-4 text-center">
                        {isExpanded ? <ChevronUp className="w-4 h-4 text-slate-400" /> : <ChevronDown className="w-4 h-4 text-slate-400" />}
                      </td>
                      <td className="px-5 py-4">
                        <span className="font-mono text-xs text-indigo-300">{w.id.substring(0, 16)}...</span>
                      </td>
                      <td className="px-5 py-4 text-slate-300 font-semibold capitalize">
                        {w.gatewayProvider}
                      </td>
                      <td className="px-5 py-4 text-slate-400 font-mono text-xs">
                        {w.eventType}
                      </td>
                      <td className="px-5 py-4 text-muted-foreground">
                        {formatDate(w.createdAt)}
                      </td>
                      <td className="px-5 py-4">
                        <span className={`px-2.5 py-0.5 text-xs font-semibold rounded-full border ${
                          w.processed
                            ? 'bg-emerald-500/10 text-emerald-300 border-emerald-500/20'
                            : 'bg-red-500/10 text-red-300 border-red-500/20'
                        }`}>
                          {w.processed ? 'PROCESSED' : 'FAILED'}
                        </span>
                      </td>
                      <td className="px-5 py-4 text-right" onClick={e => e.stopPropagation()}>
                        <button
                          onClick={() => handleRetry(w.id)}
                          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold border border-indigo-500/30 text-indigo-300 rounded-lg hover:bg-indigo-500/10 transition-all ml-auto"
                        >
                          <Play className="w-3 h-3" />
                          Retry
                        </button>
                      </td>
                    </tr>
                    {isExpanded && (
                      <tr className="bg-slate-950/60">
                        <td colSpan={7} className="px-8 py-4 space-y-3">
                          {w.errorReason && (
                            <div className="p-3 bg-red-500/10 border border-red-500/20 text-red-300 text-xs rounded-xl flex items-start gap-2">
                              <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
                              <div>
                                <span className="font-bold">Error details:</span> {w.errorReason}
                              </div>
                            </div>
                          )}
                          <div className="space-y-1">
                            <span className="text-[10px] uppercase font-bold tracking-wider text-muted-foreground">Raw Webhook Payload JSON</span>
                            <pre className="p-4 bg-slate-900 border border-border/40 rounded-xl text-[11px] font-mono text-slate-300 overflow-x-auto max-w-full max-h-80">
                              {JSON.stringify(JSON.parse(w.payload || '{}'), null, 2)}
                            </pre>
                          </div>
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
