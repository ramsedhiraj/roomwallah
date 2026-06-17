import React, { useState } from 'react';
import { apiClient } from '../services/api';
import {
  GitCompare, RefreshCw, AlertCircle, CheckCircle, HelpCircle, FileSpreadsheet, Play, Check
} from 'lucide-react';

export default function ReconciliationDashboardPage() {
  const [provider, setProvider] = useState<'STRIPE' | 'RAZORPAY' | 'CASHFREE'>('STRIPE');
  const [loading, setLoading] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Prefill mock gateway records for demonstration
  const [recordsText, setRecordsText] = useState<string>(
    JSON.stringify(
      [
        {
          gatewayTransactionId: "ch_stripe_mock_12345",
          amount: 10200.0,
          status: "SUCCESS"
        },
        {
          gatewayTransactionId: "pay_razor_mock_67890",
          amount: 5100.0,
          status: "SUCCESS"
        }
      ],
      null,
      2
    )
  );

  const handleReconcile = async () => {
    setLoading(true);
    setSuccessMsg(null);
    setErrorMsg(null);
    try {
      let parsed = [];
      try {
        parsed = JSON.parse(recordsText);
      } catch (e) {
        throw new Error("Invalid JSON format in gateway records.");
      }

      await apiClient.post('/admin/payments/reconcile', {
        gatewayProvider: provider,
        records: parsed
      });

      setSuccessMsg(`Successfully executed reconciliation batch for ${provider}. Any mismatches have been logged as system incidents.`);
    } catch (err: any) {
      setErrorMsg(err.message || err.response?.data?.message || 'Reconciliation execution failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-8 space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
          <GitCompare className="w-8 h-8 text-violet-400" />
          Reconciliation Console
        </h1>
        <p className="text-muted-foreground text-sm">
          Run double-entry ledger audits against external gateway records (Stripe, Razorpay, Cashfree).
        </p>
      </div>

      {successMsg && (
        <div className="bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 rounded-xl p-4 flex items-center gap-3">
          <CheckCircle className="w-5 h-5 shrink-0" />
          <span className="text-sm">{successMsg}</span>
        </div>
      )}

      {errorMsg && (
        <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-xl p-4 flex items-center gap-3">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span className="text-sm">{errorMsg}</span>
        </div>
      )}

      {/* Main Form */}
      <div className="glass rounded-2xl p-6 border border-white/5 space-y-5">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-2">
              Gateway Provider
            </label>
            <select
              value={provider}
              onChange={e => setProvider(e.target.value as any)}
              className="w-full px-4 py-2.5 bg-card border border-border rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="STRIPE">Stripe</option>
              <option value="RAZORPAY">Razorpay</option>
              <option value="CASHFREE">Cashfree</option>
            </select>
          </div>
          <div className="flex items-end">
            <div className="flex gap-2 items-center text-xs text-muted-foreground p-3 border border-border/40 rounded-xl bg-slate-900/40">
              <HelpCircle className="w-4 h-4 text-indigo-400 shrink-0" />
              <span>
                Matching validates database payment ledger records against external transactions to flag discrepant amounts or status mismatches.
              </span>
            </div>
          </div>
        </div>

        <div>
          <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-2">
            Gateway Transactions (JSON List)
          </label>
          <textarea
            value={recordsText}
            onChange={e => setRecordsText(e.target.value)}
            rows={10}
            className="w-full p-4 bg-slate-950 font-mono text-xs text-slate-300 border border-border rounded-xl focus:outline-none focus:ring-2 focus:ring-primary"
            placeholder="[ { 'gatewayTransactionId': '...', 'amount': 100.00, 'status': 'SUCCESS' } ]"
          />
        </div>

        <button
          onClick={handleReconcile}
          disabled={loading}
          className="w-full py-3 bg-gradient-to-r from-indigo-600 to-violet-600 hover:opacity-90 disabled:opacity-50 text-white font-bold rounded-xl transition-all flex items-center justify-center gap-2"
        >
          {loading ? (
            <>
              <RefreshCw className="w-4 h-4 animate-spin" />
              Running Reconciler...
            </>
          ) : (
            <>
              <Play className="w-4 h-4" />
              Trigger Matching & Reconciliation
            </>
          )}
        </button>
      </div>

      {/* Guide/Audit info */}
      <div className="glass rounded-2xl p-6 border border-white/5 space-y-3">
        <h3 className="font-semibold text-sm flex items-center gap-2">
          <FileSpreadsheet className="w-4 h-4 text-violet-400" />
          Reconciliation Workflow Guide
        </h3>
        <ul className="text-xs text-muted-foreground list-disc pl-5 space-y-2">
          <li><strong>Append-only ledger entries</strong>: Once matched, any manual reconciliation adjustments will post fresh entries, keeping the ledger records append-only.</li>
          <li><strong>Discrepancies Alerts</strong>: Any mismatched logs are written into standard system incident traces viewable under the Audit center.</li>
        </ul>
      </div>
    </div>
  );
}
