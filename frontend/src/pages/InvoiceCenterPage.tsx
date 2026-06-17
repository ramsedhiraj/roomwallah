import React, { useState, useEffect } from 'react';
import { paymentService, InvoiceDto } from '../services/paymentService';
import InvoiceCard from '../components/InvoiceCard';
import { FileText, RefreshCw, AlertCircle, Search } from 'lucide-react';

export default function InvoiceCenterPage() {
  const [invoices, setInvoices] = useState<InvoiceDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');

  useEffect(() => { fetchInvoices(); }, []);

  const fetchInvoices = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await paymentService.getMyInvoices();
      setInvoices(data.sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()));
    } catch {
      setError('Could not load your invoices. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const filtered = invoices.filter(inv =>
    inv.invoiceNumber.toLowerCase().includes(search.toLowerCase()) ||
    inv.bookingId.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
            <FileText className="w-7 h-7 text-indigo-400" />
            Invoice Center
          </h1>
          <p className="text-muted-foreground mt-1 text-sm">Download receipts and refund confirmations.</p>
        </div>
        <button
          onClick={fetchInvoices}
          className="flex items-center gap-2 px-4 py-2 border border-border rounded-xl text-sm font-semibold hover:bg-card transition-all"
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

      {/* Search */}
      {!loading && invoices.length > 0 && (
        <div className="relative mb-6">
          <Search className="absolute left-3.5 top-3 w-4 h-4 text-muted-foreground" aria-hidden="true" />
          <input
            type="text"
            placeholder="Search by invoice number or booking ID…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 border border-border bg-card rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            aria-label="Search invoices"
          />
        </div>
      )}

      {/* Content */}
      {loading ? (
        <div className="space-y-4">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-24 rounded-2xl bg-card border border-border animate-pulse" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-16 border border-dashed border-border rounded-2xl bg-card">
          <div className="text-5xl mb-4">📄</div>
          <p className="font-semibold text-muted-foreground">
            {invoices.length === 0 ? 'No invoices yet' : 'No invoices match your search'}
          </p>
          <p className="text-xs text-muted-foreground mt-1">
            {invoices.length === 0
              ? 'Invoices are generated automatically after successful payments.'
              : 'Try a different search term.'}
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {/* Summary bar */}
          <div className="flex items-center justify-between px-1 mb-2">
            <span className="text-xs text-muted-foreground">{filtered.length} invoice{filtered.length !== 1 ? 's' : ''}</span>
            <span className="text-xs text-muted-foreground">
              Total: <span className="font-bold text-foreground">
                ₹{filtered.reduce((sum, i) => sum + i.amount, 0).toLocaleString('en-IN')}
              </span>
            </span>
          </div>
          {filtered.map(inv => <InvoiceCard key={inv.id} invoice={inv} />)}
        </div>
      )}
    </div>
  );
}
