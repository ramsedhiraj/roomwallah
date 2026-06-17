import React from 'react';
import { Download, FileText } from 'lucide-react';
import { InvoiceDto, paymentService } from '../services/paymentService';

interface InvoiceCardProps {
  invoice: InvoiceDto;
}

export default function InvoiceCard({ invoice }: InvoiceCardProps) {
  const formatDate = (iso?: string) =>
    iso
      ? new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
      : 'N/A';

  const formatAmount = (amount: number, currency: string) =>
    `${currency === 'INR' ? '₹' : '$'}${amount.toLocaleString('en-IN')}`;

  const typeBadge =
    invoice.type === 'RECEIPT'
      ? 'bg-indigo-500/10 text-indigo-300 border-indigo-500/20'
      : 'bg-purple-500/10 text-purple-300 border-purple-500/20';

  return (
    <div className="glass glass-hover rounded-2xl p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 transition-all duration-200 border border-white/5">
      {/* Icon + Details */}
      <div className="flex items-start gap-4">
        <div className="w-10 h-10 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center flex-shrink-0">
          <FileText className="w-5 h-5 text-indigo-400" />
        </div>

        <div className="space-y-1 min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-bold text-sm text-foreground font-mono">{invoice.invoiceNumber}</span>
            <span className={`px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider rounded-full border ${typeBadge}`}>
              {invoice.type === 'RECEIPT' ? 'Receipt' : 'Refund Receipt'}
            </span>
          </div>
          <p className="text-xs text-muted-foreground">
            Date: <span className="font-semibold text-slate-300">{formatDate(invoice.createdAt)}</span>
            {' · '}
            Booking: <span className="font-mono text-slate-400">{invoice.bookingId.substring(0, 8)}</span>
          </p>
          <p className="text-lg font-bold text-foreground">
            {formatAmount(invoice.amount, invoice.currency)}
          </p>
        </div>
      </div>

      {/* Download Button */}
      <a
        href={paymentService.getInvoicePdfUrl(invoice.id)}
        target="_blank"
        rel="noopener noreferrer"
        className="flex items-center gap-2 px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-semibold transition-all duration-200 flex-shrink-0"
        aria-label={`Download invoice ${invoice.invoiceNumber}`}
        download
      >
        <Download className="w-4 h-4" />
        Download PDF
      </a>
    </div>
  );
}
